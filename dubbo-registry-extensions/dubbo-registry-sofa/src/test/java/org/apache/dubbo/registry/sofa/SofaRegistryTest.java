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
package org.apache.dubbo.registry.sofa;

import com.alipay.sofa.registry.server.test.TestRegistryMain;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.status.RegistryStatusChecker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@Disabled
public class SofaRegistryTest {

    private TestRegistryMain registryMain;

    private SofaRegistry sofaRegistry;

    private SofaRegistryFactory sofaRegistryFactory;

    private URL registryUrl;

    private static final String SERVICE = "org.apache.dubbo.test.injvmServie";

    private static final URL SERVICE_URL = URL.valueOf("http://localhost/" + SERVICE + "?notify=false&methods=test1,test2&category=providers,configurators,routers");


    @BeforeEach
    public void setUp() {
        this.registryMain = new TestRegistryMain();
        try {
            this.registryMain.startRegistry();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.registryUrl = URL.valueOf("sofa://localhost:9603");
        this.sofaRegistryFactory = new SofaRegistryFactory();
        this.sofaRegistry = (SofaRegistry) new SofaRegistryFactory().createRegistry(registryUrl);
    }

    @AfterEach
    public void tearDown() {
        try {
            this.registryMain.stopRegistry();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRegistry() {
        Set<URL> registered = null;
        for (int i = 0; i < 2; i++) {
            sofaRegistry.register(SERVICE_URL);
            registered = sofaRegistry.getRegistered();
            assertThat(registered.contains(SERVICE_URL), is(true));
        }
        registered = sofaRegistry.getRegistered();
        assertThat(registered.size(), is(1));
    }

    @Test
    public void testAnyHost() {
        assertThrows(IllegalStateException.class, () -> {
            URL errorUrl = URL.valueOf("sofa://0.0.0.0/");
            new SofaRegistryFactory().createRegistry(errorUrl);
        });
    }

    @Test
    public void testSubscribe() {
        NotifyListener listener = mock(NotifyListener.class);
        sofaRegistry.subscribe(SERVICE_URL, listener);
        Map<URL, Set<NotifyListener>> subscribed = sofaRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(SERVICE_URL).size(), is(1));
        sofaRegistry.unsubscribe(SERVICE_URL, listener);
        subscribed = sofaRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(SERVICE_URL).size(), is(0));
    }

    @Test
    public void testAvailable() {
        sofaRegistry.register(SERVICE_URL);
        assertThat(sofaRegistry.isAvailable(), is(true));
    }

    @Test
    public void testStatusChecker() {
        RegistryStatusChecker registryStatusChecker = new RegistryStatusChecker(ApplicationModel.defaultModel());
        Status status = registryStatusChecker.check();
        assertThat(status.getLevel(), is(Status.Level.UNKNOWN));

        Registry registry = sofaRegistryFactory.getRegistry(registryUrl);
        assertThat(registry, not(nullValue()));

        status = registryStatusChecker.check();
        assertThat(status.getLevel(), is(Status.Level.OK));

        registry.register(SERVICE_URL);
        status = registryStatusChecker.check();
        assertThat(status.getLevel(), is(Status.Level.OK));
    }

}
