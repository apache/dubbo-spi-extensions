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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.DynamicChannelBuffer;
import org.apache.dubbo.rpc.rocketmq.codec.RocketMQCountCodec;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.utils.MessageUtil;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

public class RocketMQChannel implements Channel {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    private RocketMQCountCodec rocketmqCountCodec;

    private DefaultMQProducer defaultMQProducer;

    private MessageExt messageExt;

    private String urlString;

    private URL url;

    private InetSocketAddress remoteAddress;

    public RocketMQChannel() {

    }

    public void setRocketMQCountCodec(RocketMQCountCodec rocketmqCountCodec) {
        this.rocketmqCountCodec = rocketmqCountCodec;
    }

    public void setDefaultMQProducer(DefaultMQProducer defaultMQProducer) {
        this.defaultMQProducer = defaultMQProducer;
    }

    public void setMessageExt(MessageExt messageExt) {
        this.messageExt = messageExt;
    }


    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return RocketMQProtocolConstant.LOCAL_ADDRESS;
    }

    @Override
    public void send(Object message) throws RemotingException {
        this.send(message, false);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        ChannelBuffer buffer = new DynamicChannelBuffer(2048);
        try {
            rocketmqCountCodec.encode(this, buffer, message);
        } catch (Exception e) {
            logger.error(e);
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

    @Override
    public void close() {

    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public void startClose() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
    }

    @Override
    public void removeAttribute(String key) {
        attributes.remove(key);
    }


}
