/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * PeakEwmaLoadBalance is designed to converge quickly when encountering slow endpoints.
 * It is quick to react to latency spikes recovering only cautiously.Peak EWMA takes
 * history into account,so that slow behavior is penalized relative to the
 * supplied `decayTime`.
 * if there are multiple invokers and the same cost,then randomly called,which doesn't care
 * about weight.
 * <p>
 * Inspiration drawn from:
 * https://github.com/twitter/finagle/blob/1bc837c4feafc0096e43c0e98516a8e1c50c4421
 * /finagle-core/src/main/scala/com/twitter/finagle/loadbalancer/PeakEwma.scala
 */
public class PeakEwmaLoadBalance extends AbstractLoadBalance implements ScopeModelAware {

    public static final String NAME = "peakewma";

    private static final String PEAK_EWMA_DECAY_TIME = "peakEwmaDecayTime";

    private static final double PENALTY = Long.MAX_VALUE >> 16;

    //double precision
    private static final double ZERO_COST = 1E-6;

    // The mean lifetime of `cost`, it reaches its half-life after decayTime*ln(2).
    private static double decayTime;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        decayTime = applicationModel.getModelEnvironment().getConfiguration().getInt(PEAK_EWMA_DECAY_TIME, 10_000);
    }

    private Map<RpcStatus, Metric> methodMap = CollectionUtils.newConcurrentHashMap();

    protected static class Metric {
        // last timestamp in Millis we observed an runningTime
        private volatile long lastUpdateTime;

        // ewma of rtt, sensitive to peaks.
        private volatile double cost;

        // calculate running time And active num
        private RpcStatus rpcStatus;
        private long invokeOffset;
        private long invokeElapsedOffset;

        //lock for get and set cost
        ReentrantLock ewmaLock = new ReentrantLock();

        public Metric(RpcStatus rpcStatus) {
            this.rpcStatus = rpcStatus;
            this.lastUpdateTime = System.currentTimeMillis();
            this.cost = 0.0;
            this.invokeOffset = 0;
            this.invokeElapsedOffset = 0;
        }

        private void observe() {
            double rtt = 0;
            long succeed = this.rpcStatus.getSucceeded() - this.invokeOffset;
            if (succeed != 0) {
                rtt = (this.rpcStatus.getSucceededElapsed() * 1.0 - this.invokeElapsedOffset) / succeed;
            }

            final long currentTime = System.currentTimeMillis();
            long td = Math.max(currentTime - lastUpdateTime, 0);
            double w = Math.exp(-td / decayTime);
            if (rtt > cost) {
                cost = rtt;
            } else {
                cost = cost * w + rtt * (1.0 - w);
            }

            lastUpdateTime = currentTime;
            invokeOffset = rpcStatus.getTotal();
            invokeElapsedOffset = rpcStatus.getTotalElapsed();
        }

        private double getCost() {
            ewmaLock.lock();
            observe();
            int active = rpcStatus.getActive();
            ewmaLock.unlock();

            double costTemp = cost;

            //If we don't have any latency history, we penalize the host on the first probe.
            return (costTemp < ZERO_COST && active != 0) ? PENALTY + active : costTemp * (active + 1);
        }
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();
        double minResponse = Double.MAX_VALUE;

        List<Integer> selectInvokerIndexList = new ArrayList<>(invokers.size());
        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);
            RpcStatus rpcStatus = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName());
            Metric metric = methodMap.computeIfAbsent(rpcStatus, Metric::new);

            // calculate the estimated response time from the product of active connections and succeeded average elapsed time.
            double estimateResponse = metric.getCost();
            if (estimateResponse < minResponse) {
                selectInvokerIndexList.clear();
                selectInvokerIndexList.add(i);
                minResponse = estimateResponse;
            } else if (estimateResponse == minResponse) {
                selectInvokerIndexList.add(i);
            }
        }

        return invokers.get(selectInvokerIndexList.get(ThreadLocalRandom.current().nextInt(selectInvokerIndexList.size())));
    }
}
