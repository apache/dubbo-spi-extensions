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

package org.apache.dubbo.filter.dubbo2;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.common.exception.PolarisBlockException;
import com.tencent.polaris.common.parser.QueryParser;
import com.tencent.polaris.common.registry.PolarisOperator;
import com.tencent.polaris.common.registry.PolarisOperatorDelegate;
import com.tencent.polaris.common.router.RuleHandler;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

@Activate(group = CommonConstants.PROVIDER)
public class RateLimitFilter extends PolarisOperatorDelegate implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RuleHandler ruleHandler;

    private final QueryParser parser;

    private final RateLimitCallback callback;

    public RateLimitFilter() {
        LOGGER.info("[POLARIS] init polaris ratelimit");
        System.setProperty("dubbo.polaris.query_parser", System.getProperty("dubbo.polaris.query_parser", "JsonPath"));
        this.ruleHandler = new RuleHandler();
        this.parser = QueryParser.load();

        ServiceLoader<RateLimitCallback> loader = ServiceLoader.load(RateLimitCallback.class);
        RateLimitCallback instance = loader.iterator().next();
        if (Objects.nonNull(instance)) {
            this.callback = instance;
        } else {
            this.callback = new DefaultRateLimitCallback();
        }
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String service = invoker.getInterface().getName();
        PolarisOperator polarisOperator = getPolarisOperator();
        if (null == polarisOperator) {
            return invoker.invoke(invocation);
        }
        ServiceRule serviceRule = polarisOperator.getServiceRule(service, EventType.RATE_LIMITING);
        Object ruleObject = serviceRule.getRule();
        if (null == ruleObject) {
            return invoker.invoke(invocation);
        }
        RateLimitProto.RateLimit rateLimit = (RateLimitProto.RateLimit) ruleObject;
        Set<RateLimitProto.MatchArgument> ratelimitLabels = ruleHandler.getRatelimitLabels(rateLimit);
        String method = invocation.getMethodName();
        Set<Argument> arguments = new HashSet<>();
        for (RateLimitProto.MatchArgument matchArgument : ratelimitLabels) {
            switch (matchArgument.getType()) {
                case HEADER:
                    String attachmentValue = RpcContext.getContext().getAttachment(matchArgument.getKey());
                    if (!StringUtils.isBlank(attachmentValue)) {
                        arguments.add(Argument.buildHeader(matchArgument.getKey(), attachmentValue));
                    }
                    break;
                case QUERY:
                    Optional<String> queryValue = parser.parse(matchArgument.getKey(), invocation.getArguments());
                    queryValue.ifPresent(value -> arguments.add(Argument.buildQuery(matchArgument.getKey(), value)));
                    break;
                default:
                    break;
            }
        }
        QuotaResponse quotaResponse = null;
        try {
            quotaResponse = polarisOperator.getQuota(service, method, arguments);
        } catch (PolarisException e) {
            LOGGER.error("[POLARIS] get quota fail, {}", e);
        }
        if (null != quotaResponse && quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
            // throw block exception when ratelimit occurs
            return callback.handle(invoker, invocation, new PolarisBlockException(
                String.format("url=%s, info=%s", invoker.getUrl(), quotaResponse.getInfo())));
        }
        return invoker.invoke(invocation);
    }

    private static final class DefaultRateLimitCallback implements RateLimitCallback {

        @Override
        public Result handle(Invoker<?> invoker, Invocation invocation, PolarisBlockException ex) {
            // throw block exception when ratelimit occurs
            throw new RpcException(RpcException.LIMIT_EXCEEDED_EXCEPTION, ex);
        }
    }
}
