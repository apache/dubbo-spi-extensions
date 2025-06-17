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
package org.apache.dubbo.seata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

class SeataTransactionPropagationProviderFilterTest {
    private final AtomicReference<Function<Invocation, Result>> invokeFunction = new AtomicReference<>();

    private Invoker invoker = new Invoker() {
        @Override
        public Class getInterface() {
            return null;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return invokeFunction.get().apply(invocation);
        }

        @Override
        public URL getUrl() {
            return URL.valueOf("dubbo://localhost");
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void destroy() {

        }
    };

    @Test
    void test() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        ModuleModel moduleModel = applicationModel.newModule();

        Filter filter = moduleModel.getExtension(Filter.class, "seata-provider");
        Assertions.assertTrue(filter instanceof SeataTransactionPropagationProviderFilter);

        Invocation invocation = Mockito.mock(Invocation.class);
        invokeFunction.set((inv) -> {
            Assertions.assertNull(RootContext.getXID());
            Assertions.assertNull(RootContext.getBranchType());
            return null;
        });
        filter.invoke(invoker, invocation);

        Mockito.when(invocation.getAttachment(RootContext.KEY_XID)).thenReturn("1234");
        invokeFunction.set((inv) -> {
            Assertions.assertEquals("1234", RootContext.getXID());
            Assertions.assertEquals(BranchType.AT, RootContext.getBranchType());
            return null;
        });
        filter.invoke(invoker, invocation);

        Mockito.when(invocation.getAttachment(RootContext.KEY_XID)).thenReturn(null);
        Mockito.when(invocation.getAttachment(RootContext.KEY_XID.toLowerCase())).thenReturn("123456");
        invokeFunction.set((inv) -> {
            Assertions.assertEquals("123456", RootContext.getXID());
            Assertions.assertEquals(BranchType.AT, RootContext.getBranchType());
            return null;
        });
        filter.invoke(invoker, invocation);

        Mockito.when(invocation.getAttachment(RootContext.KEY_BRANCH_TYPE)).thenReturn("TCC");
        invokeFunction.set((inv) -> {
            Assertions.assertEquals("123456", RootContext.getXID());
            Assertions.assertEquals(BranchType.TCC, RootContext.getBranchType());
            return null;
        });
        filter.invoke(invoker, invocation);

        Mockito.when(invocation.getAttachment(RootContext.KEY_BRANCH_TYPE)).thenReturn(null);
        Mockito.when(invocation.getAttachment(RootContext.KEY_BRANCH_TYPE.toLowerCase())).thenReturn("TCC");
        invokeFunction.set((inv) -> {
            Assertions.assertEquals("123456", RootContext.getXID());
            Assertions.assertEquals(BranchType.TCC, RootContext.getBranchType());
            return null;
        });
        filter.invoke(invoker, invocation);
        Assertions.assertNull(RootContext.getXID());
        Assertions.assertNull(RootContext.getBranchType());

        Mockito.when(invocation.getAttachment(RootContext.KEY_BRANCH_TYPE.toLowerCase())).thenReturn("AT");
        invokeFunction.set((inv) -> {
            RootContext.bind("12345678");
            return null;
        });
        filter.invoke(invoker, invocation);
        Assertions.assertEquals("12345678", RootContext.getXID());
        Assertions.assertEquals(BranchType.AT, RootContext.getBranchType());
        RootContext.unbind();
        RootContext.unbindBranchType();

        Mockito.when(invocation.getAttachment(RootContext.KEY_BRANCH_TYPE.toLowerCase())).thenReturn("AT");
        invokeFunction.set((inv) -> {
            RootContext.bind("12345678");
            RootContext.bindBranchType(BranchType.TCC);
            return null;
        });
        filter.invoke(invoker, invocation);
        Assertions.assertEquals("12345678", RootContext.getXID());
        Assertions.assertEquals(BranchType.TCC, RootContext.getBranchType());
        RootContext.unbind();
        RootContext.unbindBranchType();

        moduleModel.destroy();
    }
}
