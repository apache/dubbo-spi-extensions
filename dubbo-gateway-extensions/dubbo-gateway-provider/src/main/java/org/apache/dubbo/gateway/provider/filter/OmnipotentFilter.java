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
package org.apache.dubbo.gateway.provider.filter;

import org.apache.dubbo.common.beanutil.JavaBeanAccessor;
import org.apache.dubbo.common.beanutil.JavaBeanDescriptor;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.gateway.common.OmnipotentCommonConstants;
import org.apache.dubbo.gateway.provider.OmnipotentService;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

/**
 * Set the method name, formal parameters, and actual parameters for
 * the invokeOmn method of the Omnipotent generalized service
 */
@Activate(group = CommonConstants.PROVIDER, order = -21000)
public class OmnipotentFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {

        if (isOmnipotent(invoker.getInterface())) {
            setOmnArgs(inv);
            inv.getObjectAttachments().remove(OmnipotentCommonConstants.ORIGIN_GENERIC_PARAMETER_TYPES);
            RpcContext.getServerAttachment().removeAttachment(OmnipotentCommonConstants.ORIGIN_GENERIC_PARAMETER_TYPES);
        }

        return invoker.invoke(inv);
    }

    private boolean isOmnipotent(Class<?> interfaceClass) {
        return OmnipotentService.class.isAssignableFrom(interfaceClass);
    }

    // Restore method information before actual call
    private void setOmnArgs(Invocation inv) {
        Class<?>[] parameterTypes = (Class<?>[]) inv.getObjectAttachment(OmnipotentCommonConstants.ORIGIN_GENERIC_PARAMETER_TYPES, new Class<?>[]{Invocation.class});
        Object[] arguments = inv.getArguments();

        Object[] args = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            // In gateway mode, consumer has used JavaBeanDescriptor as parameter
            if (arguments[i] instanceof JavaBeanDescriptor) {
                args[i] = arguments[i];
            } else {
                args[i] = JavaBeanSerializeUtil.serialize(arguments[i], JavaBeanAccessor.METHOD);
            }
        }

        RpcInvocation rpcInvocation = new RpcInvocation(inv);
        // method
        rpcInvocation.setMethodName(inv.getAttachment(OmnipotentCommonConstants.ORIGIN_METHOD_KEY));
        rpcInvocation.setParameterTypes(parameterTypes);
        rpcInvocation.setArguments(args);
        rpcInvocation.setParameterTypesDesc(inv.getAttachment(OmnipotentCommonConstants.ORIGIN_PARAMETER_TYPES_DESC));

        // attachment
        rpcInvocation.setAttachment(PATH_KEY, inv.getAttachment(OmnipotentCommonConstants.ORIGIN_PATH_KEY));
        rpcInvocation.setAttachment(VERSION_KEY, inv.getAttachment(OmnipotentCommonConstants.ORIGIN_VERSION_KEY));
        rpcInvocation.setAttachment(GROUP_KEY, inv.getAttachment(OmnipotentCommonConstants.ORIGIN_GROUP_KEY));
        ((RpcInvocation) inv).setArguments(new Object[]{rpcInvocation});

    }

}
