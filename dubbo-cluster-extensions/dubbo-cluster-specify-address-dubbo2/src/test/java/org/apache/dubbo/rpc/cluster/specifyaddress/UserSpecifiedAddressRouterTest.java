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
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UserSpecifiedAddressRouterTest {
    private ApplicationModel applicationModel;
    private URL consumerUrl;

    @BeforeEach
    public void setup() {
        consumerUrl = URL.valueOf("127.0.0.2:20880").addParameter("Test", "Value").addParameter("check", "false")
            .addParameter("version", "1.0.0").addParameter("group", "Dubbo");
    }

    @Test
    public void testNotify() {
        UserSpecifiedAddressRouter userSpecifiedAddressRouter = new UserSpecifiedAddressRouter(consumerUrl);
        Assertions.assertEquals(Collections.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNull(userSpecifiedAddressRouter.getIp2Invoker());
        userSpecifiedAddressRouter.notify(Collections.emptyList());
        Assertions.assertEquals(Collections.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNull(userSpecifiedAddressRouter.getIp2Invoker());

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.1", 0));

        // no address
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, Mockito.mock(Invocation.class)));

        Assertions.assertNotNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getIp2Invoker());

        userSpecifiedAddressRouter.notify(Collections.emptyList());
        Assertions.assertEquals(Collections.emptyList(), userSpecifiedAddressRouter.getInvokers());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getAddress2Invoker());
        Assertions.assertNotNull(userSpecifiedAddressRouter.getIp2Invoker());
    }

    @Test
    public void testGetInvokerByURL() {
        UserSpecifiedAddressRouter userSpecifiedAddressRouter = new UserSpecifiedAddressRouter(consumerUrl);

        Assertions.assertEquals(Collections.emptyList(),
            userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, Mockito.mock(Invocation.class)));

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880")));
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, Mockito.mock(Invocation.class)));

        Invoker<Object> mockInvoker = Mockito.mock(Invoker.class);
        Mockito.when(mockInvoker.getUrl()).thenReturn(URL.valueOf("simple://127.0.0.1:20880?Test1=Value"));

        userSpecifiedAddressRouter.notify(new LinkedList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880")));
        List<Invoker<Object>> invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class));
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        userSpecifiedAddressRouter.notify(new LinkedList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880?Test1=Value")));
        invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class));
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        userSpecifiedAddressRouter.notify(new LinkedList<>(Collections.singletonList(mockInvoker)));
        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("simple://127.0.0.1:20880")));
        invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class));
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address(URL.valueOf("127.0.0.1:20880?Test1=Value&Test2=Value&Test3=Value")));
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, Mockito.mock(Invocation.class)));
    }

    @Test
    public void testGetInvokerByIp() {
        UserSpecifiedAddressRouter userSpecifiedAddressRouter = new UserSpecifiedAddressRouter(consumerUrl);

        Assertions.assertEquals(Collections.emptyList(),
            userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, Mockito.mock(Invocation.class)));

        Invoker<Object> mockInvoker = Mockito.mock(Invoker.class);
        Mockito.when(mockInvoker.getUrl()).thenReturn(consumerUrl);

        userSpecifiedAddressRouter.notify(new LinkedList<>(Collections.singletonList(mockInvoker)));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 0));
        List<Invoker<Object>> invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class));
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20880));
        invokers = userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class));
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(mockInvoker, invokers.get(0));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20770));
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class)));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.3", 20880));
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.route(new LinkedList<>(Collections.singletonList(mockInvoker)), consumerUrl, Mockito.mock(Invocation.class)));

        UserSpecifiedAddressUtil.setAddress(new Address("127.0.0.2", 20770, true));
        Assertions.assertThrows(RpcException.class, () ->
            userSpecifiedAddressRouter.route(Collections.emptyList(), consumerUrl, Mockito.mock(Invocation.class)));
    }
}
