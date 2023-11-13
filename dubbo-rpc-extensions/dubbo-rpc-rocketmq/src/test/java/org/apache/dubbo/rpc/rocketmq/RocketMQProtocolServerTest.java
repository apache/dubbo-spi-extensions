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

package org.apache.dubbo.rpc.rocketmq;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({RocketMQProtocolServer.class})
public class RocketMQProtocolServerTest {

    RocketMQProtocolServer service = new RocketMQProtocolServer();

    DefaultMQProducer defaultMQProducer = PowerMockito.mock(DefaultMQProducer.class);

    DefaultMQPushConsumer defaultMQPushConsumer = PowerMockito.mock(DefaultMQPushConsumer.class);

    URL url;

    @Before
    public void before() throws Exception {
        PowerMockito.whenNew(DefaultMQProducer.class).withAnyArguments().thenReturn(defaultMQProducer);
        PowerMockito.whenNew(DefaultMQPushConsumer.class).withAnyArguments().thenReturn(defaultMQPushConsumer);
        String urlString =
            "nameservice://localhost:9876/org.apache.dubbo.registry.RegistryService?application=rocketmq-provider&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=8990&release=3.0.7&route=false";
        url = URLBuilder.valueOf(urlString);

    }

    @Test
    public void setMessageListenerConcurrentlyTest() {
        MessageListenerConcurrently messageListenerConcurrently = Mockito.mock(MessageListenerConcurrently.class);
        service.setMessageListenerConcurrently(messageListenerConcurrently);
        Assert.assertEquals(messageListenerConcurrently, ReflectUtils.getFieldValue(service, "messageListenerConcurrently"));
    }

    @Test
    public void setModelTest() {
        service.setModel("1");
        Assert.assertEquals("1", ReflectUtils.getFieldValue(service, "model"));
    }

    @Test
    public void setAddressTest() {
        service.setAddress("1");
        Assert.assertEquals("1", service.getAddress());
    }

    @Test
    public void closeTest() {

    }

    @Test
    public void resetTest() throws MQClientException {
        URL url = PowerMockito.spy(this.url);
        service.reset(url);
        Mockito.verify(defaultMQProducer, Mockito.atMostOnce()).setNamesrvAddr(Mockito.anyString());
        Mockito.verify(defaultMQProducer, Mockito.atMostOnce()).setSendMsgTimeout(Mockito.anyInt());
        Mockito.verify(defaultMQProducer, Mockito.atMostOnce()).setInstanceName(Mockito.anyString());
        Mockito.verify(defaultMQProducer, Mockito.atMostOnce()).start();
        Mockito.verify(url, Mockito.atLeastOnce()).getAddress();
        Mockito.verify(url, Mockito.atMost(7)).getParameter(Mockito.anyString());
        Mockito.verify(defaultMQPushConsumer, Mockito.never()).setNamesrvAddr(Mockito.anyString());
        Assert.assertEquals(this.defaultMQProducer, service.getDefaultMQProducer());
        Assert.assertEquals(url, this.service.getUrl());

        service.setModel(CommonConstants.PROVIDER);
        service.reset(url);
        Mockito.verify(defaultMQPushConsumer, Mockito.atMostOnce()).setNamesrvAddr(Mockito.anyString());
        Mockito.verify(defaultMQPushConsumer, Mockito.atMostOnce()).setInstanceName(Mockito.anyString());
        Mockito.verify(defaultMQPushConsumer, Mockito.atMostOnce()).setConsumeThreadMin(Mockito.anyInt());
        Mockito.verify(defaultMQPushConsumer, Mockito.atMostOnce()).setConsumeThreadMax(Mockito.anyInt());
        Mockito.verify(defaultMQPushConsumer, Mockito.atMostOnce()).subscribe(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(defaultMQPushConsumer, Mockito.atMostOnce()).setMessageListener(Mockito.any(MessageListener.class));
        Mockito.verify(defaultMQPushConsumer, Mockito.atMostOnce()).start();
        Assert.assertEquals(defaultMQPushConsumer, service.getDefaultMQPushConsumer());

        service.setModel(CommonConstants.CALLBACK_INSTANCES_LIMIT_KEY);
        service.reset(url);
        Mockito.verify(defaultMQPushConsumer, Mockito.atMost(2)).setNamesrvAddr(Mockito.anyString());

        service.close();
        Mockito.verify(defaultMQPushConsumer, Mockito.atMostOnce()).shutdown();
        Mockito.verify(defaultMQProducer, Mockito.atMostOnce()).shutdown();
    }
}
