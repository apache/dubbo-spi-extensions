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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

public class DefaultUserSpecifiedServiceAddressBuilderTest {
    @Test
    public void testBuild() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();

        DefaultUserSpecifiedServiceAddressBuilder defaultUserSpecifiedServiceAddressBuilder = new DefaultUserSpecifiedServiceAddressBuilder(applicationModel);

        Address address = new Address("127.0.0.1", 0);
        ServiceModel serviceModel = Mockito.mock(ServiceModel.class);
        URL consumerUrl = URL.valueOf("").addParameter("Test", "Value").setScopeModel(applicationModel).setServiceModel(serviceModel);
        URL url = defaultUserSpecifiedServiceAddressBuilder.buildAddress(Collections.emptyList(), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(20880, url.getPort());
        Assertions.assertEquals("Value", url.getParameter("Test"));
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        address = new Address("127.0.0.1", 20770);
        url = defaultUserSpecifiedServiceAddressBuilder.buildAddress(Collections.emptyList(), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(20770, url.getPort());
        Assertions.assertEquals("Value", url.getParameter("Test"));
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        URL invokerUrl = URL.valueOf("127.0.0.2:20660").addParameter("Test1", "Value1").setScopeModel(applicationModel).setServiceModel(serviceModel);
        Invoker invoker = Mockito.mock(Invoker.class);
        Mockito.when(invoker.getUrl()).thenReturn(invokerUrl);

        address = new Address("127.0.0.1", 20770);
        url = defaultUserSpecifiedServiceAddressBuilder.buildAddress(Collections.singletonList(invoker), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(20770, url.getPort());
        Assertions.assertEquals("Value1", url.getParameter("Test1"));
        Assertions.assertNull(url.getParameter("Test"));
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        address = new Address("127.0.0.1", 0);
        url = defaultUserSpecifiedServiceAddressBuilder.buildAddress(Collections.singletonList(invoker), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(20660, url.getPort());
        Assertions.assertEquals("Value1", url.getParameter("Test1"));
        Assertions.assertNull(url.getParameter("Test"));
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        applicationModel.destroy();
    }

    @Test
    public void testReBuild() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();

        DefaultUserSpecifiedServiceAddressBuilder defaultUserSpecifiedServiceAddressBuilder = new DefaultUserSpecifiedServiceAddressBuilder(applicationModel);

        Address address = new Address(URL.valueOf("127.0.0.1:12345?Test=Value"));
        ServiceModel serviceModel = Mockito.mock(ServiceModel.class);
        URL consumerUrl = URL.valueOf("127.0.0.2:20880").addParameter("Test", "Value")
            .addParameter("version", "1.0.0").addParameter("group", "Dubbo")
            .setScopeModel(applicationModel).setServiceModel(serviceModel);
        URL url = defaultUserSpecifiedServiceAddressBuilder.rebuildAddress(Collections.emptyList(), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(12345, url.getPort());
        Assertions.assertEquals("Value", url.getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), url.getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), url.getGroup());
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        address = new Address(URL.valueOf("127.0.0.1:12345?Test=Value1"));
        consumerUrl = URL.valueOf("127.0.0.2:20880").addParameter("Test", "Value")
            .addParameter("version", "1.0.0").addParameter("group", "Dubbo")
            .setScopeModel(applicationModel).setServiceModel(serviceModel);
        url = defaultUserSpecifiedServiceAddressBuilder.rebuildAddress(Collections.emptyList(), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(12345, url.getPort());
        Assertions.assertEquals("Value", url.getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), url.getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), url.getGroup());
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        address = new Address(URL.valueOf("127.0.0.1:12345?Test1=Value1"));
        consumerUrl = URL.valueOf("127.0.0.2:20880").addParameter("Test", "Value")
            .addParameter("version", "1.0.0").addParameter("group", "Dubbo")
            .setScopeModel(applicationModel).setServiceModel(serviceModel);
        url = defaultUserSpecifiedServiceAddressBuilder.rebuildAddress(Collections.emptyList(), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(12345, url.getPort());
        Assertions.assertEquals("Value", url.getParameter("Test"));
        Assertions.assertEquals("Value1", url.getParameter("Test1"));
        Assertions.assertEquals(consumerUrl.getVersion(), url.getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), url.getGroup());
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        address = new Address(URL.valueOf("127.0.0.1:12345?Test1=Value1"));
        consumerUrl = URL.valueOf("127.0.0.2:20880").addParameter("Test", "Value")
            .addParameter("version", "1.0.0")
            .setScopeModel(applicationModel).setServiceModel(serviceModel);
        url = defaultUserSpecifiedServiceAddressBuilder.rebuildAddress(Collections.emptyList(), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(12345, url.getPort());
        Assertions.assertEquals("Value", url.getParameter("Test"));
        Assertions.assertEquals("Value1", url.getParameter("Test1"));
        Assertions.assertEquals(consumerUrl.getVersion(), url.getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), url.getGroup());
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        address = new Address(URL.valueOf("127.0.0.1:12345?Test1=Value1"));
        consumerUrl = URL.valueOf("127.0.0.2:20880").addParameter("Test", "Value")
            .addParameter("group", "Dubbo")
            .setScopeModel(applicationModel).setServiceModel(serviceModel);
        url = defaultUserSpecifiedServiceAddressBuilder.rebuildAddress(Collections.emptyList(), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(12345, url.getPort());
        Assertions.assertEquals("Value", url.getParameter("Test"));
        Assertions.assertEquals("Value1", url.getParameter("Test1"));
        Assertions.assertEquals(consumerUrl.getVersion(), url.getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), url.getGroup());
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        address = new Address(URL.valueOf("127.0.0.1:12345?Test1=Value1"));
        consumerUrl = URL.valueOf("127.0.0.2:20880").addParameter("Test", "Value")
            .setScopeModel(applicationModel).setServiceModel(serviceModel);
        url = defaultUserSpecifiedServiceAddressBuilder.rebuildAddress(Collections.emptyList(), address, Mockito.mock(Invocation.class), consumerUrl);
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(12345, url.getPort());
        Assertions.assertEquals("Value", url.getParameter("Test"));
        Assertions.assertEquals("Value1", url.getParameter("Test1"));
        Assertions.assertEquals(consumerUrl.getVersion(), url.getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), url.getGroup());
        Assertions.assertEquals(serviceModel, url.getServiceModel());
        Assertions.assertEquals(applicationModel, url.getScopeModel());

        applicationModel.destroy();
    }
}
