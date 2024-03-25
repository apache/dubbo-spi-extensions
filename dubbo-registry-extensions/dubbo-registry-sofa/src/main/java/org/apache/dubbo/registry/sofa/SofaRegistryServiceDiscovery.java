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
package org.apache.dubbo.registry.sofa;


import com.alipay.sofa.registry.client.api.Publisher;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.api.Subscriber;
import com.alipay.sofa.registry.client.api.model.RegistryType;
import com.alipay.sofa.registry.client.api.model.UserData;
import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.api.registration.SubscriberRegistration;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClient;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClientConfigBuilder;
import com.alipay.sofa.registry.core.model.ScopeEnum;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.ADDRESS_WAIT_TIME_KEY;
import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.LOCAL_DATA_CENTER;
import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.LOCAL_REGION;


public class SofaRegistryServiceDiscovery extends AbstractServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(SofaRegistryServiceDiscovery.class);

    private static final String DEFAULT_GROUP = "dubbo";

    private DefaultRegistryClient registryClient;

    private int waitAddressTimeout;

    private RegistryClientConfig registryClientConfig;

    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();

    private final Map<String, SofaRegistryListener> sofaRegistryListenerMap = new ConcurrentHashMap<>();



    public SofaRegistryServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);

        this.registryClientConfig = DefaultRegistryClientConfigBuilder.start()
                .setDataCenter(LOCAL_DATA_CENTER)
                .setZone(LOCAL_REGION)
                .setRegistryEndpoint(registryURL.getHost())
                .setRegistryEndpointPort(registryURL.getPort()).build();

        registryClient = new DefaultRegistryClient(this.registryClientConfig);
        registryClient.init();

        this.waitAddressTimeout = Integer.parseInt(System.getProperty(ADDRESS_WAIT_TIME_KEY, "5000"));
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    @Override
    protected void doDestroy() throws Exception {

    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        SofaRegistryInstance sofaRegistryInstance = new SofaRegistryInstance(UUID.randomUUID().toString(), serviceInstance.getHost(), serviceInstance.getPort(), serviceInstance.getServiceName(), serviceInstance.getMetadata());
        Publisher publisher = publishers.get(serviceInstance.getServiceName());
        this.serviceInstance = serviceInstance;
        if (null == publisher) {
            PublisherRegistration registration = new PublisherRegistration(serviceInstance.getServiceName());
            registration.setGroup(DEFAULT_GROUP);
            publisher = registryClient.register(registration, JsonUtils.toJson(sofaRegistryInstance));

            publishers.put(serviceInstance.getServiceName(), publisher);
        } else {
            publisher.republish(JsonUtils.toJson(sofaRegistryInstance));
        }
    }

    @Override
    protected void doUnregister(ServiceInstance serviceInstance) {
        registryClient.unregister(serviceInstance.getServiceName(), DEFAULT_GROUP, RegistryType.PUBLISHER);
    }

    @Override
    public synchronized void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> registerServiceWatcher(serviceName, listener));
    }

    protected void registerServiceWatcher(String serviceName, ServiceInstancesChangedListener listener) {
        SofaRegistryListener sofaRegistryListener = sofaRegistryListenerMap.get(serviceName);

        if (null == sofaRegistryListener) {
            sofaRegistryListener = new SofaRegistryListener(serviceName);
            sofaRegistryListenerMap.put(serviceName, sofaRegistryListener);
            sofaRegistryListener.addListener(listener);
            sofaRegistryListener.start();
        } else {
            sofaRegistryListener.addListener(listener);
        }

    }

    @Override
    public synchronized List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        SofaRegistryListener sofaRegistryListener = sofaRegistryListenerMap.get(serviceName);

        if (null == sofaRegistryListener) {
            sofaRegistryListener = new SofaRegistryListener(serviceName);
            sofaRegistryListenerMap.put(serviceName, sofaRegistryListener);
            sofaRegistryListener.start();
        }

        return sofaRegistryListener.peekData();
    }

    @Override
    public synchronized void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        for (String serviceName : listener.getServiceNames()) {
            SofaRegistryListener sofaRegistryListener = sofaRegistryListenerMap.get(serviceName);

            if (null != sofaRegistryListener) {
                sofaRegistryListener.removeListener(listener);

                if (sofaRegistryListener.getListenerCount() == 0) {
                    sofaRegistryListener.stop();
                    sofaRegistryListenerMap.remove(serviceName);
                }
            }
        }
    }

    public class SofaRegistryListener {
        private final String serviceName;
        private volatile Subscriber subscriber;
        private final List<ServiceInstancesChangedListener> listeners = new CopyOnWriteArrayList<>();

        private volatile List<ServiceInstance> serviceInstances;

        public SofaRegistryListener(String serviceName) {
            this.serviceName = serviceName;
        }

        public void start() {
            final CountDownLatch latch = new CountDownLatch(1);
            SubscriberRegistration subscriberRegistration = new SubscriberRegistration(serviceName, (dataId, data) -> {
                handleRegistryData(dataId, data, latch);
            });
            subscriberRegistration.setGroup(DEFAULT_GROUP);
            subscriberRegistration.setScopeEnum(ScopeEnum.global);

            subscriber = registryClient.register(subscriberRegistration);

            waitAddress(serviceName, latch);
        }

        public void stop() {
            if (null != subscriber) {
                subscriber.unregister();
            }
        }

        private void handleRegistryData(String dataId, UserData userData, CountDownLatch latch) {
            try {
                List<String> datas = getUserData(dataId, userData);
                List<ServiceInstance> newServiceInstances = new ArrayList<>(datas.size());

                for (String serviceData : datas) {
                    SofaRegistryInstance sri = JsonUtils.toJavaObject(serviceData, SofaRegistryInstance.class);

                    DefaultServiceInstance serviceInstance = new DefaultServiceInstance(dataId, sri.getHost(), sri.getPort(), applicationModel);
                    serviceInstance.setMetadata(sri.getMetadata());
                    newServiceInstances.add(serviceInstance);
                }

                this.serviceInstances = newServiceInstances;

                for (ServiceInstancesChangedListener listener : listeners) {
                    listener.onEvent(new ServiceInstancesChangedEvent(dataId, serviceInstances));
                }
            } finally {
                if (null != latch) {
                    latch.countDown();
                }
            }
        }

        public List<ServiceInstance> peekData() {
            return serviceInstances;
        }

        public void addListener(ServiceInstancesChangedListener listener) {
            listeners.add(listener);
        }

        public void removeListener(ServiceInstancesChangedListener listener) {
            listeners.remove(listener);
        }

        public int getListenerCount() {
            return listeners.size();
        }
    }

    private void waitAddress(String serviceName, CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(waitAddressTimeout, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Subscribe data failed by dataId " + serviceName);
            }
        } catch (Exception e) {
            LOGGER.error("Error when wait Address!", e);
        }
    }

    /**
     * Print address data.
     *
     * @param dataId   the data id
     * @param userData the user data
     */
    protected List<String> getUserData(String dataId, UserData userData) {

        List<String> datas = null;
        if (userData == null) {
            datas = new ArrayList<>(0);
        } else {
            datas = flatUserData(userData);
        }

        StringBuilder sb = new StringBuilder();
        for (String provider : datas) {
            sb.append("  >>> ").append(provider).append("\n");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Receive updated RPC service addresses: service[" + dataId
                    + "]\n  .Available target addresses size [" + datas.size() + "]\n"
                    + sb.toString());
        }

        return datas;
    }

    /**
     * Flat user data list.
     *
     * @param userData the user data
     * @return the list
     */
    protected List<String> flatUserData(UserData userData) {
        List<String> result = new ArrayList<>();
        Map<String, List<String>> zoneData = userData.getZoneData();

        for (Map.Entry<String, List<String>> entry : zoneData.entrySet()) {
            result.addAll(entry.getValue());
        }

        return result;
    }

    @Override
    public Set<String> getServices() {
        return sofaRegistryListenerMap.keySet();
    }

}
