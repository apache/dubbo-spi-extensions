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

package org.apache.dubbo.service.discovery.springcloud.test;

import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringCloudServiceInstanceNotificationCustomizerTest {
    private SpringCloudServiceInstanceNotificationCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new SpringCloudServiceInstanceNotificationCustomizer();
    }

    @Test
    void testCustomize() {
        // Mock ServiceInstance
        ServiceInstance instance = Mockito.spy(new DefaultServiceInstance());
        Mockito.when(instance.getMetadata("preserved.register.source")).thenReturn("SPRING_CLOUD");
        Mockito.when(instance.getServiceName()).thenReturn("myService");
        Mockito.when(instance.getAddress()).thenReturn("127.0.0.1");
        Mockito.when(instance.getPort()).thenReturn(8080);

        List<ServiceInstance> instances = Collections.singletonList(instance);
        // Call customize method with the mocked instance
        customizer.customize(instances);

        // Verify that the metadata is correctly set
        Mockito.verify(instance).setServiceMetadata(Mockito.any(MetadataInfo.class));

        // Check if MetadataInfo is set properly
        MetadataInfo metadataInfo = instance.getServiceMetadata();
        assertNotNull(metadataInfo);

        // Additional assertions to verify metadata details
        assertTrue(metadataInfo.getRevision().startsWith("SPRING_CLOUD"));
    }
}
