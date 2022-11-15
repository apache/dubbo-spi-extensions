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

package org.apache.dubbo.registry.nameservice;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExtImpl;
import org.apache.rocketmq.tools.admin.MQAdminExt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class NameServiceRegistry extends FailbackRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NameServiceRegistry.class);

    private ScheduledExecutorService scheduledExecutorService;

    private Map<URL, RegistryInfoWrapper> consumerRegistryInfoWrapperMap = new ConcurrentHashMap<>();

    private MQAdminExt mqAdminExt;

    private boolean isNotRoute = true;

    private ClusterInfo clusterInfo;

    private TopicList topicList;

    private long timeoutMillis;

    private String instanceName;

    public NameServiceRegistry(URL url) {
        super(url);
        this.isNotRoute = url.getParameter("route", true);
        if (this.isNotRoute) {
            return;
        }
        this.timeoutMillis = url.getParameter("timeoutMillis", 3000);
        this.instanceName = url.getParameter("instanceName", "nameservic-registry");
        DefaultMQAdminExt clientConfig = new DefaultMQAdminExt();
        clientConfig.setNamesrvAddr(url.getAddress());
        clientConfig.setInstanceName(instanceName);
        mqAdminExt = new DefaultMQAdminExtImpl(clientConfig, this.timeoutMillis);
        try {
            mqAdminExt.start();
            this.initBeasInfo();
        } catch (Exception e) {
            String exeptionInfo = String.format("initBeasInfo pullRoute exception , cause %s ", e.getMessage());
            logger.error(exeptionInfo, e);
            throw new RuntimeException(exeptionInfo, e);
        }
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "dubbo-registry-nameservice");
            }
        });
        scheduledExecutorService.scheduleAtFixedRate(this::run, 1000 * 10, 3000 * 10, TimeUnit.MILLISECONDS);
    }

    private void run() {
        try {
            this.initBeasInfo();
            if (consumerRegistryInfoWrapperMap.isEmpty()) {
                return;
            }
            for (Entry<URL, RegistryInfoWrapper> e : consumerRegistryInfoWrapperMap.entrySet()) {
                List<URL> urls = new ArrayList<URL>();
                this.pullRoute(e.getValue().serviceName, e.getKey(), urls);
                e.getValue().listener.notify(urls);
            }
        } catch (Exception e) {
            String exeptionInfo = String.format("ScheduledTask pullRoute exception , cause %s ", e.getMessage());
            logger.error(exeptionInfo, e);
        }
    }

    private void initBeasInfo() throws Exception {
        this.clusterInfo = this.mqAdminExt.examineBrokerClusterInfo();
        this.topicList = this.mqAdminExt.fetchAllTopicList();
    }

    private URL createProviderURL(ServiceName serviceName, URL url, int queue) {
        URLBuilder builder = URLBuilder.from(url).setProtocol("rocketmq").setAddress(this.getUrl().getAddress());
        builder.addParameter(CommonConstants.INTERFACE_KEY, serviceName.getServiceInterface());
        builder.addParameter(CommonConstants.PATH_KEY, serviceName.getServiceInterface());
        builder.addParameter("bean.name", "ServiceBean:" + serviceName.getServiceInterface());
        builder.addParameter(CommonConstants.SIDE_KEY, CommonConstants.PROVIDER);
        builder.addParameter(RegistryConstants.CATEGORY_KEY, "providers");
        builder.addParameter(CommonConstants.PROTOCOL_KEY, "rocketmq");
        builder.addParameter("queueId", queue + "");
        builder.addParameter("topic", serviceName.getValue());
        return builder.build();
    }

    private ServiceName createServiceName(URL url) {
        return new ServiceName(url);
    }

    private void createTopic(ServiceName serviceName) {
        if (this.isNotRoute) {
            return;
        }
        if (this.topicList.getTopicList().contains(serviceName.getValue())) {
            return;
        }
        try {
            TopicConfig topicConfig = new TopicConfig(serviceName.getValue());
            topicConfig.setReadQueueNums(2);
            topicConfig.setWriteQueueNums(2);
            for (Entry<String, BrokerData> entry : clusterInfo.getBrokerAddrTable().entrySet()) {
                for (String brokerAddr : entry.getValue().getBrokerAddrs().values()) {
                    this.mqAdminExt.createAndUpdateTopicConfig(brokerAddr, topicConfig);
                }
            }
        } catch (Exception e) {
            String exceptionInfo = String.format("create topic fial, topic name is %s , cause %s", serviceName.getValue(), e.getMessage());
            logger.error(exceptionInfo, e);
            throw new RuntimeException(exceptionInfo, e);
        }

    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void doRegister(URL url) {
        ServiceName serviceName = this.createServiceName(url);
        this.createTopic(serviceName);
    }

    @Override
    public void doUnregister(URL url) {
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        if (Objects.equals(url.getCategory(),
            org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY)) {
            return;
        }
        ServiceName serviceName = this.createServiceName(url);
        try {
            GroupList groupList = this.mqAdminExt.queryTopicConsumeByWho(serviceName.getValue());
            if (Objects.isNull(groupList) || groupList.getGroupList().isEmpty()) {
                return;
            }
        } catch (InterruptedException | MQBrokerException | RemotingException | MQClientException e) {
            String exceptionInfo =
                String.format("query topic consume fial, topic name is %s , url is %s , cause %s", serviceName.getValue(), url, e.getMessage());
            logger.error(exceptionInfo, e);
            throw new RuntimeException(exceptionInfo, e);
        }
        List<URL> urls = new ArrayList<URL>();
        if (this.isNotRoute) {
            URL providerURL = this.createProviderURL(serviceName, url, -1);
            urls.add(providerURL);
        } else {
            RegistryInfoWrapper registryInfoWrapper = new RegistryInfoWrapper();
            registryInfoWrapper.listener = listener;
            registryInfoWrapper.serviceName = serviceName;
            consumerRegistryInfoWrapperMap.put(url, registryInfoWrapper);
            this.pullRoute(serviceName, url, urls);
        }
        listener.notify(urls);
    }

    void pullRoute(ServiceName serviceName, URL url, List<URL> urls) {
        try {
            String topic = serviceName.getValue();
            TopicRouteData topicRouteData = this.mqAdminExt.examineTopicRouteInfo(topic);
            for (QueueData queueData : topicRouteData.getQueueDatas()) {
                if (!PermName.isReadable(queueData.getPerm())) {
                    continue;
                }
                for (int i = 0; i < queueData.getReadQueueNums(); i++) {
                    URL newUrl = this.createProviderURL(serviceName, url, i);
                    urls.add(newUrl.addParameter("brokerName", queueData.getBrokerName()));
                }
            }
        } catch (Exception e) {
            String exceptionInfo =
                String.format("query topic route fial, topic name is %s , url is %s , cause %s", serviceName.getValue(), url, e.getMessage());
            logger.error(exceptionInfo, e);
            throw new RuntimeException(exceptionInfo, e);
        }
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        this.consumerRegistryInfoWrapperMap.remove(url);
    }

    private class RegistryInfoWrapper {

        private NotifyListener listener;
        private ServiceName serviceName;

        public RegistryInfoWrapper() {
        }
    }
}
