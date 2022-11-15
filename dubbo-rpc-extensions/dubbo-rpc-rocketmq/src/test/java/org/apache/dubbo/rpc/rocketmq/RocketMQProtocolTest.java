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
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.rocketmq.codec.RocketMQCountCodec;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;


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
@PrepareForTest(value = {RocketMQProtocol.class})
public class RocketMQProtocolTest {

    private final RocketMQProtocol pocketMQProtocol = PowerMockito.spy(new RocketMQProtocol());

    private final DefaultMQProducer defaultMQProducer = PowerMockito.mock(DefaultMQProducer.class);

    private final RocketMQProtocolServer rocketMQProtocolServer = PowerMockito.mock(RocketMQProtocolServer.class);

    private final RocketMQExporter<Object> rocketMQExporter = PowerMockito.mock(RocketMQExporter.class);

    private final RocketMQCountCodec codec = PowerMockito.mock(RocketMQCountCodec.class);
    private final List<MessageExt> msgs = new ArrayList<>();
    private final MessageExt messageExt = new MessageExt();
    private MessageListenerConcurrently messageListenerConcurrently;
    private Response response;

    private URL url;


    @Before
    public void before() throws Exception {
        String urlString =
            "nameservice://localhost:9876/org.apache.dubbo.registry.RegistryService?application=rocketmq-provider&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=8990&release=3.0.7&route=false";
        url = URLBuilder.valueOf(urlString).addParameter("groupModel", "topic").addParameter(CommonConstants.ENABLE_TIMEOUT_COUNTDOWN_KEY, false);

        PowerMockito.whenNew(RocketMQProtocolServer.class).withAnyArguments().thenReturn(rocketMQProtocolServer);
        PowerMockito.when(rocketMQProtocolServer.getDefaultMQProducer()).thenReturn(defaultMQProducer);
        PowerMockito.whenNew(RocketMQExporter.class).withAnyArguments().thenReturn(rocketMQExporter);

        PowerMockito.whenNew(RocketMQCountCodec.class).withAnyArguments().thenReturn(codec);
        PowerMockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation)  {
                messageListenerConcurrently = invocation.getArgument(0);
                messageListenerConcurrently = PowerMockito.spy(messageListenerConcurrently);
                return null;
            }
        }).when(rocketMQProtocolServer).setMessageListenerConcurrently(Mockito.any(MessageListenerConcurrently.class));
        PowerMockito.when(pocketMQProtocol, "createServer", Mockito.any(URL.class), Mockito.anyString(), Mockito.anyString()).thenCallRealMethod();

        messageExt.setTopic("test");
        messageExt.putUserProperty(CommonConstants.TIMEOUT_KEY, (System.currentTimeMillis() + 100000) + "");
        messageExt.putUserProperty(RocketMQProtocolConstant.URL_STRING, urlString);
        messageExt.setBody(new byte[1024]);

        msgs.add(messageExt);

    }

    @Test
    public void createServerTest() throws Exception {
        PowerMockito.when(pocketMQProtocol, "createServer", url, "", "").thenCallRealMethod();

        Mockito.verify(rocketMQProtocolServer, Mockito.atLeastOnce()).setModel(Mockito.any(String.class));
        Mockito.verify(rocketMQProtocolServer, Mockito.atLeastOnce()).setMessageListenerConcurrently(Mockito.any(MessageListenerConcurrently.class));
        Mockito.verify(rocketMQProtocolServer, Mockito.atLeastOnce()).reset(url);

    }

    @Test
    public void openServerTest() throws Exception {
        Field serverMapField = AbstractProtocol.class.getDeclaredField("serverMap");
        serverMapField.setAccessible(true);
        Map<String, ProtocolServer> serverMap = (Map<String, ProtocolServer>) serverMapField.get(pocketMQProtocol);
        Assert.assertTrue(serverMap.isEmpty());

        RocketMQProtocolServer rocketMQProtocolServer = Whitebox.invokeMethod(pocketMQProtocol, "openServer", url, "");
        Assert.assertEquals(1, serverMap.size());
        PowerMockito.verifyPrivate(pocketMQProtocol).invoke("createServer", Mockito.any(URL.class), Mockito.anyString(), Mockito.anyString());

        RocketMQProtocolServer newRocketMQProtocolServer = Whitebox.invokeMethod(pocketMQProtocol, "openServer", url, "");
        Assert.assertEquals(rocketMQProtocolServer, newRocketMQProtocolServer);
    }

    @Test(expected = RuntimeException.class)
    public void createMessageSelectorTest() throws Exception {
        URL url = URLBuilder.from(this.url).addParameter(CommonConstants.GROUP_KEY, "test").build();
        MessageSelector messageSelector = Whitebox.invokeMethod(pocketMQProtocol, "createMessageSelector", url);
        Assert.assertEquals("group=test", messageSelector.getExpression());

        url = URLBuilder.from(url).addParameter(CommonConstants.VERSION_KEY, "1.0.0").build();
        messageSelector = Whitebox.invokeMethod(pocketMQProtocol, "createMessageSelector", url);
        Assert.assertEquals("group=test and version=1.0.0", messageSelector.getExpression());

        url = URLBuilder.from(this.url).addParameter(CommonConstants.VERSION_KEY, "1.0.0").build();
        messageSelector = Whitebox.invokeMethod(pocketMQProtocol, "createMessageSelector", url);
        Assert.assertEquals("version=1.0.0", messageSelector.getExpression());

        Whitebox.invokeMethod(pocketMQProtocol, "createMessageSelector", this.url);

    }

    @Test(expected = RpcException.class)
    public void exportTest() throws MQClientException {
        Invoker<Object> invoker = PowerMockito.mock(Invoker.class);
        PowerMockito.when(invoker.getUrl()).thenReturn(this.url);
        PowerMockito.when(rocketMQExporter.getKey()).thenReturn("test");

        DefaultMQPushConsumer consumer = PowerMockito.mock(DefaultMQPushConsumer.class);
        PowerMockito.when(this.rocketMQProtocolServer.getDefaultMQPushConsumer()).thenReturn(consumer);
        pocketMQProtocol.export(invoker);
        Mockito.verify(consumer, Mockito.atLeastOnce()).subscribe(Mockito.anyString(), Mockito.anyString());

        url = URLBuilder.from(url).addParameter("groupModel", "select").addParameter(CommonConstants.GROUP_KEY, "111").build();
        PowerMockito.when(invoker.getUrl()).thenReturn(this.url);
        pocketMQProtocol.export(invoker);
        Mockito.verify(consumer, Mockito.atLeastOnce()).subscribe(Mockito.anyString(), Mockito.any(MessageSelector.class));

        PowerMockito.doThrow(new RpcException()).when(consumer).subscribe(Mockito.anyString(), Mockito.any(MessageSelector.class));
        pocketMQProtocol.export(invoker);

    }

    @Test(expected = RpcException.class)
    public void exportExceptionTest() throws Exception {
        PowerMockito.when(pocketMQProtocol, "createServer", Mockito.any(URL.class), Mockito.anyString(), Mockito.anyString())
            .thenThrow(RpcException.class);
        Invoker<Object> invoker = PowerMockito.mock(Invoker.class);
        pocketMQProtocol.export(invoker);
    }


    @Test(expected = RpcException.class)
    public void protocolBindingReferTest() throws Exception {
        RocketMQInvoker<Object> invoker = PowerMockito.mock(RocketMQInvoker.class);
        PowerMockito.whenNew(RocketMQInvoker.class).withAnyArguments().thenReturn(invoker);

        PowerMockito.when(this.rocketMQProtocolServer.getDefaultMQProducer()).thenReturn(defaultMQProducer);

        PowerMockito.doReturn(this.rocketMQProtocolServer).when(pocketMQProtocol, "openServer", this.url, CommonConstants.CONSUMER);
        Invoker<Object> newInvoker = pocketMQProtocol.protocolBindingRefer(Object.class, this.url);
        PowerMockito.verifyPrivate(pocketMQProtocol).invoke("openServer", Mockito.any(URL.class), Mockito.anyString());
        Assert.assertEquals(newInvoker, invoker);


        PowerMockito.when(pocketMQProtocol, "openServer", this.url, CommonConstants.CONSUMER).thenThrow(RuntimeException.class);
        pocketMQProtocol.protocolBindingRefer(Object.class, this.url);

    }

    @Test
    public void consumeMessageTest() throws Exception {
        messageExt.putUserProperty(RocketMQProtocolConstant.SEND_ADDRESS, "127.0.0.1");
        ExecutorService executorService = PowerMockito.mock(ExecutorService.class);
        PowerMockito.when(rocketMQProtocolServer.getExecutorService()).thenReturn(executorService);
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        }).when(executorService).submit(Mockito.any(Runnable.class));
        messageListenerConcurrently.consumeMessage(this.msgs, null);

        Mockito.verify(executorService, Mockito.atLeastOnce()).submit(Mockito.any(Runnable.class));
        PowerMockito.verifyPrivate(messageListenerConcurrently).invoke("execute",Mockito.any(MessageExt.class));

    }

    @Test
    public void executeTest() throws Exception {
        messageExt.putUserProperty(RocketMQProtocolConstant.SEND_ADDRESS, "127.0.0.1");

        PowerMockito.when(messageListenerConcurrently, "invoke", Mockito.any(), Mockito.any(), Mockito.any()).thenReturn(new Response())
            .thenReturn(null);

        Whitebox.invokeMethod(messageListenerConcurrently, "execute", new Class[] {MessageExt.class},
            this.messageExt);

        Whitebox.invokeMethod(messageListenerConcurrently, "execute", new Class[] {MessageExt.class},
            this.messageExt);
    }


    @Test
    public void invokeTest() throws Exception {

        Channel channel = PowerMockito.mock(Channel.class);

        Response response = Whitebox.invokeMethod(messageListenerConcurrently, "invoke", new Class[] {MessageExt.class, Channel.class, URL.class},
            this.messageExt, channel, url);
        Assert.assertEquals(response.getStatus(), Response.BAD_REQUEST);

        Request request = PowerMockito.mock(Request.class);
        PowerMockito.when(codec.decode(Mockito.any(), Mockito.any())).thenReturn(request);
        Invocation inv = PowerMockito.mock(Invocation.class);
        PowerMockito.when(request.getData()).thenReturn(inv);

        Field exporterMapField = AbstractProtocol.class.getDeclaredField("exporterMap");
        exporterMapField.setAccessible(true);
        Map<String, Exporter<?>> exporterMap = (Map<String, Exporter<?>>) exporterMapField.get(pocketMQProtocol);
        Exporter<Object> exporter = PowerMockito.mock(Exporter.class);
        exporterMap.put("test", exporter);

        Invoker<Object> invoker = PowerMockito.mock(Invoker.class);
        PowerMockito.when(exporter.getInvoker()).thenReturn(invoker);

        Result result = PowerMockito.mock(Result.class);
        PowerMockito.when(invoker.invoke(Mockito.any(Invocation.class))).thenReturn(result);
        MessageAccessor.putProperty(messageExt, MessageConst.PROPERTY_CLUSTER, "");
        response = Whitebox.invokeMethod(messageListenerConcurrently, "invoke", new Class[] {MessageExt.class, Channel.class, URL.class},
            this.messageExt, channel, url);
        Assert.assertEquals(response.getResult(), result);

        messageExt.putUserProperty(CommonConstants.TIMEOUT_KEY, (System.currentTimeMillis() - 100000) + "");
        response = Whitebox.invokeMethod(messageListenerConcurrently, "invoke", new Class[] {MessageExt.class, Channel.class, URL.class},
            this.messageExt, channel, url);
        Assert.assertNull(response);
    }

    @Test
    public void createChannelBufferTest() throws Exception {
        this.response = new Response();
        Channel channel = PowerMockito.mock(Channel.class);

        ChannelBuffer channelBuffer =
            Whitebox.invokeMethod(messageListenerConcurrently, "createChannelBuffer", new Class[] {Channel.class, Response.class, URL.class},
                channel, response, url);
        Assert.assertNotNull(channelBuffer);
        Mockito.verify(codec, Mockito.atLeastOnce()).encode(Mockito.any(Channel.class), Mockito.any(ChannelBuffer.class), Mockito.any(Object.class));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation)  {
                Response newResponse = invocation.getArgument(2);
                Assert.assertEquals(newResponse, response);
                throw new RuntimeException();

            }
        }).doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation)  {
                Response newResponse = invocation.getArgument(2);
                Assert.assertEquals(newResponse.getStatus(), Response.BAD_REQUEST);
                return null;
            }
        }).when(codec).encode(Mockito.any(Channel.class), Mockito.any(ChannelBuffer.class), Mockito.any(Response.class));

        channelBuffer =
            Whitebox.invokeMethod(messageListenerConcurrently, "createChannelBuffer", new Class[] {Channel.class, Response.class, URL.class},
                channel, response, this.url);
        Assert.assertNotNull(channelBuffer);
        Mockito.verify(codec, Mockito.atLeast(2)).encode(Mockito.any(Channel.class), Mockito.any(ChannelBuffer.class), Mockito.any(Object.class));

        Mockito.reset(codec);
        PowerMockito.doThrow(new RuntimeException()).doThrow(new IOException()).when(codec)
            .encode(Mockito.any(Channel.class), Mockito.any(ChannelBuffer.class), Mockito.any(Object.class));
        channelBuffer =
            Whitebox.invokeMethod(messageListenerConcurrently, "createChannelBuffer", new Class[] {Channel.class, Response.class, URL.class},
                channel, response, this.url);
        Assert.assertNull(channelBuffer);
        Mockito.verify(codec, Mockito.atLeast(2)).encode(Mockito.any(Channel.class), Mockito.any(ChannelBuffer.class), Mockito.any(Object.class));
    }

    @Test
    public void sendMessageTest() throws Exception {
        MessageAccessor.putProperty(messageExt, MessageConst.PROPERTY_CLUSTER, "");
        ChannelBuffer buffer = PowerMockito.mock(ChannelBuffer.class);

        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation)  {
                long time = invocation.getArgument(1);
                Assert.assertEquals(time, 3000L);
                return null;
            }
        }).when(this.defaultMQProducer).send(Mockito.any(Message.class), Mockito.anyLong());
        boolean value = Whitebox
            .invokeMethod(messageListenerConcurrently, "sendMessage", new Class[] {MessageExt.class, ChannelBuffer.class, URL.class, String.class},
                this.messageExt, buffer, this.url, "url");
        Assert.assertTrue(value);
        Mockito.verify(defaultMQProducer, Mockito.atLeastOnce()).send(Mockito.any(Message.class), Mockito.anyLong());

        PowerMockito.when(this.defaultMQProducer.send(Mockito.any(Message.class), Mockito.anyLong())).thenThrow(new RuntimeException());
        value = Whitebox
            .invokeMethod(messageListenerConcurrently, "sendMessage", new Class[] {MessageExt.class, ChannelBuffer.class, URL.class, String.class},
                this.messageExt, buffer, this.url, "url");
        Assert.assertFalse(value);
    }

}
