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
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

public class UserSpecifiedAddressRouterTest {
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
    public void test() {
        Assertions.assertTrue(new UserSpecifiedAddressRouter<>(URL.valueOf("").setScopeModel(applicationModel.newModule())).supportContinueRoute());
    }

    @Test
    public void testNotify() {
        UserSpecifiedAddressRouter<Object> userSpecifiedAddressRouter = new UserSpecifiedAddressRouter<>(consumerUrl);
        Assertions.assertEquals(BitList.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNull(userSpecifiedAddressRouter.getIp2Invoker());
        userSpecifiedAddressRouter.notify(BitList.emptyList());
        Assertions.assertEquals(BitList.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNull(userSpecifiedAddressRouter.getIp2Invoker());

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.1", 0));

        // no address
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null));

        Assertions.assertNotNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getIp2Invoker());

        userSpecifiedAddressRouter.notify(BitList.emptyList());
        Assertions.assertEquals(BitList.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getIp2Invoker());
    }

    @Test
    public void testGetInvokerByURL() {
        UserSpecifiedAddressRouter<Object> userSpecifiedAddressRouter = new UserSpecifiedAddressRouter<>(consumerUrl);

        Assertions.assertEquals(BitList.emptyList(),
            userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null));

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880")));
        BitList<Invoker<Object>> invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20880, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        Invoker<Object> mockInvoker = Mockito.mock(Invoker.class);
        Mockito.when(mockInvoker.getUrl()).thenReturn(URL.valueOf("simple://127.0.0.1:20880?Test1=Value"));

        userSpecifiedAddressRouter.notify(new BitList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880")));
        invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        userSpecifiedAddressRouter.notify(new BitList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880?Test1=Value")));
        invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        userSpecifiedAddressRouter.notify(new BitList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("simple://127.0.0.1:20880")));
        invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("dubbo://127.0.0.1:20880")));
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20880, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20770")));
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20770, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20770?Test1=Value1")));
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20770, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value1", invokers.get(0).getUrl().getParameter("Test1"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.2:20770?Test1=Value1")));
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.2", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20770, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value1", invokers.get(0).getUrl().getParameter("Test1"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880?Test1=Value&Test2=Value&Test3=Value")));
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
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
    public void testGetInvokerByIp() {
        UserSpecifiedAddressRouter<Object> userSpecifiedAddressRouter = new UserSpecifiedAddressRouter<>(consumerUrl);

        Assertions.assertEquals(BitList.emptyList(),
            userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null));

        Invoker<Object> mockInvoker = Mockito.mock(Invoker.class);
        Mockito.when(mockInvoker.getUrl()).thenReturn(consumerUrl);

        userSpecifiedAddressRouter.notify(new BitList<>(Collections.singletonList(mockInvoker)));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 0));
        BitList<Invoker<Object>> invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20880));
        invokers = userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20770));
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class), false, null, null));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.3", 20880));
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.doRoute(new BitList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class), false, null, null));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20770, true));
        invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.2", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20770, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getVersion(), invokers.get(0).getUrl().getVersion());
        Assertions.assertEquals(consumerUrl.getGroup(), invokers.get(0).getUrl().getGroup());
    }

    @Test
    public void testRemovalTask() throws InterruptedException {
        UserSpecifiedAddressRouter.EXPIRE_TIME = 10;
        UserSpecifiedAddressRouter<Object> userSpecifiedAddressRouter = new UserSpecifiedAddressRouter<>(consumerUrl);

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880")));
        BitList<Invoker<Object>> invokers = userSpecifiedAddressRouter.doRoute(BitList.emptyList(), consumerUrl, Mockito.mock(Invocation.class), false, null, null);
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
