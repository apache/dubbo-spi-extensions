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

package org.apache.dubbo.rpc.cluster.router;

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.common.parser.QueryParser;
import com.tencent.polaris.common.registry.PolarisOperator;
import com.tencent.polaris.common.registry.PolarisOperators;
import com.tencent.polaris.common.router.RuleHandler;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PolarisRouter extends AbstractRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisRouter.class);

    private final RuleHandler routeRuleHandler;

    private final PolarisOperator polarisOperator;

    private final QueryParser parser;

    public PolarisRouter(URL url) {
        super(url);
        LOGGER.info(String.format("[POLARIS] init service router, url is %s, parameters are %s", url,
            url.getParameters()));
        this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
        this.routeRuleHandler = new RuleHandler();
        this.polarisOperator = PolarisOperators.INSTANCE.getPolarisOperator(url.getHost(), url.getPort());
        this.parser = QueryParser.load();
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (null == invokers || invokers.size() == 0) {
            return invokers;
        }
        if (null == polarisOperator) {
            return invokers;
        }
        List<Instance> instances;
        if (invokers.get(0) instanceof Instance) {
            instances = (List<Instance>) ((List<?>) invokers);
        } else {
            instances = new ArrayList<>();
            for (Invoker<T> invoker : invokers) {
                instances.add(new InstanceInvoker<>(invoker, polarisOperator.getPolarisConfig().getNamespace()));
            }
        }

        String service = url.getServiceInterface();
        ServiceRule serviceRule = polarisOperator.getServiceRule(service, EventType.ROUTING);
        Object ruleObject = serviceRule.getRule();
        Set<RouteArgument> arguments = new HashSet<>();
        if (null != ruleObject) {
            RoutingProto.Routing routing = (RoutingProto.Routing) ruleObject;
            Set<String> routeLabels = routeRuleHandler.getRouteLabels(routing);
            for (String routeLabel : routeLabels) {
                if (StringUtils.equals(RouteArgument.LABEL_KEY_PATH, routeLabel)) {
                    arguments.add(RouteArgument.buildPath(invocation.getMethodName()));
                } else if (routeLabel.startsWith(RouteArgument.LABEL_KEY_HEADER)) {
                    String headerName = routeLabel.substring(RouteArgument.LABEL_KEY_HEADER.length());
                    String value = RpcContext.getContext().getAttachment(headerName);
                    if (!StringUtils.isBlank(value)) {
                        arguments.add(RouteArgument.buildHeader(headerName, value));
                    }
                } else if (routeLabel.startsWith(RouteArgument.LABEL_KEY_QUERY)) {
                    String queryName = routeLabel.substring(RouteArgument.LABEL_KEY_QUERY.length());
                    if (!StringUtils.isBlank(queryName)) {
                        Optional<String> queryValue = parser.parse(queryName, invocation.getArguments());
                        queryValue.ifPresent(value -> arguments.add(RouteArgument.buildQuery(queryName, value)));
                    }
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("[POLARIS] list service %s, method %s, labels %s, url %s", service,
                invocation.getMethodName(), arguments, url));
        }
        List<Instance> resultInstances = polarisOperator.route(service, invocation.getMethodName(), arguments, instances);
        return (List<Invoker<T>>) ((List<?>) resultInstances);
    }
}
