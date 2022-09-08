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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.dubbo.common.URL;
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

public class NameServiceRegistry extends FailbackRegistry {

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
        this.isNotRoute = !url.getParameter("route", false);
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
            throw new RuntimeException(e);
        }
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "dubbo-registry-nameservice");
            }
        });
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    NameServiceRegistry.this.initBeasInfo();

                    if (consumerRegistryInfoWrapperMap.isEmpty()) {
                        return;
                    }
                    for (Entry<URL, RegistryInfoWrapper> e : consumerRegistryInfoWrapperMap.entrySet()) {
                        List<URL> urls = new ArrayList<URL>();
                        NameServiceRegistry.this.pullRoute(e.getValue().serviceName, e.getKey(), urls);
                        e.getValue().listener.notify(urls);
                    }
                } catch (Exception e) {
                    logger.error("ScheduledTask pullRoute exception", e);
                }
            }
        }, 1000 * 10, 3000 * 10, TimeUnit.MILLISECONDS);
    }

    private void initBeasInfo() throws Exception {
        this.clusterInfo = this.mqAdminExt.examineBrokerClusterInfo();
        this.topicList = this.mqAdminExt.fetchAllTopicList();
    }

    private URL createProviderURL(ServiceName serviceName, URL url, int queue) {
        Map<String, String> parameters = url.getParameters();
        parameters.put(CommonConstants.INTERFACE_KEY, serviceName.getServiceInterface());
        parameters.put(CommonConstants.PATH_KEY, serviceName.getServiceInterface());
        parameters.put("bean.name", "ServiceBean:" + serviceName.getServiceInterface());
        parameters.put(CommonConstants.SIDE_KEY, CommonConstants.PROVIDER);
        parameters.put(RegistryConstants.CATEGORY_KEY, "providers");
        parameters.put(CommonConstants.PROTOCOL_KEY, "rocketmq");
        parameters.put("queueId", queue + "");
        parameters.put("topic", serviceName.getValue());
        return new URL("rocketmq", this.getUrl().getIp(), this.getUrl().getPort(), url.getPath(), parameters);
    }

    private ServiceName createServiceName(URL url) {
        return new ServiceName(url);
    }

    private void createTopic(ServiceName serviceName) {
        if (!this.topicList.getTopicList().contains(serviceName.getValue())) {
            try {
                TopicConfig topicConfig = new TopicConfig(serviceName.getValue());
                topicConfig.setReadQueueNums(2);
                topicConfig.setWriteQueueNums(2);
                for (Entry<String, BrokerData> entry : clusterInfo.getBrokerAddrTable().entrySet()) {
                    this.mqAdminExt.createAndUpdateTopicConfig(entry.getValue().selectBrokerAddr(), topicConfig);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return false;
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
            throw new RuntimeException(e);
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
            Map<String, String> brokerAddrBybrokerName = new HashMap<>();
            for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
                brokerAddrBybrokerName.put(brokerData.getBrokerName(), brokerData.selectBrokerAddr());
            }
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
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        this.consumerRegistryInfoWrapperMap.remove(url);
    }

    private class RegistryInfoWrapper {

        private NotifyListener listener;

        private ServiceName serviceName;
    }
}
