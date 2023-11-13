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
package org.apache.dubbo.registry.etcd;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.remoting.etcd.ChildListener;
import org.apache.dubbo.remoting.etcd.EtcdClient;
import org.apache.dubbo.remoting.etcd.EtcdTransporter;
import org.apache.dubbo.remoting.etcd.StateListener;
import org.apache.dubbo.remoting.etcd.option.OptionUtil;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 2019-07-08
 */
public class EtcdServiceDiscovery extends AbstractServiceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(EtcdServiceDiscovery.class);

    private final String root = "/services";

    private final Set<String> services = new ConcurrentHashSet<>();
    private final Map<String, InstanceChildListener> childListenerMap = new ConcurrentHashMap<>();

    EtcdClient etcdClient;

    public EtcdServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);
        EtcdTransporter etcdTransporter = applicationModel.getExtensionLoader(EtcdTransporter.class).getAdaptiveExtension();

        if (registryURL.isAnyHost()) {
            throw new IllegalStateException("Service discovery address is invalid, actual: '" + registryURL.getHost() + "'");
        }

        etcdClient = etcdTransporter.connect(registryURL);

        etcdClient.addStateListener(state -> {
            if (state == StateListener.CONNECTED) {
                try {
                    recover();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });

        this.registryURL = registryURL;
    }

    @Override
    protected void doDestroy() throws Exception {
        if (etcdClient != null && etcdClient.isConnected()) {
            etcdClient.close();
        }
    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        try {
            String path = toPath(serviceInstance);
            etcdClient.putEphemeral(path, new Gson().toJson(serviceInstance));
            services.add(serviceInstance.getServiceName());
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + serviceInstance + " to etcd " + etcdClient.getUrl()
                + ", cause: " + (OptionUtil.isProtocolError(e)
                ? "etcd3 registry may not be supported yet or etcd3 registry is not available."
                : e.getMessage()), e);
        }
    }

    protected String toPath(ServiceInstance serviceInstance) {
        return root + File.separator + serviceInstance.getServiceName() + File.separator + serviceInstance.getHost()
            + ":" + serviceInstance.getPort();
    }

    protected String toParentPath(String serviceName) {
        return root + File.separator + serviceName;
    }

    @Override
    protected void doUnregister(ServiceInstance serviceInstance) {
        try {
            String path = toPath(serviceInstance);
            etcdClient.delete(path);
            services.remove(serviceInstance.getServiceName());
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + serviceInstance + " to etcd " + etcdClient.getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public Set<String> getServices() {
        return Collections.unmodifiableSet(services);
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        for (String serviceName : listener.getServiceNames()) {
            registerServiceWatcher(serviceName, listener);
        }
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        List<String> children = etcdClient.getChildren(toParentPath(serviceName));
        if (CollectionUtils.isEmpty(children)) {
            return Collections.emptyList();
        }
        List<ServiceInstance> list = new ArrayList<>(children.size());
        for (String child : children) {
            ServiceInstance serviceInstance = new Gson().fromJson(etcdClient.getKVValue(child), DefaultServiceInstance.class);
            list.add(serviceInstance);
        }
        return list;
    }

    protected void registerServiceWatcher(String serviceName, ServiceInstancesChangedListener listener) {
        String path = root + File.separator + serviceName;

        InstanceChildListener childListener = childListenerMap.get(serviceName);

        if (childListener == null) {
            childListener = new InstanceChildListener(serviceName);
            childListenerMap.put(serviceName, childListener);
            childListener.addListener(listener);

            etcdClient.create(path);
            etcdClient.addChildListener(path, childListener);
        } else {
            childListener.addListener(listener);
        }
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        for (String serviceName : listener.getServiceNames()) {
            unregisterServiceWatcher(serviceName, listener);
        }
    }

    protected void unregisterServiceWatcher(String serviceName, ServiceInstancesChangedListener listener) {
        String path = root + File.separator + serviceName;

        InstanceChildListener childListener = childListenerMap.get(serviceName);

        if (childListener != null) {
            childListener.removeListener(listener);

            if (childListener.getListenerCount() == 0) {
                etcdClient.removeChildListener(path, childListener);
            }
        }
    }

    public class InstanceChildListener implements ChildListener {
        private final List<ServiceInstancesChangedListener> listeners;

        private final String serviceName;

        public InstanceChildListener(String serviceName) {
            this.serviceName = serviceName;
            this.listeners = new CopyOnWriteArrayList<>();
        }

        @Override
        public void childChanged(String path, List<String> children) {
            List<ServiceInstance> list = new ArrayList<>(children.size());
            for (String child : children) {
                ServiceInstance serviceInstance = new Gson().fromJson(etcdClient.getKVValue(child), DefaultServiceInstance.class);
                list.add(serviceInstance);
            }

            for (ServiceInstancesChangedListener listener : listeners) {
                listener.onEvent(new ServiceInstancesChangedEvent(serviceName, list));
            }
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

    private void recover() throws Exception {
        // register
        if (serviceInstance != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover application register: " + serviceInstance);
            }
            doRegister(serviceInstance);
        }
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }
}
