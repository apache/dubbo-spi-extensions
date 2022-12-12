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

package org.apache.dubbo.mock.filter;

import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * {@link AdminMockFilter} unit tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class AdminMockFilterTest {

    @InjectMocks
    private AdminMockFilter adminMockFilter;

    @Mock
    private Invoker invoker;

    @Mock
    private Invocation invocation;

    @Test
    public void testInvoke() {
        Result result = new AsyncRpcResult(null, invocation);
        Mockito.when(invoker.invoke(Mockito.any())).thenReturn(result);
        Object res1 = adminMockFilter.invoke(invoker, invocation);
        Assert.assertEquals(result, res1);
    }
}
