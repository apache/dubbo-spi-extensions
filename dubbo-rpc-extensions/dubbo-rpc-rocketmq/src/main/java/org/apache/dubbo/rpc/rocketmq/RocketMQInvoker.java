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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.buffer.DynamicChannelBuffer;
import org.apache.dubbo.remoting.buffer.HeapChannelBuffer;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.rocketmq.codec.RocketMQCountCodec;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.RequestCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


public class RocketMQInvoker<T> extends AbstractInvoker<T> {

    private final ReentrantLock destroyLock = new ReentrantLock();
    private final String version;
    private RocketMQCountCodec rocketMQCountCodec = new RocketMQCountCodec(FrameworkModel.defaultModel());
    private DefaultMQProducer defaultMQProducer;
    private String group;

    private MessageQueue messageQueue;

    private Channel channel = new RocketMQChannel();

    private String topic;

    private String groupModel;

    private Integer timeout;

    public RocketMQInvoker(Class<T> type, URL url, RocketMQProtocolServer rocketMQProtocolServer) {
        super(type, url);
        this.version = url.getParameter(CommonConstants.VERSION_KEY);
        this.group = url.getParameter(CommonConstants.GROUP_KEY);
        this.groupModel = url.getParameter("groupModel");
        this.defaultMQProducer = rocketMQProtocolServer.getDefaultMQProducer();
        this.topic = url.getParameter("topic");
        this.timeout = url.getParameter(CommonConstants.TIMEOUT_KEY, CommonConstants.DEFAULT_TIMEOUT);
        Integer queueId = url.getParameter("queueId", Integer.class, -1);
        if (queueId != -1) {
            messageQueue = new MessageQueue();
            messageQueue.setBrokerName(url.getParameter("brokerName"));
            messageQueue.setTopic(this.topic);
            messageQueue.setQueueId(queueId);
        }
    }


    @SuppressWarnings("deprecation")
    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {
        RpcInvocation inv = (RpcInvocation) invocation;
        final String methodName = RpcUtils.getMethodName(invocation);
        inv.setAttachment(CommonConstants.PATH_KEY, getUrl().getPath());
        inv.setAttachment(CommonConstants.VERSION_KEY, version);
        try {

            RocketMQChannel channel = new RocketMQChannel();

            channel.setUrl(getUrl());

            RpcContext.getContext().setLocalAddress(RocketMQProtocolConstant.LOCAL_ADDRESS);

            boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
            int timeout = calculateTimeout(invocation, methodName);
            invocation.put(CommonConstants.TIMEOUT_KEY, timeout);

            Request request = new Request();
            request.setData(inv);
            DynamicChannelBuffer buffer = new DynamicChannelBuffer(2048);

            rocketMQCountCodec.encode(channel, buffer, request);

            Message message = new Message(topic, null, buffer.array());
            //message.putUserProperty(MessageConst.PROPERTY_MESSAGE_TYPE, "MixAll.REPLY_MESSAGE_FLAG");

            if (!Objects.equals(this.groupModel, "topic")) {
                message.putUserProperty(CommonConstants.GENERIC_KEY, this.group);
                message.putUserProperty(CommonConstants.VERSION_KEY, this.version);
            }
            message.putUserProperty(RocketMQProtocolConstant.SEND_ADDRESS, NetUtils.getLocalHost());
            Long messageTimeout = System.currentTimeMillis() + timeout;
            message.putUserProperty(CommonConstants.TIMEOUT_KEY, messageTimeout.toString());
            message.putUserProperty(RocketMQProtocolConstant.URL_STRING, getUrl().toString());
            if (isOneway) {
                if (Objects.isNull(messageQueue)) {
                    defaultMQProducer.sendOneway(message);
                } else {
                    defaultMQProducer.sendOneway(message, messageQueue);
                }
                return AsyncRpcResult.newDefaultAsyncResult(invocation);
            } else {
                CompletableFuture<AppResponse> appResponseFuture =
                    DefaultFuture.newFuture(channel, request, timeout, this.getCallbackExecutor(getUrl(), inv))
                        .thenApply(obj -> (AppResponse) obj);
                RequestCallback dubboRequestCallback = this.getRequestCallback();
                AsyncRpcResult result = new AsyncRpcResult(appResponseFuture, inv);
                if (Objects.isNull(messageQueue)) {
                    defaultMQProducer.request(message, dubboRequestCallback, timeout);
                } else {
                    defaultMQProducer.request(message, messageQueue, dubboRequestCallback, timeout);
                }
                return result;
            }
        } catch (RemotingTooMuchRequestException e) {
            String exceptionInfo = "Invoke remote method timeout. method: "
                + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage();
            logger.error(exceptionInfo, e);
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, exceptionInfo, e);
        } catch (Exception e) {
            String exceptionInfo = "Failed to invoke remote method: "
                + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage();
            logger.error(exceptionInfo, e);
            throw new RpcException(RpcException.NETWORK_EXCEPTION, exceptionInfo, e);
        }
    }

    public RequestCallback getRequestCallback() {
        return new DubboRequestCallback();
    }

    @SuppressWarnings("deprecation")
    private int calculateTimeout(Invocation invocation, String methodName) {
        Object countdown = RpcContext.getContext().get().get(CommonConstants.TIME_COUNTDOWN_KEY);
        int timeout = 1000;
        if (countdown == null) {
            timeout = (int) RpcUtils.getTimeout(getUrl(), methodName, RpcContext.getContext(), invocation, this.timeout);
            if (getUrl().getParameter(CommonConstants.ENABLE_TIMEOUT_COUNTDOWN_KEY, false)) {
                invocation.setObjectAttachment(CommonConstants.TIMEOUT_ATTACHMENT_KEY, timeout); // pass timeout to remote server
            }
        } else {
            TimeoutCountDown timeoutCountDown = (TimeoutCountDown) countdown;
            timeout = (int) timeoutCountDown.timeRemaining(TimeUnit.MILLISECONDS);
            invocation.setObjectAttachment(CommonConstants.TIMEOUT_ATTACHMENT_KEY, timeout);// pass timeout to remote server
        }
        return timeout;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        return true;
    }

    public void destroy() {
        if (super.isDestroyed()) {
            return;
        }
        try {
            destroyLock.lock();
            if (super.isDestroyed()) {
                return;
            }
            defaultMQProducer.shutdown();
        } finally {
            destroyLock.unlock();
        }
    }

    class DubboRequestCallback implements RequestCallback {
        @SuppressWarnings("deprecation")
        @Override
        public void onSuccess(Message message) {
            try {
                RpcContext.getContext().setRemoteAddress(message.getUserProperty(RocketMQProtocolConstant.SEND_ADDRESS), 9876);

                String urlString = message.getUserProperty(RocketMQProtocolConstant.URL_STRING);
                URL url = URL.valueOf(urlString);

                RocketMQChannel channel = new RocketMQChannel();
                channel.setRemoteAddress(RpcContext.getContext().getRemoteAddress());
                channel.setUrl(url);

                HeapChannelBuffer heapChannelBuffer = new HeapChannelBuffer(message.getBody());
                Object object = (Object) rocketMQCountCodec.decode(channel, heapChannelBuffer);
                Response response = (Response) object;
                DefaultFuture.received(channel, response);
            } catch (Exception e) {
                this.onException(e);
            }
        }

        @Override
        public void onException(Throwable e) {
            Response response = new Response();
            response.setErrorMessage(e.getMessage());
            response.setStatus(Response.SERVICE_ERROR);
            DefaultFuture.received(channel, response);
            logger.error(e.getMessage(), e);
        }
    }
}
