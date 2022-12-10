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
import com.tencent.polaris.client.pb.RoutingProto.Routing;
import com.tencent.polaris.common.registry.PolarisOperator;
import com.tencent.polaris.common.registry.PolarisOperators;
import com.tencent.polaris.common.router.ObjectParser;
import com.tencent.polaris.common.router.RuleHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Constants;

public class PolarisRouter extends AbstractRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisRouter.class);

    private final RuleHandler routeRuleHandler;

    private final PolarisOperator polarisOperator;

    private final AtomicReference<Map<URL, InstanceInvoker<?>>> invokersCache = new AtomicReference<>();

    public PolarisRouter(URL url) {
        super(url);
        LOGGER.info(String.format("[POLARIS] init service router, url is %s, parameters are %s", url,
                url.getParameters()));
        this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
        routeRuleHandler = new RuleHandler();
        polarisOperator = PolarisOperators.INSTANCE.getPolarisOperator(url.getHost(), url.getPort());
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (null == invokers || invokers.size() == 0) {
            return invokers;
        }
        if (null == polarisOperator) {
            return invokers;
        }
        List<Instance> instances = new ArrayList<>(invokers.size());
        Map<URL, InstanceInvoker<?>> instanceInvokerMap = invokersCache.get();
        if (null == instanceInvokerMap) {
            instanceInvokerMap = Collections.emptyMap();
        }
        for (Invoker<T> invoker : invokers) {
                InstanceInvoker<?> instanceInvoker = instanceInvokerMap.get(invoker.getUrl());
                if (null != instanceInvoker) {
                    instances.add(instanceInvoker);
                } else {
                    instances.add(new InstanceInvoker<>(invoker, polarisOperator.getPolarisConfig().getNamespace()));
                }
        }
        String service = url.getServiceInterface();
        ServiceRule serviceRule = polarisOperator.getServiceRule(service, EventType.ROUTING);
        Object ruleObject = serviceRule.getRule();
        Set<RouteArgument> arguments = new HashSet<>();
        if (null != ruleObject) {
            Routing routing = (Routing) ruleObject;
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
                        Object value = ObjectParser.parseArgumentsByExpression(queryName, invocation.getArguments());
                        if (null != value) {
                            arguments.add(RouteArgument.buildQuery(queryName, String.valueOf(value)));
                        }
                    }
                }
            }
        }
        LOGGER.debug(String.format("[POLARIS] list service %s, method %s, labels %s, url %s", service,
                invocation.getMethodName(), arguments, url));
        List<Instance> resultInstances = polarisOperator
                .route(service, invocation.getMethodName(), arguments, instances);
        return (List<Invoker<T>>) ((List<?>) resultInstances);
    }

    public <T> void notify(List<Invoker<T>> invokers) {
        if (null == polarisOperator) {
            return;
        }
        Map<URL, InstanceInvoker<?>> instanceInvokers = new HashMap<>(invokers.size());
        for (Invoker<T> invoker : invokers) {
            instanceInvokers.put(invoker.getUrl(), new InstanceInvoker<>(invoker, polarisOperator.getPolarisConfig().getNamespace()));
        }
        invokersCache.set(instanceInvokers);
    }
}
