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

import org.apache.dubbo.common.beanutil.JavaBeanDescriptor;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadlocal.InternalThreadLocal;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;

import java.util.Arrays;
import java.util.stream.Stream;

public class UserSpecifiedAddressUtil {

    private final static Logger logger = LoggerFactory.getLogger(UserSpecifiedAddressUtil.class);
    private final static InternalThreadLocal<Address> ADDRESS = new InternalThreadLocal<>();

    /**
     * Set specified address to next invoke
     *
     * @param address specified address
     */
    public static void setAddress(Address address) {
        ADDRESS.set(address);
    }

    public static Address getAddress() {
        try {
            return ADDRESS.get();
        } finally {
            // work once
            ADDRESS.remove();
        }
    }

    public static void convertParameterTypeToJavaBeanDescriptor(Invocation invocation) {
        if (!(invocation instanceof RpcInvocation)) {
            logger.warn("Non-RpcInvocation type, gateway mode does not take effect, type:" + invocation.getClass().getName());
            return;
        }
        Class<?>[] parameterTypes = invocation.getParameterTypes();
        invocation.setObjectAttachment("originParameterType", getDesc(parameterTypes));

        Arrays.fill(parameterTypes, JavaBeanDescriptor.class);

        Object[] arguments = invocation.getArguments();
        for (int i = 0; i < arguments.length; i++) {
            JavaBeanDescriptor jbdArg = JavaBeanSerializeUtil.serialize(arguments[i]);
            arguments[i] = jbdArg;
        }

        ((RpcInvocation) invocation).setParameterTypesDesc(ReflectUtils.getDesc(parameterTypes));
        ((RpcInvocation) invocation).setCompatibleParamSignatures(Stream.of(parameterTypes).map(Class::getName).toArray(String[]::new));
    }

    private static String[] getDesc(Class<?>[] parameterTypes) {
        return Arrays.stream(parameterTypes).map(Class::getName).toArray(String[]::new);
    }
}
