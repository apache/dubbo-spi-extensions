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
import org.apache.dubbo.rpc.cluster.support.FailoverClusterInvoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class UserSpecifiedAddressRouterTest {
    private ApplicationModel applicationModel;
    private URL consumerUrl;

    @BeforeEach
    public void setup() {
        consumerUrl = URL.valueOf("127.0.0.2:20880").addParameter("Test", "Value").addParameter("check", "false").addParameter("lazy","true")
            .addParameter("version", "1.0.0").addParameter("group", "Dubbo").addParameter("interface", DemoService.class.getName());
    }

    @Test
    void testNotify() {
        UserSpecifiedAddressRouter userSpecifiedAddressRouter = new UserSpecifiedAddressRouter(consumerUrl);
        Assertions.assertEquals(Collections.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNull(userSpecifiedAddressRouter.getIp2Invoker());
        userSpecifiedAddressRouter.notify(Collections.emptyList());
        Assertions.assertEquals(Collections.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNull(userSpecifiedAddressRouter.getIp2Invoker());

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.1", 0));
        FailoverClusterInvoker<Object> mockInvoker = Mockito.mock(FailoverClusterInvoker.class);
        AddressSpecifyClusterInterceptor interceptor = new AddressSpecifyClusterInterceptor();

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.1", 0));
        Invocation invocation = new RpcInvocation();
        interceptor.before(mockInvoker,invocation);

        // no address
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, invocation));

        Assertions.assertNotNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getIp2Invoker());

        userSpecifiedAddressRouter.notify(Collections.emptyList());
        Assertions.assertEquals(Collections.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getIp2Invoker());
    }

    @Test
    void testGetInvokerByURL() {
        UserSpecifiedAddressRouter userSpecifiedAddressRouter = new UserSpecifiedAddressRouter(consumerUrl);

        Assertions.assertEquals(Collections.emptyList(),
            userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, Mockito.mock(Invocation.class)));

        FailoverClusterInvoker<Object> mockInvoker = Mockito.mock(FailoverClusterInvoker.class);

        AddressSpecifyClusterInterceptor interceptor = new AddressSpecifyClusterInterceptor();

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880?lazy=true")));
        Invocation invocation = new RpcInvocation();
        interceptor.before(mockInvoker,invocation);

        List<Invoker<Object>> invokers = userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, invocation);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20880, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getParameter("version"), invokers.get(0).getUrl().getParameter("version"));
        Assertions.assertEquals(consumerUrl.getParameter("group"), invokers.get(0).getUrl().getParameter("group"));

        Mockito.when(mockInvoker.getUrl()).thenReturn(URL.valueOf("simple://127.0.0.1:20880?Test1=Value"));

        userSpecifiedAddressRouter.notify(new LinkedList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880")));
        Invocation invocation1 = new RpcInvocation();
        interceptor.before(mockInvoker,invocation1);
        invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation1);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        userSpecifiedAddressRouter.notify(new LinkedList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880?Test1=Value")));
        Invocation invocation2 = new RpcInvocation();
        interceptor.before(mockInvoker,invocation2);
        invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation2);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        userSpecifiedAddressRouter.notify(new LinkedList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("simple://127.0.0.1:20880")));
        Invocation invocation3 = new RpcInvocation();
        interceptor.before(mockInvoker,invocation3);
        invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation3);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880?Test1=Value&Test2=Value&Test3=Value")));
        Invocation invocation4 = new RpcInvocation();
        interceptor.before(mockInvoker,invocation4);
        invokers = userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, invocation4);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.1", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20880, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test1"));
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test2"));
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test3"));
        Assertions.assertEquals(consumerUrl.getParameter("version"), invokers.get(0).getUrl().getParameter("version"));
        Assertions.assertEquals(consumerUrl.getParameter("group"), invokers.get(0).getUrl().getParameter("group"));
    }

    @Test
    void testGetInvokerByIp() {
        UserSpecifiedAddressRouter userSpecifiedAddressRouter = new UserSpecifiedAddressRouter(consumerUrl);

        Assertions.assertEquals(Collections.emptyList(),
            userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, Mockito.mock(Invocation.class)));
        AddressSpecifyClusterInterceptor interceptor = new AddressSpecifyClusterInterceptor();

        FailoverClusterInvoker<Object> mockInvoker = Mockito.mock(FailoverClusterInvoker.class);
        Mockito.when(mockInvoker.getUrl()).thenReturn(consumerUrl);

        userSpecifiedAddressRouter.notify(new LinkedList<>(Collections.singletonList(mockInvoker)));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 0));
        Invocation invocation = new RpcInvocation();
        interceptor.before(mockInvoker,invocation);
        List<Invoker<Object>> invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class));
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20880));
        Invocation invocation1 = new RpcInvocation();
        interceptor.before(mockInvoker,invocation1);
        invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation1);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20770));
        Invocation invocation2 = new RpcInvocation();
        interceptor.before(mockInvoker,invocation2);
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation2));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.3", 20880));
        Invocation invocation3 = new RpcInvocation();
        interceptor.before(mockInvoker,invocation3);
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, invocation3));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20770,true));
        Invocation invocation4 = new RpcInvocation();
        interceptor.before(mockInvoker,invocation4);
        invokers = userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, invocation4);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("127.0.0.2", invokers.get(0).getUrl().getHost());
        Assertions.assertEquals(20770, invokers.get(0).getUrl().getPort());
        Assertions.assertEquals("Value", invokers.get(0).getUrl().getParameter("Test"));
        Assertions.assertEquals(consumerUrl.getParameter("version"), invokers.get(0).getUrl().getParameter("version"));
        Assertions.assertEquals(consumerUrl.getParameter("group"), invokers.get(0).getUrl().getParameter("group"));
    }
}
