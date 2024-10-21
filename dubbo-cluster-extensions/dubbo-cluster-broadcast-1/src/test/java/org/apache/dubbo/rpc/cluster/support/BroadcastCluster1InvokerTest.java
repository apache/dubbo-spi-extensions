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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @see BroadcastCluster1Invoker
 */
public class BroadcastCluster1InvokerTest {
    private URL url;
    private Directory<DemoService> dic;
    private RpcInvocation invocation;
    private BroadcastCluster1Invoker clusterInvoker;

    private MockInvoker invoker1;
    private MockInvoker invoker2;
    private MockInvoker invoker3;
    private MockInvoker invoker4;

    @BeforeEach
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        invoker1 = new MockInvoker();
        invoker2 = new MockInvoker();
        invoker3 = new MockInvoker();
        invoker4 = new MockInvoker();

        url = URL.valueOf("test://127.0.0.1:8080/test");
        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.getInterface()).willReturn(DemoService.class);

        invocation = new RpcInvocation();
        invocation.setMethodName("test");

        clusterInvoker = new BroadcastCluster1Invoker(dic);
    }

    @Test
    void testNormal() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        // Every invoker will be called
        clusterInvoker.invoke(invocation);
        assertTrue(invoker1.isInvoked());
        assertTrue(invoker2.isInvoked());
        assertTrue(invoker3.isInvoked());
        assertTrue(invoker4.isInvoked());
    }

    @Test
    void testEx() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        invoker1.invokeThrowEx();
        invoker2.invokeThrowEx();
        invoker3.invokeThrowEx();
        invoker4.invokeThrowEx();
        Throwable exception = clusterInvoker.invoke(invocation).getException();
        assertInstanceOf(RpcException.class, exception);
        assertTrue(exception.getMessage().contains("throwEx is true"));
        assertTrue(invoker1.isInvoked());
        assertTrue(invoker2.isInvoked());
        assertTrue(invoker3.isInvoked());
        assertTrue(invoker4.isInvoked());
    }

    @Test
    void testFailByOneInvoker() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        // Once there is an exception, the result is abnormal.
        invoker1.invokeThrowEx();
        Throwable exception = clusterInvoker.invoke(invocation).getException();
        assertInstanceOf(RpcException.class, exception);
        assertTrue(exception.getMessage().contains("throwEx is true"));
    }

    @Test
    void testNoProvider() {
        given(dic.list(invocation)).willReturn(Collections.emptyList());
        RpcException exception = assertThrows(RpcException.class, () ->
            clusterInvoker.invoke(invocation)
        );
        assertTrue(exception.getMessage().contains("No provider available"));
    }
}

class MockInvoker implements Invoker<DemoService> {
    private URL url = URL.valueOf("test://127.0.0.1:8080/test");
    private boolean throwEx = false;
    private boolean invoked = false;

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {}

    @Override
    public Class<DemoService> getInterface() {
        return DemoService.class;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        invoked = true;
        if (throwEx) {
//            throwEx = false;
            throw new RpcException("throwEx is true");
        }
        return new AppResponse("sucess");
    }

    public void invokeThrowEx() {
        throwEx = true;
    }

    public boolean isInvoked() {
        return invoked;
    }
}
