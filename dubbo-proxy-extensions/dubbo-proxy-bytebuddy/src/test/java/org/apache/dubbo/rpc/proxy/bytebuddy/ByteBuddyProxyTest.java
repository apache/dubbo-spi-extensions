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
package org.apache.dubbo.rpc.proxy.bytebuddy;

import org.apache.dubbo.rpc.proxy.InvokerInvocationHandler;
import org.apache.dubbo.rpc.proxy.RemoteService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;

import static org.mockito.ArgumentMatchers.any;

class ByteBuddyProxyTest {

    @Test
    void testNewInstance() throws Throwable {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InvokerInvocationHandler handler = Mockito.mock(InvokerInvocationHandler.class);
        Object proxy = ByteBuddyProxy.newInstance(cl, new Class<?>[]{RemoteService.class}, handler);
        Assertions.assertTrue(proxy instanceof RemoteService);
        Assertions.assertTrue(proxy instanceof Proxy);
        RemoteService remoteService = (RemoteService) proxy;
        remoteService.getThreadName();
        remoteService.sayHello("test");
        Mockito.verify(handler, Mockito.times(2)).invoke(any(), any(), any());
    }
}
