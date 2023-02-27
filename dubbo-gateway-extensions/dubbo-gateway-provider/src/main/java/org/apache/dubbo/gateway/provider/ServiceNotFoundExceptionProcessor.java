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

package org.apache.dubbo.gateway.provider;

import org.apache.dubbo.common.serialize.Cleanable;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.remoting.ExceptionProcessor;
import org.apache.dubbo.remoting.RetryHandleException;
import org.apache.dubbo.remoting.ServiceNotFoundException;
import org.apache.dubbo.remoting.exchange.ErrorData;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;
import org.apache.dubbo.rpc.protocol.dubbo.RetryDecodeableRpcInvocation;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_VERSION;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

/**
 * <p>The default exception handler interrupts the return of the client error message by throwing a {@link RetryHandleException},
 * so that there is a chance to enter the decode process again.
 * <p>Then handle the {@link ServiceNotFoundException} through server-side generalization {@link OmnipotentService}
 * (different from the original generalization, which can accept any interface)
 *
 * @since 3.2.0
 */
public class ServiceNotFoundExceptionProcessor implements ExceptionProcessor {

    private static final String DEFAULT_OMNIPOTENT_SERVICE = OmnipotentService.class.getName();

    @Override
    public boolean shouldHandleError(Throwable data) {
        return data instanceof ServiceNotFoundException;
    }

    @Override
    public String wrapAndHandleException(ExchangeChannel channel, Request req) throws RetryHandleException {
        Object data = req.getData();
        if (!(data instanceof ErrorData)) {
            return null;
        }
        RetryDecodeableRpcInvocation invocation = (RetryDecodeableRpcInvocation) ((ErrorData) data).getData();

        invocation.setAttachment(OmnipotentCommonConstants.ORIGIN_PATH_KEY, invocation.getAttachment(PATH_KEY));
        // Replace serviceName in req with omn
        invocation.setAttachment(PATH_KEY, DEFAULT_OMNIPOTENT_SERVICE);
        invocation.setAttachment(INTERFACE_KEY, DEFAULT_OMNIPOTENT_SERVICE);

        // version
        invocation.setAttachment(OmnipotentCommonConstants.ORIGIN_VERSION_KEY, invocation.getAttachment(VERSION_KEY));
        invocation.setAttachment(VERSION_KEY, DEFAULT_VERSION);

        // method
        invocation.setAttachment(OmnipotentCommonConstants.ORIGIN_METHOD_KEY, invocation.getMethodName());
        invocation.setAttachment(METHOD_KEY, OmnipotentCommonConstants.$INVOKE_OMN);
        invocation.setMethodName(OmnipotentCommonConstants.$INVOKE_OMN);
        invocation.setParameterTypes(new Class<?>[]{Invocation.class});

        // Reset the decoded flag to continue the decoding process again
        invocation.resetHasDecoded();

        throw new RetryHandleException(channel, "Service not found:" + invocation.getAttachment(OmnipotentCommonConstants.ORIGIN_PATH_KEY) + ", " + invocation.getAttachment(OmnipotentCommonConstants.ORIGIN_METHOD_KEY));
    }

    @Override
    public void customPts(Object context, Class<?>[] pts) {
        if (!(context instanceof DecodeableRpcInvocation)) {
            return;
        }
        ((DecodeableRpcInvocation) context).setAttachment(OmnipotentCommonConstants.ORIGIN_GENERIC_PARAMETER_TYPES, pts);
    }

    @Override
    public void customAttachment(Object context, Map<String, Object> map) {

        if (!(context instanceof DecodeableRpcInvocation)) {
            return;
        }
        // Omn needs to use the default path, version and group,
        // and the original value starts with origin to save the variable
        map.remove(PATH_KEY);
        map.remove(VERSION_KEY);
        if (map.containsKey(GROUP_KEY)) {
            map.put(OmnipotentCommonConstants.ORIGIN_GROUP_KEY, map.get(GROUP_KEY));
            map.remove(GROUP_KEY);
        }
    }

    @Override
    public void cleanUp(Object context) {

        if (!(context instanceof RetryDecodeableRpcInvocation)) {
            return;
        }
        ObjectInput objectInput = ((RetryDecodeableRpcInvocation) context).getObjectInput();
        if ((objectInput instanceof Cleanable)) {
            ((Cleanable) objectInput).cleanup();
        }
        ((RetryDecodeableRpcInvocation) context).setObjectInput(null);
    }


}
