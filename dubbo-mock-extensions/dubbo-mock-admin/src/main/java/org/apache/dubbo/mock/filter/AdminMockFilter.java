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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.mock.api.MockResult;
import org.apache.dubbo.mock.api.MockService;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * AdminMockFilter will intercept the request from user's consumer. if the mock tag is opened,
 * then the user's consumer will request the mock data configured in Dubbo Admin. The mock data's
 * request is agent by the implement of {@link MockService}.
 *
 * @author chenglu
 * @date 2021-08-23 16:14
 */
@Activate(group = CommonConstants.CONSUMER)
public class AdminMockFilter implements ClusterFilter {
    
    /**
     * the global enable mock config.
     */
    private static final boolean ENABLE_DUBBO_ADMIN_MOCK;
    
    static {
        ReferenceConfig<MockService> mockServiceConfig = new ReferenceConfig<>();
        mockServiceConfig.setCheck(false);
        mockServiceConfig.setInterface(MockService.class);
        DubboBootstrap.getInstance().reference(mockServiceConfig);
    
        ENABLE_DUBBO_ADMIN_MOCK = Boolean.parseBoolean(System.getProperty("dubbo.admim.mock.enable", "false"));
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // check if open the admin mock config, global config.
        if (!ENABLE_DUBBO_ADMIN_MOCK) {
            return invoker.invoke(invocation);
        }
        
        // check if the MockService's invoker, then request.
        if (Objects.equals(invoker.getInterface().getName(), MockService.class.getName())) {
            return invoker.invoke(invocation);
        }
        MockService mockService = DubboBootstrap.getInstance().getCache().get(MockService.class);
        if (Objects.isNull(mockService)) {
            throw new RpcException("Cloud not find MockService, please check if it has started.");
        }
        
        // parse the result from MockService, build the real method's return value.
        String interfaceName = invoker.getInterface().getName();
        String methodName = invocation.getMethodName();
        Object[] params = invocation.getArguments();
        MockResult mockResult = mockService.mock(interfaceName, methodName, params);
        if (!mockResult.isEnable()) {
            return invoker.invoke(invocation);
        }
        Class<?> returnType = ((RpcInvocation) invocation).getReturnType();
        
        // parse the return data.
        Object data = parseResult(mockResult.getContent(), returnType);
        AppResponse appResponse = new AppResponse(data);
        CompletableFuture<AppResponse> appResponseFuture = new CompletableFuture<>();
        appResponseFuture.complete(appResponse);
        return new AsyncRpcResult(appResponseFuture, invocation);
    }
    
    private Object parseResult(String content, Class<?> returnType) {
        // parse it to json.
        try {
            return PojoUtils.realize(content, returnType);
        } catch (Exception e) {
            // todo if failed, parse it as protobuf data.
            return null;
        }
    }
}
