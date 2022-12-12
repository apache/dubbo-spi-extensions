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
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExtImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;


@RunWith(PowerMockRunner.class)
@PrepareForTest({NameServiceRegistry.class})
public class NameServiceRegistryTest {

    private DefaultMQAdminExtImpl defaultMQAdminExtImpl = PowerMockito.mock(DefaultMQAdminExtImpl.class);
    ;

    private ScheduledExecutorService scheduledExecutorService = PowerMockito.mock(ScheduledExecutorService.class);

    private URL routuUrl;

    private URL routuFlase;

    private Class<?> registryInfoWrapper;

    private Field listenerField;

    private Field serviceNameField;

    @Before
    public void init() throws Exception {
        String urlString =
            "nameservice://localhost:9876/org.apache.dubbo.registry.RegistryService?application=rocketmq-provider&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=8990&release=3.0.7&route=false";
        routuFlase = URLBuilder.valueOf(urlString);
        routuUrl = URLBuilder.from(routuFlase).addParameter("route", "true").build();
        PowerMockito.whenNew(DefaultMQAdminExtImpl.class).withAnyArguments().thenReturn(defaultMQAdminExtImpl);
        PowerMockito.mockStatic(Executors.class);
        PowerMockito.when(Executors.newSingleThreadScheduledExecutor(Mockito.any())).thenReturn(scheduledExecutorService);

        registryInfoWrapper = Class.forName("org.apache.dubbo.registry.nameservice.NameServiceRegistry$RegistryInfoWrapper");
        listenerField = registryInfoWrapper.getDeclaredField("listener");
        listenerField.setAccessible(true);
        serviceNameField = registryInfoWrapper.getDeclaredField("serviceName");
        serviceNameField.setAccessible(true);
    }

    @Test
    public void newTest() throws Exception {

        NameServiceRegistry nameServiceRegistry = new NameServiceRegistry(routuUrl);
        boolean isNotRoute = ReflectUtils.getFieldValue(nameServiceRegistry, "isNotRoute");
        Assert.assertEquals(isNotRoute, true);

        long timeoutMillis = ReflectUtils.getFieldValue(nameServiceRegistry, "timeoutMillis");
        Assert.assertEquals(timeoutMillis, 0);

        String instanceName = ReflectUtils.getFieldValue(nameServiceRegistry, "instanceName");
        Assert.assertEquals(instanceName, null);


        nameServiceRegistry = new NameServiceRegistry(routuFlase);

        isNotRoute = ReflectUtils.getFieldValue(nameServiceRegistry, "isNotRoute");
        Assert.assertEquals(isNotRoute, false);

        timeoutMillis = ReflectUtils.getFieldValue(nameServiceRegistry, "timeoutMillis");
        Assert.assertEquals(timeoutMillis, 3000);

        instanceName = ReflectUtils.getFieldValue(nameServiceRegistry, "instanceName");
        Assert.assertEquals(instanceName, "nameservic-registry");

        Mockito.verify(defaultMQAdminExtImpl, Mockito.atLeastOnce()).start();
        Mockito.verify(defaultMQAdminExtImpl, Mockito.atLeastOnce()).examineBrokerClusterInfo();
        Mockito.verify(defaultMQAdminExtImpl, Mockito.atLeastOnce()).fetchAllTopicList();

        Mockito.verify(scheduledExecutorService, Mockito.atLeastOnce()).scheduleAtFixedRate(Mockito.any(Runnable.class)
            , Mockito.any(Long.class)
            , Mockito.any(Long.class)
            , Mockito.any(TimeUnit.class));
    }

    @Test(expected = RuntimeException.class)
    public void newStartExceptionTest() throws Exception {
        PowerMockito.doThrow(new RuntimeException()).when(defaultMQAdminExtImpl).start();
        NameServiceRegistry nameServiceRegistry = new NameServiceRegistry(routuFlase);
    }

