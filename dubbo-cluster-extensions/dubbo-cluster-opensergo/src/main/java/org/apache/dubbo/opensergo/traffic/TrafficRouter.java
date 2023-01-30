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

package org.apache.dubbo.opensergo.traffic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.alibaba.csp.sentinel.datasource.OpenSergoDataSourceGroup;
import com.alibaba.csp.sentinel.traffic.ClusterManager;
import com.alibaba.csp.sentinel.traffic.DefaultInstanceManager;
import com.alibaba.csp.sentinel.traffic.Instance;
import com.alibaba.csp.sentinel.traffic.InstanceManager;
import com.alibaba.csp.sentinel.traffic.RouterFilter;
import com.alibaba.csp.sentinel.traffic.TrafficContext;
import com.alibaba.csp.sentinel.traffic.TrafficRouterFilter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;
import org.apache.dubbo.rpc.cluster.router.RouterResult;

import org.springframework.util.StringUtils;

public class TrafficRouter extends AbstractRouter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrafficRouter.class);

    private List<RouterFilter> filters;
    private InstanceManager instanceManager;
    private ClusterManager clusterManager;
    private OpenSergoDataSourceGroup openSergoDataSourceGroup;
    private String openSergoHost;
    private Integer openSergoPort;
    private String openSergoNamespace;

    private final static String OPENSERGO_HOST_KEY = "opensergo.host";
    private final static String OPENSERGO_PORT_KEY = "opensergo.port";
    private final static String OPENSERGO_NAMESPACE_KEY = "opensergo.namespace";


    public TrafficRouter(URL url) {
        super(url);
        this.openSergoHost = url.getParameter(OPENSERGO_HOST_KEY);
        this.openSergoPort = url.getParameter(OPENSERGO_PORT_KEY, 10246);
        this.openSergoNamespace =url.getParameter(OPENSERGO_NAMESPACE_KEY, "default");
        if (StringUtils.isEmpty(openSergoHost)) {
            LOGGER.error("init OpenSergo service router error due to miss OpenSergo host.");
        }
        LOGGER.info(String.format("init OpenSergo service router, url is %s, parameters are %s", url,
            url.getParameters()));
        filters = new ArrayList<>();
        instanceManager = new DefaultInstanceManager();
        clusterManager = new ClusterManager(filters,null, instanceManager);
        filters.add(new TrafficRouterFilter(clusterManager));
        this.openSergoDataSourceGroup = new OpenSergoDataSourceGroup(openSergoHost, openSergoPort, openSergoNamespace, url.getApplication());
        try {
            openSergoDataSourceGroup.start();
        }
        catch (Exception e) {
            LOGGER.error("Start OpenSergo client enhance error", e);
        }

        LOGGER.info(String.format("[OpenSergo] init service router success, endpoint is %s, parameters are %s", "127.0.0.1:" + 10246,
            url.getParameters()));
    }

    @Override
    public <T> RouterResult<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage) throws RpcException {
        TrafficContext trafficContext = getTrafficContext(invocation);
        List<Instance> instances = clusterManager.route(trafficContext);
        return new RouterResult<>(instancesToInvokers(instances));
    }

    private <T> List<Invoker<T>> instancesToInvokers(List<Instance> instances) {
        List<Invoker<T>> invokers = new ArrayList<>(instances.size());
        for (Instance instance: instances) {
            invokers.add((Invoker<T>) instance.getTargetInstance());
        }
        return invokers;
    }

    private TrafficContext getTrafficContext(Invocation invocation) {
        TrafficContext context = new TrafficContext();
        context.setServiceName(invocation.getServiceName());
        context.setMethodName(invocation.getMethodName());
        if (invocation.getArguments() != null) {
            context.setArgs(Arrays.asList(invocation.getArguments()));
        }
        context.setBaggage(invocation.getAttachments());
        context.setHeaders(invocation.getAttachments());
        return context;
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        super.notify(invokers);
        clusterManager.notify(invokersToInstances(invokers));
    }

    private <T> List<Instance> invokersToInstances(List<Invoker<T>> invokers) {
        List<Instance> instances = new ArrayList<>(invokers.size());
        for (Invoker<T> invoker: invokers) {
            Instance instance = new Instance();
            String host = invoker.getUrl().getHost();
            Integer port = invoker.getUrl().getPort();

            String appName = invoker.getUrl().getRemoteApplication();
            instance.setAppName(appName);
            instance.setHost(host);
            instance.setPort(port);
            instance.setMetadata(invoker.getUrl().getParameters());
            instance.setTargetInstance(invoker);
            instances.add(instance);
        }
        return instances;
    }

    @Override
    public void stop() {
        try {
            openSergoDataSourceGroup.close();
        }
        catch (Exception e) {
            LOGGER.error("Stop OpenSergo client enhance error", e);
        }
    }
}
