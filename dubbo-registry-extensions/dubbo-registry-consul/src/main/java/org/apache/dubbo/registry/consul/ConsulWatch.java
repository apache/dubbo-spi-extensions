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
package org.apache.dubbo.registry.consul;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import com.ecwid.consul.ConsulException;
import com.ecwid.consul.transport.TransportException;
import com.ecwid.consul.v1.OperationException;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Runs a Consul blocking watch across transient query failures.
 *
 * @date 2026/11/5
 */
final class ConsulWatch {
    private static final Logger logger = LoggerFactory.getLogger(ConsulWatch.class);

    private ConsulWatch() {
    }

    static void run(BooleanSupplier running, Runnable poll, String service) {
        while (running.getAsBoolean() && !Thread.currentThread().isInterrupted()) {
            try {
                poll.run();
            } catch (ConsulException failure) {
                if (!isRetryable(failure)) {
                    throw failure;
                }
                if (!running.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                    return;
                }
                logger.warn("Consul watch query failed for " + service + "; retrying", failure);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static boolean isRetryable(ConsulException failure) {
        if (failure instanceof OperationException) {
            int status = ((OperationException) failure).getStatusCode();
            return status == 429 || status >= 500 && status < 600;
        }
        return failure instanceof TransportException;
    }
}
