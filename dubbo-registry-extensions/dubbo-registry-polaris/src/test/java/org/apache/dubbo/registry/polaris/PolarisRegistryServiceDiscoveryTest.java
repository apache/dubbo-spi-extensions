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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PolarisRegistryServiceDiscoveryTest {

    private static PolarisRegistryServiceDiscovery polarisRegistryServiceDiscovery;

    private static ApplicationModel applicationModel;

    @BeforeAll
    public static void setup() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("namespace", "dubbo-java-test");
        parameters.put("persist_enable", "false");
        URL url = new URL("polaris", "183.47.111.80", 8091, parameters);
        applicationModel = FrameworkModel.defaultModel().newApplication();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("Test"));
        polarisRegistryServiceDiscovery = new PolarisRegistryServiceDiscovery(applicationModel, url);
    }

    @AfterAll
    public static void teardown() {
        if (null != polarisRegistryServiceDiscovery) {
            polarisRegistryServiceDiscovery.doDestroy();
        }
    }

    @Test
    public void testRegister() {
        int count = 10;
        String svcName = "polaris-registry-test-service-register";
        String host = NetUtils.getLocalHost();
        List<ServiceInstance> serviceInstances = buildInstanceUrls(svcName, host, 11300, count);
        for (ServiceInstance serviceInstance : serviceInstances) {
            polarisRegistryServiceDiscovery.doRegister(serviceInstance);
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            List<ServiceInstance> instances = polarisRegistryServiceDiscovery.getInstances(svcName);
            Assertions.assertEquals(count, countInstanceByHost(host, instances));
        } finally {
            for (ServiceInstance serviceInstance : serviceInstances) {
                polarisRegistryServiceDiscovery.doUnregister(serviceInstance);
            }
        }
    }

    private static List<ServiceInstance> buildInstanceUrls(String service, String host, int startPort, int count) {
        List<ServiceInstance> serviceUrls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ServiceInstance serviceInstance = new DefaultServiceInstance(service, host, startPort + i, applicationModel);
            serviceUrls.add(serviceInstance);
        }
        return serviceUrls;
    }

    @Test
    public void testSubscribe() {
        int count = 10;
        AtomicBoolean notified = new AtomicBoolean(false);
        String svcName = "polaris-registry-test-service-subscribe";
        Set<String> services = new HashSet<>();
        services.add(svcName);
        ServiceInstancesChangedListener serviceInstancesChangedListener = new ServiceInstancesChangedListener(services, polarisRegistryServiceDiscovery);
        URL consumerUrl = URL.valueOf("consumer://0.0.0.0/" + svcName);
        NotifyListener listener = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                notified.set(true);
                //notifiedCount.set(urls.size());
            }

            public ServiceInstancesChangedListener getServiceListener() {
                return serviceInstancesChangedListener;
            }

            public URL getConsumerUrl() {
                return consumerUrl;
            }
        };
        String host = NetUtils.getLocalHost();
        serviceInstancesChangedListener.addListenerAndNotify(consumerUrl, listener);
        polarisRegistryServiceDiscovery.addServiceInstancesChangedListener(serviceInstancesChangedListener);
        List<ServiceInstance> instances = buildInstanceUrls(svcName, host, 11300, count);
        for (ServiceInstance instance : instances) {
            polarisRegistryServiceDiscovery.doRegister(instance);
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        polarisRegistryServiceDiscovery.removeServiceInstancesChangedListener(serviceInstancesChangedListener);
        Assertions.assertTrue(notified.get());
        //Assertions.assertEquals(count, notifiedCount.get());
        for (ServiceInstance serviceInstance : instances) {
            polarisRegistryServiceDiscovery.doUnregister(serviceInstance);
        }
    }

    private static int countInstanceByHost(String host, List<ServiceInstance> instances) {
        int count = 0;
        for (ServiceInstance instance : instances) {
            if (StringUtils.isEquals(host, instance.getHost())) {
                count++;
            }
        }
        return count;
    }
}
