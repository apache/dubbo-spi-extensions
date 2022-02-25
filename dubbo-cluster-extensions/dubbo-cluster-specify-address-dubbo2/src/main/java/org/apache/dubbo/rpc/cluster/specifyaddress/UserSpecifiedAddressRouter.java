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
package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserSpecifiedAddressRouter extends AbstractRouter {
    private final static Logger logger = LoggerFactory.getLogger(UserSpecifiedAddressRouter.class);
    // protected for ut purpose
    protected static int EXPIRE_TIME = 10 * 60 * 1000;

    private volatile List<Invoker<?>> invokers = Collections.emptyList();
    private volatile Map<String, Invoker<?>> ip2Invoker;
    private volatile Map<String, Invoker<?>> address2Invoker;

    private final Lock cacheLock = new ReentrantLock();

    public UserSpecifiedAddressRouter(URL referenceUrl) {
        super(referenceUrl);
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        this.invokers = (List) invokers;
        // do not build cache until first Specify Invoke happened
        if (ip2Invoker != null) {
            ip2Invoker = processIp((List) invokers);
            address2Invoker = processAddress((List) invokers);
        }
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Address address = UserSpecifiedAddressUtil.getAddress();

        // 1. check if set address in ThreadLocal
        if (address == null) {
            return invokers;
        }

        List<Invoker<T>> result = new LinkedList<>();

        // 2. check if set address url
        if (address.getUrlAddress() != null) {
            Invoker<?> invoker = getInvokerByURL(address, invocation);
            result.add((Invoker) invoker);
            return result;
        }

        // 3. check if set ip and port
        if (StringUtils.isNotEmpty(address.getIp())) {
            Invoker<?> invoker = getInvokerByIp(address, invocation);
            result.add((Invoker) invoker);
            return result;
        }

        return invokers;
    }

    private Invoker<?> getInvokerByURL(Address address, Invocation invocation) {
        tryLoadSpecifiedMap();

        // try to find in directory
        URL urlAddress = address.getUrlAddress();
        String targetAddress = urlAddress.getHost() + ":" + urlAddress.getPort();
        Invoker<?> invoker = address2Invoker.get(targetAddress);
        if (invoker != null) {
            AtomicBoolean match = new AtomicBoolean(true);
            if (StringUtils.isNotEmpty(urlAddress.getProtocol())) {
                match.set(invoker.getUrl().getProtocol().equals(urlAddress.getProtocol()));
            }
            if (match.get()) {
                urlAddress.getParameters().forEach((k, v) -> {
                    if (match.get()) {
                        match.set(v.equals(invoker.getUrl().getParameter(k)));
                    }
                });
            }
            if (match.get()) {
                return invoker;
            }
        }

        // create new one
        throw new RpcException("User specified server address not support refer new url in Dubbo 2.x. Please upgrade to Dubbo 3.x and use dubbo-cluster-specify-address-dubbo3.");
    }

    public Invoker<?> getInvokerByIp(Address address, Invocation invocation) {
        tryLoadSpecifiedMap();

        String ip = address.getIp();
        int port = address.getPort();

        Invoker<?> targetInvoker;
        if (port != 0) {
            targetInvoker = address2Invoker.get(ip + ":" + port);
            if (targetInvoker != null) {
                return targetInvoker;
            }
        } else {
            targetInvoker = ip2Invoker.get(ip);
            if (targetInvoker != null) {
                return targetInvoker;
            }
        }

        if (!address.isNeedToCreate()) {
            throwException(invocation, address);
        }

        throw new RpcException("User specified server address not support refer new url in Dubbo 2.x. Please upgrade to Dubbo 3.x and use dubbo-cluster-specify-address-dubbo3.");
    }

    private void throwException(Invocation invocation, Address address) {
        throw new RpcException("user specified server address : [" + address + "] is not a valid provider for service: ["
            + getUrl().getServiceKey() + "]");
    }


    private Map<String, Invoker<?>> processIp(List<Invoker<?>> invokerList) {
        Map<String, Invoker<?>> ip2Invoker = new HashMap<>();
        for (Invoker<?> invoker : invokerList) {
            ip2Invoker.put(invoker.getUrl().getHost(), invoker);
        }
        return Collections.unmodifiableMap(ip2Invoker);
    }

    private Map<String, Invoker<?>> processAddress(List<Invoker<?>> addresses) {
        Map<String, Invoker<?>> address2Invoker = new HashMap<>();
        for (Invoker<?> invoker : addresses) {
            address2Invoker.put(invoker.getUrl().getHost() + ":" + invoker.getUrl().getPort(), invoker);
        }
        return Collections.unmodifiableMap(address2Invoker);
    }

    // For ut only
    @Deprecated
    protected Map<String, Invoker<?>> getIp2Invoker() {
        return ip2Invoker;
    }

    // For ut only
    @Deprecated
    protected Map<String, Invoker<?>> getAddress2Invoker() {
        return address2Invoker;
    }

    // For ut only
    @Deprecated
    protected List<Invoker<?>> getInvokers() {
        return invokers;
    }

    private void tryLoadSpecifiedMap() {
        if (ip2Invoker != null) {
            return;
        }
        synchronized (this) {
            if (ip2Invoker != null) {
                return;
            }
            List<Invoker<?>> invokers = this.invokers;
            if (CollectionUtils.isEmpty(invokers)) {
                address2Invoker = Collections.unmodifiableMap(new HashMap<>());
                ip2Invoker = Collections.unmodifiableMap(new HashMap<>());
                return;
            }
            address2Invoker = processAddress(invokers);
            ip2Invoker = processIp(invokers);
        }
    }
}
