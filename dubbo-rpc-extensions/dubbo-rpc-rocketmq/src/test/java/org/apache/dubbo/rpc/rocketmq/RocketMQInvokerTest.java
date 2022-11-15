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
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.rocketmq.codec.RocketMQCountCodec;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.RequestCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {RocketMQInvoker.class, DefaultFuture.class})
public class RocketMQInvokerTest {

    private RocketMQInvoker invoker;

    private RocketMQInvoker selectTopicInvoker;

    private URL url;

    private URL selectTopic;

    private RocketMQProtocolServer service = Mockito.mock(RocketMQProtocolServer.class);

    private DefaultMQProducer defaultMQProducer = Mockito.mock(DefaultMQProducer.class);

    private RocketMQCountCodec rocketMQCountCodec = Mockito.mock(RocketMQCountCodec.class);

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException {

        Field rocketMQCountCodecField = RocketMQInvoker.class.getDeclaredField("rocketMQCountCodec");
        rocketMQCountCodecField.setAccessible(true);

        String urlString =
            "nameservice://localhost:9876/org.apache.dubbo.registry.RegistryService?application=rocketmq-provider&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=8990&release=3.0.7&route=false";
        url = URLBuilder.valueOf(urlString).addParameter("groupModel", "topic").addParameter(CommonConstants.ENABLE_TIMEOUT_COUNTDOWN_KEY, false);
        Mockito.when(service.getDefaultMQProducer()).thenReturn(defaultMQProducer);
        invoker = new RocketMQInvoker(RocketMQInvokerTest.class, url, service);
        rocketMQCountCodecField.set(invoker, rocketMQCountCodec);
        selectTopic = URLBuilder.from(url).addParameter("version", "1.0.0")
            .addParameter("group", "a")
            .addParameter("topic", "true")
            .addParameter(CommonConstants.TIMEOUT_KEY, 100)
            .addParameter("queueId", 4).build();

        selectTopicInvoker = new RocketMQInvoker(RocketMQInvokerTest.class, selectTopic, service);
        rocketMQCountCodecField.set(selectTopicInvoker, rocketMQCountCodec);

    }

    @Test
    public void newTest() {
        Assert.assertNull(ReflectUtils.getFieldValue(invoker, "version"));
        Assert.assertNull(ReflectUtils.getFieldValue(invoker, "group"));
        Assert.assertNull(ReflectUtils.getFieldValue(invoker, "groupModel"));
        Assert.assertEquals("topic", ReflectUtils.getFieldValue(invoker, "topic"));
        Assert.assertEquals(defaultMQProducer, ReflectUtils.getFieldValue(invoker, "defaultMQProducer"));
        Assert.assertEquals(CommonConstants.DEFAULT_TIMEOUT, (int) ReflectUtils.getFieldValue(invoker, "timeout"));
        Assert.assertNull(ReflectUtils.getFieldValue(invoker, "messageQueue"));

        Assert.assertEquals("1.0.0", ReflectUtils.getFieldValue(selectTopicInvoker, "version"));
        Assert.assertEquals("a", ReflectUtils.getFieldValue(selectTopicInvoker, "group"));
        Assert.assertNull(ReflectUtils.getFieldValue(selectTopicInvoker, "groupModel"));
        Assert.assertEquals("true", ReflectUtils.getFieldValue(selectTopicInvoker, "topic"));
        Assert.assertEquals(defaultMQProducer, ReflectUtils.getFieldValue(selectTopicInvoker, "defaultMQProducer"));
        Assert.assertEquals(100, (int) ReflectUtils.getFieldValue(selectTopicInvoker, "timeout"));
        Assert.assertNotNull(ReflectUtils.getFieldValue(selectTopicInvoker, "messageQueue"));
    }

    @Test
    public void doInvokeTest() throws Throwable {

        Invocation invocation = Mockito.mock(RpcInvocation.class);
        Mockito.when(invocation.getMethodName()).thenReturn("test");


        invoker.doInvoke(invocation);
        Mockito.verify(defaultMQProducer, Mockito.atLeastOnce())
            .request(Mockito.any(Message.class), Mockito.any(RequestCallback.class), Mockito.any(Long.class));
        selectTopicInvoker.doInvoke(invocation);
        Mockito.verify(defaultMQProducer, Mockito.atLeastOnce()).request(Mockito.any(Message.class), Mockito.any(MessageQueue.class), Mockito.any(
            RequestCallback.class), Mockito.any(Long.class));

        Mockito.when(invocation.getAttachment(Constants.RETURN_KEY)).thenReturn("false");
        invoker.doInvoke(invocation);
        Mockito.verify(defaultMQProducer, Mockito.atLeastOnce()).sendOneway(Mockito.any(Message.class));

        selectTopicInvoker.doInvoke(invocation);
        Mockito.verify(defaultMQProducer, Mockito.atLeastOnce()).sendOneway(Mockito.any(Message.class), Mockito.any(MessageQueue.class));
    }

    @Test(expected = RpcException.class)
    public void doInvokeRemotingTooMuchRequestExceptionTest() throws Throwable {
        Invocation invocation = Mockito.mock(RpcInvocation.class);
        Mockito.when(invocation.getAttachment(Constants.RETURN_KEY)).thenReturn("false");
        Mockito.doThrow(RemotingTooMuchRequestException.class).when(defaultMQProducer)
            .request(Mockito.any(Message.class), Mockito.any(RequestCallback.class), Mockito.any(Long.class));
        invoker.doInvoke(invocation);
    }

    @Test(expected = RpcException.class)
    public void doInvokeExceptionTest() throws Throwable {
        Invocation invocation = Mockito.mock(RpcInvocation.class);
        Mockito.when(invocation.getAttachment(Constants.RETURN_KEY)).thenReturn("false");
        Mockito.doThrow(Exception.class).when(defaultMQProducer)
            .request(Mockito.any(Message.class), Mockito.any(RequestCallback.class), Mockito.any(Long.class));
        invoker.doInvoke(invocation);
    }

    @Test
    public void calculateTimeoutTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = RocketMQInvoker.class.getDeclaredMethod("calculateTimeout", new Class[] {Invocation.class, String.class});
        method.setAccessible(true);
        Invocation invocation = Mockito.mock(RpcInvocation.class);
        RpcContext.getContext().setObjectAttachment(CommonConstants.TIMEOUT_KEY, "10000");
        int time = (int) method.invoke(invoker, new Object[] {invocation, null});
        Assert.assertEquals(time, 10000);

        TimeoutCountDown timeoutCountDown = TimeoutCountDown.newCountDown(20000, TimeUnit.MILLISECONDS);
        RpcContext.getContext().set(CommonConstants.TIME_COUNTDOWN_KEY, timeoutCountDown);
        time = (int) method.invoke(invoker, new Object[] {invocation, null});
        Assert.assertEquals(time, timeoutCountDown.timeRemaining(TimeUnit.MILLISECONDS));
    }

    @Test
    public void callbackSuccess() throws Exception {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getUserProperty(Mockito.any(String.class))).thenReturn("");
        Mockito.when(message.getBody()).thenReturn(new byte[1024]);

        RequestCallback callback = this.invoker.getRequestCallback();
        callback.onSuccess(message);
        callback.onException(new Exception());
    }
}
