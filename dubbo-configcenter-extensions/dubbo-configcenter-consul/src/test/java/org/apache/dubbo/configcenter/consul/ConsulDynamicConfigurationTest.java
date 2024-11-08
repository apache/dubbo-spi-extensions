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
package org.apache.dubbo.configcenter.consul;
import org.apache.dubbo.common.URL;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConsulDynamicConfigurationTest {

    private static ConsulProcess consul;
    private static URL configCenterUrl;
    private static ConsulDynamicConfiguration configuration;

    private static Consul client;
    private static KeyValueClient kvClient;

    private static ConfigurationListener configurationListener;
    private static  CountDownLatch latch;

    @BeforeAll
    public static void setUp() throws Exception {
        consul = ConsulStarterBuilder.consulStarter()
                .build()
                .start();
        configCenterUrl = URL.valueOf("consul://127.0.0.1:" + consul.getHttpPort());

        configuration = new ConsulDynamicConfiguration(configCenterUrl);
        client = Consul.builder().withHostAndPort(HostAndPort.fromParts("127.0.0.1", consul.getHttpPort())).withReadTimeoutMillis(TimeUnit.SECONDS.toMillis(11)).build();
        kvClient = client.keyValueClient();

        latch = new CountDownLatch(1);
        configurationListener = event -> {
            //test equals
            assertEquals("value", event.getContent());
            assertEquals("/dubbo/config/dubbo/abc", event.getKey());
            assertEquals("dubbo", event.getGroup());
            assertEquals(ConfigChangeType.MODIFIED, event.getChangeType());
            System.out.println("Test Passed: Configuration change is correct.");
            latch.countDown();  // Signal that the event was received
        };
    }

    @AfterAll
    public static void tearDown() throws Exception {
        consul.close();
        configuration.close();
    }

    @Test
    public void testGetConfig() {
        kvClient.putValue("/dubbo/config/dubbo/foo", "bar");
        // test equals
        assertEquals("bar", configuration.getConfig("foo", "dubbo"));
        // test does not block
        assertEquals("bar", configuration.getConfig("foo", "dubbo"));
        Assertions.assertNull(configuration.getConfig("not-exist", "dubbo"));
    }

    @Test
    public void testPublishConfig() {
        configuration.publishConfig("value", "metadata", "1");
        // test equals
        assertEquals("1", configuration.getConfig("value", "/metadata"));
        assertEquals("1", kvClient.getValueAsString("/dubbo/config/metadata/value").get());
    }

    @Test
    public void testRemoveConfig() {
        kvClient.putValue("/dubbo/config/dubbo/foo", "bar");
        configuration.removeConfig("foo","dubbo");
        //test equals
        Assertions.assertFalse(kvClient.getValue("/dubbo/config/dubbo/foo").isPresent());
    }


    @Test
    public void testAddListener() throws InterruptedException {
        configuration.addListener("abc","dubbo",configurationListener);
        kvClient.putValue("/dubbo/config/dubbo/abc", "value");
        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assertions.assertTrue(completed, "Listener event was not triggered in time.");
    }

    @Test
    public void testRemoveListener() {
        configuration.removeListener("abc","test",configurationListener);
    }

    @Test
    public void testGetConfigKeys() {
        configuration.publishConfig("v1", "metadata", "1");
        configuration.publishConfig("v2", "metadata", "2");
        configuration.publishConfig("v3", "metadata", "3");
        // test equals
        assertEquals(Arrays.asList("v1", "v2", "v3"), configuration.doGetConfigKeys("/dubbo/config/metadata"));

    }
}
