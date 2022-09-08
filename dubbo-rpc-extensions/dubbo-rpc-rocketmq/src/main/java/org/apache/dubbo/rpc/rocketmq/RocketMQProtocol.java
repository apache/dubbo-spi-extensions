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

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.DynamicChannelBuffer;
import org.apache.dubbo.remoting.buffer.HeapChannelBuffer;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.rocketmq.codec.RocketMQCountCodec;

import org.apache.rocketmq.client.common.ClientErrorCode;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.utils.MessageUtil;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;


public class RocketMQProtocol extends AbstractProtocol {

    public static final String NAME = "rocketmq";

    public static final int DEFAULT_PORT = 20880;


    public RocketMQProtocol() {
    }


    public static RocketMQProtocol getDubboProtocol(ScopeModel scopeModel) {
        return (RocketMQProtocol) scopeModel.getExtensionLoader(Protocol.class).getExtension(RocketMQProtocol.NAME, false);
    }

    /**
     * <host:port,Exchanger>
     */

    @Override
    public int getDefaultPort() {
        return 9876;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();
        RocketMQExporter<T> exporter = new RocketMQExporter<T>(invoker, url, exporterMap);

        String topic = exporter.getKey();
        RocketMQProtocolServer rocketMQProtocolServer = this.openServer(url, CommonConstants.PROVIDER);
        try {
            String groupModel = url.getParameter("groupModel");
            if (Objects.nonNull(groupModel) && Objects.equals(groupModel, "select")) {
                if (Objects.isNull(url.getParameter(CommonConstants.GROUP_KEY)) &&
                    Objects.isNull((url.getParameter(CommonConstants.GROUP_KEY)))) {
                    // error
                }
                StringBuffer stringBuffer = new StringBuffer();
                boolean isGroup = false;
                if (Objects.nonNull(url.getParameter(CommonConstants.GROUP_KEY))) {
                    stringBuffer.append(CommonConstants.GROUP_KEY).append("=").append(url.getParameter(CommonConstants.GROUP_KEY));
                    isGroup = true;
                }
                if (Objects.nonNull(url.getParameter(CommonConstants.VERSION_KEY))) {
                    if (isGroup) {
                        stringBuffer.append(" and ");
                    }
                    stringBuffer.append(CommonConstants.VERSION_KEY).append("=").append(url.getParameter(CommonConstants.VERSION_KEY));
                }
                MessageSelector messageSelector = MessageSelector.bySql(stringBuffer.toString());
                rocketMQProtocolServer.getDefaultMQPushConsumer().subscribe(topic, messageSelector);
            } else {
                rocketMQProtocolServer.getDefaultMQPushConsumer().subscribe(topic, CommonConstants.ANY_VALUE);
            }
            return exporter;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RocketMQProtocolServer openServer(URL url, String model) {
        // find server.
        String key = url.getAddress();
        ProtocolServer server = serverMap.get(key);
        if (server == null) {
            synchronized (this) {
                server = serverMap.get(key);
                if (server == null) {
                    serverMap.put(key, createServer(url, key, model));
                }
                server = serverMap.get(key);

                RocketMQProtocolServer rocketMQProtocolServer = (RocketMQProtocolServer) server;
                return rocketMQProtocolServer;
            }
        } else {
            return (RocketMQProtocolServer) server;
        }
    }

    private ProtocolServer createServer(URL url, String key, String model) {
        RocketMQProtocolServer rocketMQProtocolServer = new RocketMQProtocolServer();
        rocketMQProtocolServer.setModel(model);
        DubboMessageListenerConcurrently dubboMessageListenerConcurrently = new DubboMessageListenerConcurrently();
        rocketMQProtocolServer.setMessageListenerConcurrently(dubboMessageListenerConcurrently);
        rocketMQProtocolServer.reset(url);
        dubboMessageListenerConcurrently.defaultMQProducer = rocketMQProtocolServer.getDefaultMQProducer();
        return rocketMQProtocolServer;
    }

    @Override
    protected <T> Invoker<T> protocolBindingRefer(Class<T> type, URL url) throws RpcException {
        try {
            RocketMQProtocolServer rocketMQProtocolServer = this.openServer(url, CommonConstants.CONSUMER);
            RocketMQInvoker<T> rocketMQInvoker = new RocketMQInvoker<>(type, url, rocketMQProtocolServer);
            return rocketMQInvoker;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private class DubboMessageListenerConcurrently implements MessageListenerConcurrently {

        private RocketMQCountCodec rocketmqCountCodec = new RocketMQCountCodec(FrameworkModel.defaultModel());

        private DefaultMQProducer defaultMQProducer;

        @SuppressWarnings("deprecation")
        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {

            for (MessageExt messageExt : msgs) {
                String timeoutString = messageExt.getUserProperty(CommonConstants.TIMEOUT_KEY);
                Long timeout = Long.valueOf(timeoutString);

                RpcContext.getContext().setRemoteAddress(messageExt.getUserProperty(RocketMQProtocolConstant.SEND_ADDRESS), 9876);
                String urlString = messageExt.getUserProperty(RocketMQProtocolConstant.URL_STRING);
                URL url = URL.valueOf(urlString);

                RocketMQChannel channel = new RocketMQChannel();
                channel.setRemoteAddress(RpcContext.getContext().getRemoteAddress());
                channel.setUrl(url);
                channel.setUrlString(urlString);
                channel.setMessageExt(messageExt);
                channel.setDefaultMQProducer(defaultMQProducer);
                channel.setRocketMQCountCodec(rocketmqCountCodec);


                Response response = new Response();
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("reply message ext is : %s", messageExt));
                    }
                    if (Objects.isNull(messageExt.getProperty(MessageConst.PROPERTY_CLUSTER))) {
                        MQClientException exception = new MQClientException(ClientErrorCode.CREATE_REPLY_MESSAGE_EXCEPTION, "create reply message fail, requestMessage error, property[" + MessageConst.PROPERTY_CLUSTER + "] is null.");
                        response.setErrorMessage(exception.getMessage());
                        response.setStatus(Response.BAD_REQUEST);
                        logger.error(exception);
                    } else {
                        HeapChannelBuffer heapChannelBuffer = new HeapChannelBuffer(messageExt.getBody());
                        Object object = rocketmqCountCodec.decode(channel, heapChannelBuffer);
                        String topic = messageExt.getTopic();
                        Invocation inv = (Invocation) ((Request) object).getData();
                        if (timeout < System.currentTimeMillis()) {
                            logger.warn(String.format("message timeoute time is %d invocation is %s ", timeout, inv));
                            continue;
                        }
                        Invoker<?> invoker = exporterMap.get(topic).getInvoker();

                        RpcContext.getContext().setRemoteAddress(channel.getRemoteAddress());
                        Result result = invoker.invoke(inv);
                        response.setStatus(Response.OK);
                        response.setResult(result);
                    }
                } catch (Exception e) {
                    response.setErrorMessage(e.getMessage());
                    response.setStatus(Response.BAD_REQUEST);
                    logger.error(e);

                }
                ChannelBuffer buffer = new DynamicChannelBuffer(2048);
                try {
                    rocketmqCountCodec.encode(channel, buffer, response);
                } catch (Exception e) {
                    response.setErrorMessage(e.getMessage());
                    response.setStatus(Response.BAD_REQUEST);
                    logger.error(e);
                    try {
                        buffer = new DynamicChannelBuffer(2048);
                        rocketmqCountCodec.encode(channel, buffer, response);
                    } catch (IOException e1) {
                        logger.error(e1);
                        continue;
                    }
                }
                try {
                    Message newMessage = MessageUtil.createReplyMessage(messageExt, buffer.array());
                    newMessage.putUserProperty(RocketMQProtocolConstant.SEND_ADDRESS, RocketMQProtocolConstant.LOCAL_ADDRESS.getHostString());
                    newMessage.putUserProperty(RocketMQProtocolConstant.URL_STRING, urlString);
                    SendResult sendResult = defaultMQProducer.send(newMessage, 3000);
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("send result is : %s", sendResult));
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }

    }

}
