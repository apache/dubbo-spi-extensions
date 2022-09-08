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

import java.util.Map;
import java.util.Objects;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.rpc.ProtocolServer;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

public class RocketMQProtocolServer implements ProtocolServer {

    private MessageListenerConcurrently messageListenerConcurrently;

    private DefaultMQProducer defaultMQProducer;

    private DefaultMQPushConsumer defaultMQPushConsumer;

    private String address;

    private String namespace;

    private URL url;

    private String model;

    private String inistanceName;

    private String producerGroup;

    private String consumerGroup;

    private boolean enableMsgTrace;

    private String customizedTraceTopic;

    private int sendMsgTimeout;


    public void setMessageListenerConcurrently(MessageListenerConcurrently messageListenerConcurrently) {
        this.messageListenerConcurrently = messageListenerConcurrently;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getAddress() {
        return this.address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void close() {
        if (Objects.nonNull(defaultMQProducer)) {
            defaultMQProducer.shutdown();
        }

        if (Objects.nonNull(defaultMQPushConsumer)) {
            defaultMQPushConsumer.shutdown();
        }
    }

    public RemotingServer getRemotingServer() {
        return null;
    }

    public URL getUrl() {
        return url;
    }

    private void getConfig() {
        this.address = url.getAddress();
        this.enableMsgTrace = url.getParameter("enableMsgTrace", false);
        this.namespace = url.getParameter("namespace");
        this.customizedTraceTopic = url.getParameter("customizedTraceTopic");
        this.sendMsgTimeout = url.getParameter("timeout", 3000);

        this.inistanceName = url.getParameter("inistanceName", "default-" + System.currentTimeMillis());
        this.producerGroup = url.getParameter("producerGroup", RocketMQProtocolConstant.PRODUCER_CROUP_NAME);
        this.consumerGroup = url.getParameter("consumerGroup", RocketMQProtocolConstant.CONSUMER_CROUP_NAME);
    }

    public synchronized void reset(URL url) {
        try {
            this.url = url;
            this.getConfig();

            DefaultMQProducer defaultMQProducer = new DefaultMQProducer(this.namespace, this.producerGroup, null, this.enableMsgTrace,
                customizedTraceTopic);
            defaultMQProducer.setNamesrvAddr(this.address);
            defaultMQProducer.setSendMsgTimeout(this.sendMsgTimeout);
            defaultMQProducer.setInstanceName("producer- " + inistanceName);
            defaultMQProducer.setSendMsgTimeout(this.sendMsgTimeout);

            defaultMQProducer.start();


            this.defaultMQProducer = defaultMQProducer;
            if (Objects.equals(this.model, CommonConstants.PROVIDER) || Objects.equals(this.model, CommonConstants.CALLBACK_INSTANCES_LIMIT_KEY)) {
                this.createConsumer();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void createConsumer() throws MQClientException {
        DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer(this.namespace,
            this.consumerGroup);

        defaultMQPushConsumer.setNamesrvAddr(this.address);
        defaultMQPushConsumer.setInstanceName("consumer- " + inistanceName);
        defaultMQPushConsumer.setConsumeThreadMin(16);
        defaultMQPushConsumer.setConsumeThreadMax(200);
        defaultMQPushConsumer.subscribe(RocketMQProtocolConstant.DUBBO_DEFAULT_PROTOCOL_TOPIC, defaultMQPushConsumer.buildMQClientId());
        defaultMQPushConsumer.setMessageListener(this.messageListenerConcurrently);

        defaultMQPushConsumer.start();

        this.defaultMQPushConsumer = defaultMQPushConsumer;
    }

    public DefaultMQProducer getDefaultMQProducer() {
        return defaultMQProducer;
    }

    public DefaultMQPushConsumer getDefaultMQPushConsumer() {
        return defaultMQPushConsumer;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

}
