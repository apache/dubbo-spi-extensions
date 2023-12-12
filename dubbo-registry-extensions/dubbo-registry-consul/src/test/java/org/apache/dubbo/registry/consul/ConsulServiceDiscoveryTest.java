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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConsulServiceDiscoveryTest {

    private URL url;
    private ConsulServiceDiscovery consulServiceDiscovery;
    private ConsulProcess consul;
    private static final String SERVICE_NAME = "A";
    private static final String LOCALHOST = "127.0.0.1";

    private ApplicationModel applicationModel;

    @BeforeEach
    public void init() throws Exception {
        this.applicationModel = FrameworkModel.defaultModel().newApplication();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("A"));
        this.consul = ConsulStarterBuilder.consulStarter()
                .build()
                .start();
        url = URL.valueOf("consul://localhost:" + consul.getHttpPort()).addParameter("consul-check-pass-interval", "1000");
        consulServiceDiscovery = new ConsulServiceDiscovery(applicationModel, url);
    }

    @AfterEach
    public void close() throws Exception {
        consulServiceDiscovery.destroy();
        consul.close();
        this.applicationModel.destroy();
    }

    @Test
    public void testRegister() throws InterruptedException {
        ServiceInstance serviceInstance = new DefaultServiceInstance(SERVICE_NAME, LOCALHOST, 8080, applicationModel);
        serviceInstance.getMetadata().put("test", "test");
        serviceInstance.getMetadata().put("test123", "test");
        consulServiceDiscovery.doRegister(serviceInstance);
        Thread.sleep(3000);
        List<ServiceInstance> serviceInstances = consulServiceDiscovery.getInstances(SERVICE_NAME);
        assertEquals(1, serviceInstances.size());
        assertEquals(serviceInstance, serviceInstances.get(0));

        ServiceInstance serviceInstance2 = new DefaultServiceInstance(SERVICE_NAME, LOCALHOST, 8081, applicationModel);
        serviceInstance2.getMetadata().put("test", "test");
        serviceInstance2.getMetadata().put("test123", "test");
        consulServiceDiscovery.doRegister(serviceInstance2);

        Thread.sleep(3000);
        serviceInstances = consulServiceDiscovery.getInstances(SERVICE_NAME);
        assertEquals(2, serviceInstances.size());
        assertEquals(Arrays.asList(serviceInstance, serviceInstance2), serviceInstances);

        consulServiceDiscovery.doUnregister(serviceInstance);
        Thread.sleep(3000);
        serviceInstances = consulServiceDiscovery.getInstances(SERVICE_NAME);
        assertEquals(1, serviceInstances.size());
        assertEquals(serviceInstance2, serviceInstances.get(0));

        consulServiceDiscovery.doUnregister(serviceInstance2);
        Thread.sleep(3000);
        serviceInstances = consulServiceDiscovery.getInstances(SERVICE_NAME);
        assertEquals(0, serviceInstances.size());
    }

    @Test
    public void testNotify() throws InterruptedException {
        ServiceInstancesChangedListener serviceInstancesChangedListener1 = Mockito.spy(new ServiceInstancesChangedListener(Collections.singleton(SERVICE_NAME), consulServiceDiscovery));
        ServiceInstancesChangedListener serviceInstancesChangedListener2 = Mockito.spy(new ServiceInstancesChangedListener(Collections.singleton(SERVICE_NAME), consulServiceDiscovery));
        consulServiceDiscovery.addServiceInstancesChangedListener(serviceInstancesChangedListener1);
        consulServiceDiscovery.addServiceInstancesChangedListener(serviceInstancesChangedListener2);
        consulServiceDiscovery.removeServiceInstancesChangedListener(serviceInstancesChangedListener2);
        ArgumentCaptor<ServiceInstancesChangedEvent> eventArgumentCaptor = ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);

        ServiceInstance serviceInstance = new DefaultServiceInstance(SERVICE_NAME, LOCALHOST, 8080, applicationModel);
        serviceInstance.getMetadata().put("test", "test");
        serviceInstance.getMetadata().put("test123", "test");
        consulServiceDiscovery.doRegister(serviceInstance);
        Thread.sleep(3000);

        Mockito.verify(serviceInstancesChangedListener1, Mockito.atLeast(1)).onEvent(eventArgumentCaptor.capture());
        List<ServiceInstance> serviceInstances = eventArgumentCaptor.getValue().getServiceInstances();
        assertEquals(1, serviceInstances.size());
        assertEquals(serviceInstance, serviceInstances.get(0));

        ServiceInstance serviceInstance2 = new DefaultServiceInstance(SERVICE_NAME, LOCALHOST, 8081, applicationModel);
        serviceInstance2.getMetadata().put("test", "test");
        serviceInstance2.getMetadata().put("test123", "test");
        consulServiceDiscovery.doRegister(serviceInstance2);
        Thread.sleep(3000);

        Mockito.verify(serviceInstancesChangedListener1, Mockito.atLeast(2)).onEvent(eventArgumentCaptor.capture());
        serviceInstances = eventArgumentCaptor.getValue().getServiceInstances();
        assertEquals(2, serviceInstances.size());
        assertEquals(Arrays.asList(serviceInstance, serviceInstance2), serviceInstances);

        consulServiceDiscovery.doUnregister(serviceInstance);
        Thread.sleep(3000);
        Mockito.verify(serviceInstancesChangedListener1, Mockito.atLeast(3)).onEvent(eventArgumentCaptor.capture());
        serviceInstances = eventArgumentCaptor.getValue().getServiceInstances();
        assertEquals(1, serviceInstances.size());
        assertEquals(serviceInstance2, serviceInstances.get(0));

        consulServiceDiscovery.doUnregister(serviceInstance2);
        Thread.sleep(3000);
        Mockito.verify(serviceInstancesChangedListener1, Mockito.atLeast(4)).onEvent(eventArgumentCaptor.capture());
        serviceInstances = eventArgumentCaptor.getValue().getServiceInstances();
        assertEquals(0, serviceInstances.size());

        Mockito.verify(serviceInstancesChangedListener2, Mockito.never()).onEvent(Mockito.any());
    }
}
