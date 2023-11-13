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
package org.apache.dubbo.rpc.protocol.hessian;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.threadlocal.InternalThreadLocal;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

import java.util.HashMap;
import java.util.Map;

@Activate(group = CommonConstants.CONSUMER, order = Integer.MAX_VALUE)
public class HessianProtocolClientFilter implements Filter {
    private final static InternalThreadLocal<Map<String, String>> attachments = new InternalThreadLocal<>();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Map<String, String> attachments = new HashMap<>(RpcContext.getContext().getAttachments());
        attachments.putAll(invocation.getAttachments());
        HessianProtocolClientFilter.attachments.set(attachments);
        try {
            return invoker.invoke(invocation);
        } finally {
            HessianProtocolClientFilter.attachments.remove();
        }
    }

    public static Map<String, String> getAttachments() {
        return attachments.get();
    }
}
