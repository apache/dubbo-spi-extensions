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

import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SeataTransactionPropagationConsumerFilterTest {
    @Test
    public void test() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        ModuleModel moduleModel = applicationModel.newModule();

        Filter filter = moduleModel.getExtension(Filter.class, "seata-consumer");
        Assertions.assertTrue(filter instanceof SeataTransactionPropagationConsumerFilter);


        Invoker invoker = Mockito.mock(Invoker.class);
        Invocation invocation = Mockito.mock(Invocation.class);
        filter.invoke(invoker, invocation);

        Mockito.verify(invoker, Mockito.times(1)).invoke(invocation);
        Mockito.verify(invocation, Mockito.times(0)).setAttachment(Mockito.any(), Mockito.any());

        RootContext.bind("123456");
        filter.invoke(invoker, invocation);
        Mockito.verify(invoker, Mockito.times(2)).invoke(invocation);
        Mockito.verify(invocation, Mockito.times(1)).setAttachment(RootContext.KEY_XID, "123456");
        Mockito.verify(invocation, Mockito.times(2)).setAttachment(Mockito.any(), Mockito.any());

        RootContext.bind("12345678");
        RootContext.bindBranchType(BranchType.SAGA);
        filter.invoke(invoker, invocation);
        Mockito.verify(invoker, Mockito.times(3)).invoke(invocation);
        Mockito.verify(invocation, Mockito.times(1)).setAttachment(RootContext.KEY_XID, "12345678");
        Mockito.verify(invocation, Mockito.times(1)).setAttachment(RootContext.KEY_BRANCH_TYPE, "SAGA");
        Mockito.verify(invocation, Mockito.times(4)).setAttachment(Mockito.any(), Mockito.any());

        moduleModel.destroy();
    }
}