    @Test
    public void runnableTest() throws Exception {
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                Assert.assertNotNull(arguments[0]);
                Assert.assertEquals(arguments[1], 10000L);
                Assert.assertEquals(arguments[2], 30000L);
                Assert.assertEquals(arguments[3], TimeUnit.MILLISECONDS);
                return null;
            }
        }).when(scheduledExecutorService)
            .scheduleAtFixedRate(Mockito.any(Runnable.class)
                , Mockito.any(Long.class)
                , Mockito.any(Long.class)
                , Mockito.any(TimeUnit.class));
        NameServiceRegistry nameServiceRegistry = PowerMockito.spy(new NameServiceRegistry(routuFlase));
        PowerMockito.doNothing().when(nameServiceRegistry, "initBeasInfo");
        PowerMockito.when(nameServiceRegistry, "run").thenCallRealMethod();
        PowerMockito.verifyPrivate(nameServiceRegistry, Mockito.never()).invoke("pullRoute", null, null, null);

        nameServiceRegistry = new NameServiceRegistry(routuFlase);

        Map<URL, Object> consumerRegistryInfoWrapperMap = ReflectUtils.getFieldValue(nameServiceRegistry, "consumerRegistryInfoWrapperMap");

        Constructor<?> constructor = registryInfoWrapper.getConstructor(new Class<?>[] {NameServiceRegistry.class});
        constructor.setAccessible(true);
        Object wrapper = constructor.newInstance(nameServiceRegistry);
        NotifyListener notifyListener = PowerMockito.mock(NotifyListener.class);
        listenerField.set(wrapper, notifyListener);
        ServiceName serviceName = new ServiceName(routuFlase);
        serviceNameField.set(wrapper, serviceName);
        consumerRegistryInfoWrapperMap.put(routuUrl, wrapper);

        nameServiceRegistry = PowerMockito.spy(nameServiceRegistry);
        PowerMockito.when(nameServiceRegistry, "run").thenCallRealMethod();

        TopicRouteData topicRouteData = new TopicRouteData();
        List<QueueData> queueDataList = new ArrayList<>();
        topicRouteData.setQueueDatas(queueDataList);
        QueueData queueData = new QueueData();
        queueData.setPerm(PermName.PERM_WRITE);
        queueDataList.add(queueData);
        queueData = new QueueData();
        queueData.setPerm(PermName.PERM_READ);
        queueData.setReadQueueNums(8);
        queueDataList.add(queueData);
        Mockito.doReturn(topicRouteData).when(defaultMQAdminExtImpl).examineTopicRouteInfo(Mockito.any());

        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                List<Object> urlList = (List<Object>) arguments[0];
                Assert.assertEquals(urlList.size(), 8);
                return null;
            }
        }).when(notifyListener).notify(Mockito.any(List.class));

        PowerMockito.when(nameServiceRegistry, "run").thenCallRealMethod();
        Mockito.verify(notifyListener, Mockito.atLeastOnce()).notify(Mockito.any());
    }

    @Test
    public void createProviderURLTest() throws Exception {
        NameServiceRegistry nameServiceRegistry = PowerMockito.spy(new NameServiceRegistry(routuFlase));
        ServiceName serviceName = new ServiceName(routuFlase);
        URL url = Whitebox.invokeMethod(nameServiceRegistry, "createProviderURL", serviceName, routuFlase, 8);

        Assert.assertEquals(url.getProtocol(), "rocketmq");
        Assert.assertEquals(url.getParameter(CommonConstants.PROTOCOL_KEY), "rocketmq");
        Assert.assertEquals(url.getParameter(CommonConstants.INTERFACE_KEY), serviceName.getServiceInterface());
        Assert.assertEquals(url.getParameter("topic"), serviceName.getValue());
        Assert.assertEquals(url.getParameter("queueId"), 8 + "");
    }

    @Test
    public void createServiceNameTest() throws Exception {
        NameServiceRegistry nameServiceRegistry = PowerMockito.spy(new NameServiceRegistry(routuFlase));
        ServiceName serviceName = new ServiceName(routuFlase);
        ServiceName newServiceName = Whitebox.invokeMethod(nameServiceRegistry, "createServiceName", routuFlase);
        Assert.assertEquals(serviceName, newServiceName);
    }

    @Test
    public void createTopicTest() throws Exception {
        NameServiceRegistry nameServiceRegistry = new NameServiceRegistry(routuUrl);

        Field mqAdminExtField = NameServiceRegistry.class.getDeclaredField("mqAdminExt");
        mqAdminExtField.setAccessible(true);
        mqAdminExtField.set(nameServiceRegistry, this.defaultMQAdminExtImpl);

        Field isNotRouteField = NameServiceRegistry.class.getDeclaredField("isNotRoute");
        isNotRouteField.setAccessible(true);
        TopicList topicList = PowerMockito.mock(TopicList.class);
        Field topicListField = NameServiceRegistry.class.getDeclaredField("topicList");
        topicListField.setAccessible(true);
        topicListField.set(nameServiceRegistry, topicList);


        ServiceName serviceName = new ServiceName(routuUrl);
        Whitebox.invokeMethod(nameServiceRegistry, "createTopic", serviceName);
        Mockito.verify(topicList, Mockito.never()).getTopicList();

        isNotRouteField.set(nameServiceRegistry, false);
        Set<String> topic = Mockito.mock(Set.class);
        Mockito.when(topicList.getTopicList()).thenReturn(topic);
        Mockito.when(topic.contains(Mockito.any())).thenReturn(true);
        Whitebox.invokeMethod(nameServiceRegistry, "createTopic", serviceName);
        Mockito.verify(topicList, Mockito.atLeastOnce()).getTopicList();

        Mockito.when(topic.contains(Mockito.any())).thenReturn(false);

        ClusterInfo clusterInfo = PowerMockito.mock(ClusterInfo.class);
        Field clusterInfoField = NameServiceRegistry.class.getDeclaredField("clusterInfo");
        clusterInfoField.setAccessible(true);
        clusterInfoField.set(nameServiceRegistry, clusterInfo);
        Whitebox.invokeMethod(nameServiceRegistry, "createTopic", serviceName);
        Mockito.verify(defaultMQAdminExtImpl, Mockito.never()).createAndUpdateTopicConfig(Mockito.any(), Mockito.any());

        HashMap<String/* brokerName */, BrokerData> brokerAddrTable = new HashMap<>();
        BrokerData brokerData = new BrokerData();
        brokerAddrTable.put("", brokerData);
        HashMap<Long, String> brokerAddrs = new HashMap<>();
        brokerAddrs.put(1L, "");
        brokerAddrs.put(2L, "");
        brokerData.setBrokerAddrs(brokerAddrs);
        Mockito.when(clusterInfo.getBrokerAddrTable()).thenReturn(brokerAddrTable);

        Whitebox.invokeMethod(nameServiceRegistry, "createTopic", serviceName);
        Mockito.verify(defaultMQAdminExtImpl, Mockito.atMost(2)).createAndUpdateTopicConfig(Mockito.any(), Mockito.any());

    }

    @Test
    public void isAvailableTest() {
        NameServiceRegistry nameServiceRegistry = new NameServiceRegistry(routuUrl);
        Assert.assertTrue(nameServiceRegistry.isAvailable());
    }

    @Test
    public void doRegisterTest() throws Exception {
        NameServiceRegistry nameServiceRegistry = PowerMockito.spy(new NameServiceRegistry(routuUrl));

        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Assert.assertEquals(invocation.getArguments()[0].getClass(), ServiceName.class);
                return null;
            }
        }).when(nameServiceRegistry, "createTopic", Mockito.any(ServiceName.class));
        nameServiceRegistry.doRegister(this.routuUrl);
    }

    @Test(expected = RuntimeException.class)
    public void doSubscribeException() throws Exception {
        NameServiceRegistry nameServiceRegistry = PowerMockito.spy(new NameServiceRegistry(routuUrl));
        nameServiceRegistry.doSubscribe(routuUrl, null);
    }

    @Test
    public void doSubscribe() throws Exception {
        NameServiceRegistry nameServiceRegistry = PowerMockito.spy(new NameServiceRegistry(routuUrl));

        Field mqAdminExtField = NameServiceRegistry.class.getDeclaredField("mqAdminExt");
        mqAdminExtField.setAccessible(true);
        mqAdminExtField.set(nameServiceRegistry, this.defaultMQAdminExtImpl);

        URLBuilder builder = URLBuilder.from(this.routuUrl);
        builder.addParameter(RegistryConstants.CATEGORY_KEY, RegistryConstants.CONFIGURATORS_CATEGORY);
        URL url = builder.build();
        nameServiceRegistry.doSubscribe(url, null);
        PowerMockito.verifyPrivate(nameServiceRegistry, Mockito.never()).invoke("createServiceName", url);

        NotifyListener listener = PowerMockito.mock(NotifyListener.class);

        Mockito.when(this.defaultMQAdminExtImpl.queryTopicConsumeByWho(Mockito.any())).thenReturn(null);
        nameServiceRegistry.doSubscribe(routuUrl, listener);
        Mockito.verify(listener, Mockito.never()).notify(Mockito.any());

        GroupList groupList = new GroupList();
        Mockito.when(this.defaultMQAdminExtImpl.queryTopicConsumeByWho(Mockito.any())).thenReturn(groupList);
        nameServiceRegistry.doSubscribe(routuUrl, listener);
        Mockito.verify(listener, Mockito.never()).notify(Mockito.any());

        groupList.getGroupList().add("");
        nameServiceRegistry.doSubscribe(routuUrl, listener);
        Mockito.verify(listener, Mockito.atLeastOnce()).notify(Mockito.any(List.class));

        Field isNotRouteField = NameServiceRegistry.class.getDeclaredField("isNotRoute");
        isNotRouteField.setAccessible(true);
        isNotRouteField.set(nameServiceRegistry, false);

        TopicRouteData topicRouteData = PowerMockito.mock(TopicRouteData.class);
        Mockito.when(this.defaultMQAdminExtImpl.examineTopicRouteInfo(Mockito.any())).thenReturn(topicRouteData);
        nameServiceRegistry.doSubscribe(routuUrl, listener);
        Mockito.verify(listener, Mockito.atLeastOnce()).notify(Mockito.any());
        Mockito.verify(this.defaultMQAdminExtImpl, Mockito.atLeastOnce()).examineTopicRouteInfo(Mockito.any());
    }
}
