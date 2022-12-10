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
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.common.registry.Consts;
import com.tencent.polaris.common.registry.ConvertUtils;
import com.tencent.polaris.common.registry.PolarisOperator;
import com.tencent.polaris.common.utils.ExtensionConsts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.polaris.task.FetchTask;
import org.apache.dubbo.registry.polaris.task.InstancesHandler;
import org.apache.dubbo.registry.polaris.task.TaskScheduler;
import org.apache.dubbo.registry.polaris.task.WatchTask;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.cluster.RouterFactory;

public class PolarisRegistry extends FailbackRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisRegistry.class);

    private static final TaskScheduler taskScheduler = new TaskScheduler();

    private final Set<URL> registeredInstances = new ConcurrentHashSet<>();

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private final Map<NotifyListener, ServiceListener> serviceListeners = new ConcurrentHashMap<>();

    private final PolarisOperator polarisOperator;

    private final boolean hasCircuitBreaker;

    private final boolean hasRouter;

    public PolarisRegistry(URL url) {
        super(url);
        polarisOperator = PolarisRegistryUtils.getOrCreatePolarisOperator(url);
        ExtensionLoader<RouterFactory> routerExtensionLoader = ExtensionLoader.getExtensionLoader(RouterFactory.class);
        hasRouter = routerExtensionLoader.hasExtension(ExtensionConsts.PLUGIN_ROUTER_NAME);
        ExtensionLoader<Filter> filterExtensionLoader = ExtensionLoader.getExtensionLoader(Filter.class);
        hasCircuitBreaker = filterExtensionLoader.hasExtension(ExtensionConsts.PLUGIN_CIRCUITBREAKER_NAME);
    }

    private URL buildRouterURL(URL consumerUrl) {
        URL routerURL = null;
        if (hasRouter) {
            URL registryURL = getUrl();
            routerURL = new URL(RegistryConstants.ROUTE_PROTOCOL, registryURL.getHost(), registryURL.getPort());
            routerURL = routerURL.setServiceInterface(CommonConstants.ANY_VALUE);
            routerURL = routerURL.addParameter(Constants.ROUTER_KEY, ExtensionConsts.PLUGIN_ROUTER_NAME);
            String consumerGroup = consumerUrl.getParameter(CommonConstants.GROUP_KEY);
            String consumerVersion = consumerUrl.getParameter(CommonConstants.VERSION_KEY);
            String consumerClassifier = consumerUrl.getParameter(CommonConstants.CLASSIFIER_KEY);
            if (null != consumerGroup) {
                routerURL = routerURL.addParameter(CommonConstants.GROUP_KEY, consumerGroup);
            }
            if (null != consumerVersion) {
                routerURL = routerURL.addParameter(CommonConstants.VERSION_KEY, consumerVersion);
            }
            if (null != consumerClassifier) {
                routerURL = routerURL.addParameter(CommonConstants.CLASSIFIER_KEY, consumerClassifier);
            }
        }
        return routerURL;
    }

    @Override
    public void doRegister(URL url) {
        if (!shouldRegister(url)) {
            return;
        }
        LOGGER.info(String.format("[POLARIS] register service to polaris: %s", url.toString()));
        Map<String, String> metadata = new HashMap<>(url.getParameters());
        metadata.put(CommonConstants.PATH_KEY, url.getPath());
        int port = url.getPort();
        if (port > 0) {
            int weight = url.getParameter(Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT);
            String version = url.getParameter(CommonConstants.VERSION_KEY, "");
            polarisOperator.register(url.getServiceInterface(), url.getHost(), port, url.getProtocol(), version, weight,
                    metadata);
            registeredInstances.add(url);
        } else {
            LOGGER.warn(String.format("[POLARIS] skip register url %s for zero port value", url));
        }
    }

    private boolean shouldRegister(URL url) {
        return !StringUtils.equals(url.getProtocol(), CommonConstants.CONSUMER);
    }

    @Override
    public void doUnregister(URL url) {
        if (!shouldRegister(url)) {
            return;
        }
        LOGGER.info(String.format("[POLARIS] unregister service from polaris: %s", url.toString()));
        int port = url.getPort();
        if (port > 0) {
            polarisOperator.deregister(url.getServiceInterface(), url.getHost(), url.getPort());
            registeredInstances.remove(url);
        }
    }

    @Override
    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            super.destroy();
            Collection<URL> urls = Collections.unmodifiableCollection(registeredInstances);
            for (URL url : urls) {
                doUnregister(url);
            }
            PolarisRegistryUtils.removePolarisOperator(getUrl());
            polarisOperator.destroy();
            taskScheduler.destroy();
        }
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        String service = url.getServiceInterface();
        Instance[] instances = polarisOperator.getAvailableInstances(service, !hasCircuitBreaker);
        onInstances(url, listener, instances);
        LOGGER.info(String.format("[POLARIS] submit watch task for service %s", service));
        PolarisInstancesHandler polarisInstancesHandler = new PolarisInstancesHandler(url, listener);
        FetchTask fetchTask = new FetchTask(
            url.getServiceInterface(), polarisInstancesHandler, polarisOperator, !hasCircuitBreaker);
        taskScheduler.submitWatchTask(new WatchTask(url.getServiceInterface(), fetchTask, taskScheduler));
    }


    private void onInstances(URL url, NotifyListener listener, Instance[] instances) {
        LOGGER.info(String.format("[POLARIS] update instances count: %d, service: %s", null == instances ? 0 : instances.length,
                url.getServiceInterface()));
        List<URL> urls = new ArrayList<>();
        if (null != instances) {
            for (Instance instance : instances) {
                urls.add(instanceToURL(instance));
            }
        }
        URL routerURL = buildRouterURL(url);
        if (null != routerURL) {
            urls.add(routerURL);
        }
        PolarisRegistry.this.notify(url, listener, urls);
    }

    private static URL instanceToURL(Instance instance) {
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
        newMetadata.put(Consts.INSTANCE_KEY_ID, instance.getId());
        newMetadata.put(Consts.INSTANCE_KEY_HEALTHY, Boolean.toString(instance.isHealthy()));
        newMetadata.put(Consts.INSTANCE_KEY_ISOLATED, Boolean.toString(instance.isIsolated()));
        newMetadata.put(Consts.INSTANCE_KEY_CIRCUIT_BREAKER, ConvertUtils.circuitBreakersToString(instance));
        clearEmptyKeys(newMetadata, new String[]{CommonConstants.VERSION_KEY, CommonConstants.GROUP_KEY});
        return new URL(instance.getProtocol(),
                instance.getHost(),
                instance.getPort(),
                newMetadata.get(CommonConstants.PATH_KEY),
                newMetadata);
    }

    private static void clearEmptyKeys(Map<String, String> parameters, String[] keys) {
        for (String key : keys) {
            String value = parameters.get(key);
            if (null != value && StringUtils.isBlank(value)) {
                parameters.remove(key);
            }
        }
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        LOGGER.info(String.format("[polaris] unsubscribe service: %s", url.toString()));
        taskScheduler.submitWatchTask(new Runnable() {
            @Override
            public void run() {
                ServiceListener serviceListener = serviceListeners.remove(listener);
                if (null != serviceListener) {
                    polarisOperator.unwatchService(url.getServiceInterface(), serviceListener);
                }
            }
        });
    }

    public PolarisOperator getPolarisOperator() {
        return polarisOperator;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private class PolarisInstancesHandler implements InstancesHandler {

        private final URL url;

        private final NotifyListener listener;

        public PolarisInstancesHandler(URL url, NotifyListener listener) {
            this.url = url;
            this.listener = listener;
        }

        @Override
        public void onInstances(String serviceName, Instance[] instances) {
            PolarisRegistry.this.onInstances(url, listener, instances);
        }

        @Override
        public void onWatchSuccess(String serviceName, ServiceListener serviceListener) {
            serviceListeners.put(listener, serviceListener);
        }
    }
}
