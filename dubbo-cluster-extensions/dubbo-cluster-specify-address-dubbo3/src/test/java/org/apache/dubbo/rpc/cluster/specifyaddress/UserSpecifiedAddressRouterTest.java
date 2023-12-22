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
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

class UserSpecifiedAddressRouterTest {
    private ApplicationModel applicationModel;
    private URL consumerUrl;

    @BeforeEach
    public void setup() {
        applicationModel = ApplicationModel.defaultModel();
        ServiceModel serviceModel = Mockito.mock(ServiceModel.class);
        Mockito.when(serviceModel.getServiceInterfaceClass()).thenReturn((Class) DemoService.class);
        consumerUrl = URL.valueOf("127.0.0.2:20880").addParameter("Test", "Value").addParameter("check", "false")
            .addParameter("version", "1.0.0").addParameter("group", "Dubbo")
            .setScopeModel(applicationModel.newModule()).setServiceModel(serviceModel);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void test() {
        Assertions.assertTrue(new UserSpecifiedAddressRouter<>(URL.valueOf("").setScopeModel(applicationModel.newModule())).supportContinueRoute());
    }

    @Test
    void testNotify() {
        UserSpecifiedAddressRouter<Object> userSpecifiedAddressRouter = new UserSpecifiedAddressRouter<>(consumerUrl);
        Assertions.assertEquals(BitList.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNull(userSpecifiedAddressRouter.getIp2Invoker());
        userSpecifiedAddressRouter.notify(BitList.emptyList());
        Assertions.assertEquals(BitList.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNull(userSpecifiedAddressRouter.getIp2Invoker());

        Invocation invocation = new RpcInvocation();
        Invoker mockInvoker = Mockito.mock(Invoker.class);
        AddressSpecifyClusterFilter clusterFilter = new AddressSpecifyClusterFilter();
        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.1", 0));
        clusterFilter.invoke(mockInvoker,invocation);

        // no address
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, invocation, false, null, null));

        Assertions.assertNotNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getIp2Invoker());

        userSpecifiedAddressRouter.notify(BitList.emptyList());
        Assertions.assertEquals(BitList.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getIp2Invoker());
    }

    @Test
    void testGetInvokerByURL() {
        UserSpecifiedAddressRouter<Object> userSpecifiedAddressRouter = new UserSpecifiedAddressRouter<>(consumerUrl);

        Assertions.assertEquals(BitList.emptyList(),
            userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null));

        Invocation invocation = new RpcInvocation();
        Invoker<Object> mockInvoker = Mockito.mock(Invoker.class);
        AddressSpecifyClusterFilter clusterFilter = new AddressSpecifyClusterFilter();
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880")));
        clusterFilter.invoke(mockInvoker,invocation);

        BitList<Invoker<Object>> invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, invocation, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20880, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        Mockito.when(mockInvoker.getUrl()).thenReturn(URL.valueOf("simple://127.0.0.1:20880?Test1=Value"));

        userSpecifiedAddressRouter.notify(new BitList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880")));
        Invocation invocation1 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation1);

        invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation1, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        userSpecifiedAddressRouter.notify(new BitList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880?Test1=Value")));
        Invocation invocation2 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation2);
        invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation2, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        userSpecifiedAddressRouter.notify(new BitList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("simple://127.0.0.1:20880")));
        Invocation invocation3 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation3);
        invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation3, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("dubbo://127.0.0.1:20880")));
        Invocation invocation4 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation4);
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, invocation4, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20880, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20770")));
        Invocation invocation5 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation5);

        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, invocation5, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20770, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20770?Test1=Value1")));
        Invocation invocation6 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation6);
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, invocation6, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20770, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value1", invokers.get(0).getUrl().getParameter("Test1"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.2:20770?Test1=Value1")));
        Invocation invocation7 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation7);
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, invocation7, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.2", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20770, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value1", invokers.get(0).getUrl().getParameter("Test1"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880?Test1=Value&Test2=Value&Test3=Value")));
        Invocation invocation8 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation8);
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, invocation8, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20880, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test1"));
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test2"));
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test3"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());
    }

    @Test
    void testGetInvokerByIp() {
        UserSpecifiedAddressRouter<Object> userSpecifiedAddressRouter = new UserSpecifiedAddressRouter<>(consumerUrl);

        Assertions.assertEquals(BitList.emptyList(),
            userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null));

        Invoker<Object> mockInvoker = Mockito.mock(Invoker.class);
        AddressSpecifyClusterFilter clusterFilter = new AddressSpecifyClusterFilter();
        Mockito.when(mockInvoker.getUrl()).thenReturn(consumerUrl);

        userSpecifiedAddressRouter.notify(new BitList<>(Collections.singletonList(mockInvoker)));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 0));
        Invocation invocation = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation);
        BitList<Invoker<Object>> invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20880));
        Invocation invocation1 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation1);
        invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation1, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20770));
        Invocation invocation2 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation2);
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation2, false, null, null));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.3", 20880));
        Invocation invocation3 = new RpcInvocation();
        clusterFilter.invoke(mockInvoker,invocation3);
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation3, false, null, null));

        Invocation invocation4 = new RpcInvocation();
        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20770,true));
        clusterFilter.invoke(mockInvoker,invocation4);
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, invocation4, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.2", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20770, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRemovalTask() throws InterruptedException {
        UserSpecifiedAddressRouter.EXPIRE_TIME = 10;
        UserSpecifiedAddressRouter<Object> userSpecifiedAddressRouter = new UserSpecifiedAddressRouter<>(consumerUrl);

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880")));
        Invocation invocation = new RpcInvocation();
        Invoker<Object> mockInvoker = Mockito.mock(Invoker.class);
        AddressSpecifyClusterFilter clusterFilter = new AddressSpecifyClusterFilter();
        clusterFilter.invoke(mockInvoker,invocation);
        BitList<Invoker<Object>> invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, invocation, false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20880, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        Assertions.assertEquals(1, userSpecifiedAddressRouter.getNewInvokerCache().size());
        Thread.sleep(50);
        Assertions.assertEquals(0, userSpecifiedAddressRouter.getNewInvokerCache().size());

        userSpecifiedAddressRouter.stop();
        UserSpecifiedAddressRouter.EXPIRE_TIME = 10 * 60 * 1000;

    }
}
