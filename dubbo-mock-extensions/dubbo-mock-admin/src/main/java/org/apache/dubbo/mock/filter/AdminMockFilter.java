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

import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.mock.api.GlobalMockRule;
import org.apache.dubbo.mock.api.MockContext;
import org.apache.dubbo.mock.api.MockResult;
import org.apache.dubbo.mock.api.MockService;
import org.apache.dubbo.mock.handler.CommonTypeHandler;
import org.apache.dubbo.mock.handler.ResultContext;
import org.apache.dubbo.mock.handler.TypeHandler;
import org.apache.dubbo.mock.utils.ProtobufUtil;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.google.gson.Gson;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.mock.api.MockConstants.ADMIN_MOCK_RULE_GROUP;
import static org.apache.dubbo.mock.api.MockConstants.ADMIN_MOCK_RULE_KEY;

/**
 * AdminMockFilter will intercept the request from user's consumer. if the mock tag is opened,
 * then the user's consumer will request the mock data configured in Dubbo Admin. The mock data's
 * request is agent by the implement of {@link MockService}.
 *
 * @author chenglu
 * @date 2021-08-23 23:14
 */
@Activate(group = CommonConstants.CONSUMER)
public class AdminMockFilter implements ClusterFilter {

    private static final Logger logger = LoggerFactory.getLogger(AdminMockFilter.class);

    private final TypeHandler typeHandler;

    private GlobalMockRule globalMockRule = new GlobalMockRule();
    
    static {
        ReferenceConfig<MockService> mockServiceConfig = new ReferenceConfig<>();
        mockServiceConfig.setCheck(false);
        mockServiceConfig.setInterface(MockService.class);
        DubboBootstrap.getInstance().reference(mockServiceConfig);
    }

    public AdminMockFilter() {
        typeHandler = new CommonTypeHandler();
        Optional<DynamicConfiguration> dynamicConfigurationOptional = ApplicationModel.getEnvironment().getDynamicConfiguration();
        if (!dynamicConfigurationOptional.isPresent()) {
            logger.warn("[Dubbo Admin Mock] could not find configuration center, all consumer request will not be mocked!");
            return;
        }
        DynamicConfiguration dynamicConfiguration = dynamicConfigurationOptional.get();
        String config = dynamicConfiguration.getConfig(ADMIN_MOCK_RULE_KEY, ADMIN_MOCK_RULE_GROUP);
        if (StringUtils.isNotEmpty(config)) {
            GlobalMockRule newGlobalMockRule = new Gson().fromJson(config, GlobalMockRule.class);
            globalMockRule.setEnableMock(newGlobalMockRule.getEnableMock());
            globalMockRule.getEnabledMockRules().addAll(newGlobalMockRule.getEnabledMockRules());
        }

        dynamicConfiguration.addListener(ADMIN_MOCK_RULE_KEY, ADMIN_MOCK_RULE_GROUP, event -> {
            globalMockRule.getEnabledMockRules().clear();
            String content = event.getContent();
            if (StringUtils.isBlank(content)) {
                globalMockRule.setEnableMock(false);
                return;
            }
            GlobalMockRule newRule = new Gson().fromJson(config, GlobalMockRule.class);
            globalMockRule.setEnableMock(newRule.getEnableMock());
            globalMockRule.getEnabledMockRules().addAll(newRule.getEnabledMockRules());
        });
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // check if open the admin mock config, global config.
        if (!globalMockRule.getEnableMock()) {
            return invoker.invoke(invocation);
        }

        String interfaceName = invocation.getTargetServiceUniqueName();
        String methodName = invocation.getMethodName();
        Object[] params = solveParams(invocation.getArguments());

        // check if the service is in mock list
        String mockRuleName = interfaceName + "#" + methodName;
        Set<String> mockRules = globalMockRule.getEnabledMockRules();
        if (CollectionUtils.isEmpty(mockRules) || !mockRules.contains(mockRuleName)) {
            return invoker.invoke(invocation);
        }
        
        // check if the MockService's invoker, then request.
        if (Objects.equals(interfaceName, MockService.class.getName())) {
            return invoker.invoke(invocation);
        }
        MockService mockService = DubboBootstrap.getInstance().getCache().get(MockService.class);
        if (Objects.isNull(mockService)) {
            throw new RpcException("Cloud not find MockService, please check if it has started.");
        }
        
        // parse the result from MockService, build the real method's return value.
        MockContext mockContext = MockContext.newMockContext()
            .serviceName(interfaceName).methodName(methodName).arguments(params).build();
        MockResult mockResult = mockService.mock(mockContext);
        if (!mockResult.getEnable()) {
            return invoker.invoke(invocation);
        }
        Class<?> returnType = ((RpcInvocation) invocation).getReturnType();

        // parse the return data.
        ResultContext resultContext = ResultContext.newResultContext()
            .data(mockResult.getContent()).targetType(returnType)
            .serviceName(interfaceName).methodName(methodName).build();
        Object data = typeHandler.handleResult(resultContext);
        AppResponse appResponse = new AppResponse(data);
        CompletableFuture<AppResponse> appResponseFuture = new CompletableFuture<>();
        appResponseFuture.complete(appResponse);
        return new AsyncRpcResult(appResponseFuture, invocation);
    }

    /**
     * Solve the request params, due to the protobuf object has no public constructor.
     * and we use dubbo protocol, so we must solve it.
     *
     * @param arguments arguments
     * @return new resolved params.
     */
    private Object[] solveParams(Object[] arguments) {
        if (Objects.isNull(arguments)) {
            return null;
        }
        Object[] params = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            if (Objects.isNull(argument)) {
                params[i] = null;
                continue;
            }
            if (ProtobufUtil.isProtobufClass(argument.getClass())) {
                params[i] = ProtobufUtil.protobufToJson(argument);
            } else {
                params[i] = argument;
            }
        }
        return params;
    }
}
