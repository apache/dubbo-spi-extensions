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

package org.apache.dubbo.registry.polaris;

import com.tencent.polaris.api.listener.ServiceListener;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.common.registry.Consts;
import com.tencent.polaris.common.registry.ConvertUtils;
import com.tencent.polaris.common.registry.PolarisOperator;
import com.tencent.polaris.common.utils.ExtensionConsts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.polaris.task.FetchTask;
import org.apache.dubbo.registry.polaris.task.InstancesHandler;
import org.apache.dubbo.registry.polaris.task.TaskScheduler;
import org.apache.dubbo.registry.polaris.task.WatchTask;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class PolarisRegistryServiceDiscovery extends AbstractServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisRegistryServiceDiscovery.class);

    private final PolarisOperator polarisOperator;

    private static final TaskScheduler taskScheduler = new TaskScheduler();

    private final Set<ServiceInstance> registeredInstances = new ConcurrentHashSet<>();

    private final Map<String, ServiceListener> serviceListeners = new ConcurrentHashMap<>();

    private final boolean hasCircuitBreaker;

    public PolarisRegistryServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);
        polarisOperator = PolarisRegistryUtils.getOrCreatePolarisOperator(registryURL);
        ExtensionLoader<Filter> filterExtensionLoader = applicationModel.getExtensionDirector()
            .getExtensionLoader(Filter.class);
        if (null != filterExtensionLoader) {
            hasCircuitBreaker = filterExtensionLoader.hasExtension(ExtensionConsts.PLUGIN_CIRCUITBREAKER_NAME);
        } else {
            hasCircuitBreaker = false;
        }
    }

    @Override
    protected void doRegister(ServiceInstance serviceInstance) throws RuntimeException {
        LOGGER.info(String.format(
            "[POLARIS] register %s to service %s", serviceInstance.getAddress(), serviceInstance.getServiceName()));
        int weight = Constants.DEFAULT_WEIGHT;
        String weightValue = serviceInstance.getMetadata(Constants.WEIGHT_KEY);
        if (StringUtils.isNotBlank(weightValue)) {
            try {
                weight = Integer.parseInt(weightValue);
            } catch (Exception e) {
                LOGGER.warn(String.format("[POLARIS] fail to parse weight value %s", weightValue));
            }
        }
        String applicationName = serviceInstance.getMetadata(CommonConstants.APPLICATION_KEY);
        if (StringUtils.isBlank(applicationName)) {
            ApplicationModel applicationModel = serviceInstance.getApplicationModel();
            if (null != applicationModel) {
                applicationName = applicationModel.getApplicationName();
            }
            serviceInstance.putExtendParam(CommonConstants.APPLICATION_KEY, applicationName);
        }
        String version = serviceInstance.getMetadata(CommonConstants.VERSION_KEY, "");
        polarisOperator.register(serviceInstance.getServiceName(), serviceInstance.getHost(), serviceInstance.getPort(),
            "", version, weight, serviceInstance.getAllParams());
        registeredInstances.add(serviceInstance);
    }

    @Override
    protected void doUnregister(ServiceInstance serviceInstance) {
        LOGGER.info(String.format(
            "[POLARIS] unregister %s to service %s", serviceInstance.getAddress(), serviceInstance.getServiceName()));
        polarisOperator.deregister(serviceInstance.getServiceName(), serviceInstance.getHost(), serviceInstance.getPort());
        registeredInstances.remove(serviceInstance);
    }

    @Override
    protected void doDestroy() {
        for (ServiceInstance serviceInstance : registeredInstances) {
            doUnregister(serviceInstance);
        }
        PolarisRegistryUtils.removePolarisOperator(getUrl());
        polarisOperator.destroy();
        taskScheduler.destroy();
    }

    @Override
    public Set<String> getServices() {
        List<ServiceInfo> services = polarisOperator.getServices();
        Set<String> svcNames = new HashSet<>();
        for (ServiceInfo serviceInfo : services) {
            svcNames.add(serviceInfo.getService());
        }
        return svcNames;
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        Instance[] availableInstances = polarisOperator.getAvailableInstances(serviceName, !hasCircuitBreaker);
        List<ServiceInstance> instances = new ArrayList<>();
        for (Instance availableInstance : availableInstances) {
            instances.add(instanceToServiceInstance(availableInstance));
        }
        return instances;
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
        throws NullPointerException, IllegalArgumentException {
        PolarisInstancesHandler polarisInstancesHandler = new PolarisInstancesHandler(listener);
        for (String serviceName : listener.getServiceNames()) {
            LOGGER.info(String.format("[polaris] subscribe service: %s", serviceName));
            FetchTask fetchTask = new FetchTask(serviceName, polarisInstancesHandler, polarisOperator, !hasCircuitBreaker);
            taskScheduler.submitWatchTask(new WatchTask(serviceName, fetchTask, taskScheduler));
        }
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) {
        for (String serviceName : listener.getServiceNames()) {
            LOGGER.info(String.format("[polaris] unsubscribe service: %s", serviceName));
            ServiceListener serviceListener = serviceListeners.remove(serviceName);
            if (null != serviceListener) {
                polarisOperator.unwatchService(serviceName, serviceListener);
            }
        }
    }

    private ServiceInstance instanceToServiceInstance(Instance instance) {
        ServiceInstance serviceInstance = new DefaultServiceInstance(
            instance.getService(), instance.getHost(), instance.getPort(), applicationModel);
        Map<String, String> newMetadata = new HashMap<>(instance.getMetadata());
        boolean hasWeight = false;
        if (newMetadata.containsKey(Constants.WEIGHT_KEY)) {
            String weightStr = newMetadata.get(Constants.WEIGHT_KEY);
            try {
                int weightValue = Integer.parseInt(weightStr);
                if (weightValue == instance.getWeight()) {
                    hasWeight = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (!hasWeight) {
            newMetadata.put(Constants.WEIGHT_KEY, Integer.toString(instance.getWeight()));
        }
        String applicationName = newMetadata.getOrDefault(CommonConstants.APPLICATION_KEY, "" );
        MetadataInfo.ServiceInfo serviceInfo = new MetadataInfo.ServiceInfo(
            instance.getService(), CommonConstants.ANY_VALUE,
            instance.getVersion(),
            instance.getProtocol(), instance.getPort(),
            newMetadata.getOrDefault(CommonConstants.PATH_KEY, ""), newMetadata);
        Map<String, MetadataInfo.ServiceInfo> serviceInfoMap = new HashMap<>();
        serviceInfoMap.put(instance.getService(), serviceInfo);
        MetadataInfo metadataInfo = new MetadataInfo(applicationName, instance.getRevision(), serviceInfoMap);
        serviceInstance.setServiceMetadata(metadataInfo);
        serviceInstance.getMetadata().putAll(newMetadata);

        serviceInstance.putExtendParamIfAbsent(Consts.INSTANCE_KEY_ID, instance.getId());
        serviceInstance.putExtendParamIfAbsent(Consts.INSTANCE_KEY_HEALTHY, Boolean.toString(instance.isHealthy()));
        serviceInstance.putExtendParamIfAbsent(Consts.INSTANCE_KEY_ISOLATED, Boolean.toString(instance.isIsolated()));
        serviceInstance.putExtendParamIfAbsent(Consts.INSTANCE_KEY_CIRCUIT_BREAKER, ConvertUtils.circuitBreakersToString(instance));
        return serviceInstance;
    }

    private class PolarisInstancesHandler implements InstancesHandler {

        private final ServiceInstancesChangedListener listener;

        public PolarisInstancesHandler(ServiceInstancesChangedListener listener) {
            this.listener = listener;
        }

        @Override
        public void onInstances(String serviceName, Instance[] instances) {
            List<ServiceInstance> serviceInstances = new ArrayList<>();
            for (Instance instance : instances) {
                serviceInstances.add(instanceToServiceInstance(instance));
            }
            listener.onEvent(new ServiceInstancesChangedEvent(serviceName, serviceInstances));
        }

        @Override
        public void onWatchSuccess(String serviceName, ServiceListener serviceListener) {
            serviceListeners.put(serviceName, serviceListener);
        }
    }
}
