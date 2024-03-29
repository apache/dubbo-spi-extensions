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
package org.apache.dubbo.crossthread.interceptor;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.RpcContext;

import net.bytebuddy.asm.Advice;

public class RunnableOrCallableMethodInterceptor {

    @Advice.OnMethodEnter
    public static void onMethodEnter(
        @Advice.FieldValue(value = RunnableOrCallableActivation.FIELD_NAME_DUBBO_TAG, readOnly = false) String dubboTag) {
        // copy tag to RpcContext from RunnableOrCallable's field value
        RpcContext.getClientAttachment().setAttachment(CommonConstants.TAG_KEY, dubboTag);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onMethodExit() {
        // clear tag in RpcContext
        RpcContext.getClientAttachment().removeAttachment(CommonConstants.TAG_KEY);
    }

}
