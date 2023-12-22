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

package org.apache.dubbo.registry.polaris.task;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.common.registry.PolarisOperator;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

public class FetchTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchTask.class);

    private final String service;

    private final InstancesHandler handler;

    private final PolarisOperator polarisOperator;

    private final boolean includeCircuitBreak;

    public FetchTask(String service, InstancesHandler handler, PolarisOperator polarisOperator, boolean includeCircuitBreak) {
        this.service = service;
        this.handler = handler;
        this.polarisOperator = polarisOperator;
        this.includeCircuitBreak = includeCircuitBreak;
    }

    public PolarisOperator getPolarisOperator() {
        return polarisOperator;
    }

    public InstancesHandler getHandler() {
        return handler;
    }

    @Override
    public void run() {
        Instance[] instances;
        try {
            instances = polarisOperator.getAvailableInstances(service, includeCircuitBreak);
        } catch (PolarisException e) {
            LOGGER.error(String.format("[POLARIS] fail to fetch instances for service %s", service), e);
            return;
        }
        handler.onInstances(service, instances);
    }
}
