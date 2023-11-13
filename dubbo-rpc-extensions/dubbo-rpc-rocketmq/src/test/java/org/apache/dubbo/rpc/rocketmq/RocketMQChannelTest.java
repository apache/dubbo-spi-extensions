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
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.rocketmq.codec.RocketMQCountCodec;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageExt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {RocketMQChannel.class})
public class RocketMQChannelTest {

    RocketMQChannel rocketMQChannel = new RocketMQChannel();

    @Test
    public void setRocketMQCountCodecTest() {
        RocketMQCountCodec rocketMQCountCodec = new RocketMQCountCodec(null);
        rocketMQChannel.setRocketMQCountCodec(rocketMQCountCodec);
        Assert.assertEquals(rocketMQCountCodec, ReflectUtils.getFieldValue(rocketMQChannel, "rocketmqCountCodec"));
    }

    @Test
    public void setDefaultMQProducerTest() {
        DefaultMQProducer defaultMQProducer = Mockito.mock(DefaultMQProducer.class);
        rocketMQChannel.setDefaultMQProducer(defaultMQProducer);
        Assert.assertEquals(defaultMQProducer, ReflectUtils.getFieldValue(rocketMQChannel, "defaultMQProducer"));
    }

    @Test
    public void setMessageExtTest() {
        MessageExt messageExt = new MessageExt();
        rocketMQChannel.setMessageExt(messageExt);
        Assert.assertEquals(messageExt, ReflectUtils.getFieldValue(rocketMQChannel, "messageExt"));
    }

    @Test
    public void setUrlStringTest() {
        rocketMQChannel.setUrlString("1");
        Assert.assertEquals("1", ReflectUtils.getFieldValue(rocketMQChannel, "urlString"));
    }

    @Test
    public void setUrlTest() {
        URL url = new URL("", "", 1000);
        rocketMQChannel.setUrl(url);
        Assert.assertEquals(url, ReflectUtils.getFieldValue(rocketMQChannel, "url"));
    }

    @Test
    public void getLocalAddressTest() {
        Assert.assertEquals(RocketMQProtocolConstant.LOCAL_ADDRESS, rocketMQChannel.getLocalAddress());
    }

    @Test
    public void attributeTest() {
        Assert.assertNull(rocketMQChannel.getAttribute(RocketMQProtocolConstant.URL_STRING));
        Assert.assertFalse(rocketMQChannel.hasAttribute(RocketMQProtocolConstant.URL_STRING));

        rocketMQChannel.setAttribute(RocketMQProtocolConstant.URL_STRING, RocketMQProtocolConstant.URL_STRING);
        Assert.assertTrue(rocketMQChannel.hasAttribute(RocketMQProtocolConstant.URL_STRING));
        Assert.assertEquals(rocketMQChannel.getAttribute(RocketMQProtocolConstant.URL_STRING), RocketMQProtocolConstant.URL_STRING);

        rocketMQChannel.removeAttribute(RocketMQProtocolConstant.URL_STRING);
        Assert.assertNull(rocketMQChannel.getAttribute(RocketMQProtocolConstant.URL_STRING));
        Assert.assertFalse(rocketMQChannel.hasAttribute(RocketMQProtocolConstant.URL_STRING));
    }
}
