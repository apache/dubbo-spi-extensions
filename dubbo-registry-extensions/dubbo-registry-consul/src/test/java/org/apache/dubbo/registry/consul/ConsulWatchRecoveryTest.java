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
package org.apache.dubbo.registry.consul;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsulWatchRecoveryTest {

    @Test
    void registryWatchResumesAfterServerError() throws Exception {
        HttpServer server = startConsul();
        ConsulRegistry registry = new ConsulRegistry(registryUrl(server));
        URL consumer = URL.valueOf("consumer://127.0.0.1/example.Service");
        CountDownLatch update = new CountDownLatch(2);
        try {
            registry.subscribe(consumer, urls -> update.countDown());
            assertTrue(update.await(5, TimeUnit.SECONDS), "The registry watch should deliver the next update");
        } finally {
            registry.destroy();
            server.stop(0);
        }
    }

    @Test
    void serviceDiscoveryWatchResumesAfterServerError() throws Exception {
        HttpServer server = startConsul();
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("test"));
        ConsulServiceDiscovery discovery = new ConsulServiceDiscovery(applicationModel, registryUrl(server));
        CountDownLatch update = new CountDownLatch(1);
        try {
            discovery.addServiceInstancesChangedListener(
                    new ServiceInstancesChangedListener(Collections.singleton("example.Service"), discovery) {
                        @Override
                        public void onEvent(ServiceInstancesChangedEvent event) {
                            update.countDown();
                        }
                    });
            assertTrue(update.await(5, TimeUnit.SECONDS), "The discovery watch should deliver the next update");
        } finally {
            discovery.destroy();
            applicationModel.destroy();
            server.stop(0);
        }
    }

    @Test
    void wildcardWatchRetriesServiceLookupWithoutSkippingCatalogIndex() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger catalogRequests = new AtomicInteger();
        AtomicInteger healthRequests = new AtomicInteger();
        server.createContext("/v1/catalog/services", exchange -> {
            boolean initial = catalogRequests.incrementAndGet() == 1;
            respond(exchange, initial ? 10 : 11, initial ? "{}" : "{\"example.Service\":[\"dubbo\"]}");
        });
        server.createContext("/v1/health/service/example.Service", exchange -> {
            if (healthRequests.incrementAndGet() == 1) {
                unavailable(exchange);
            } else {
                respond(exchange, 11, "[]");
            }
        });
        server.start();

        ConsulRegistry registry = new ConsulRegistry(registryUrl(server));
        URL consumer = URL.valueOf("consumer://127.0.0.1/*");
        CountDownLatch update = new CountDownLatch(2);
        try {
            registry.subscribe(consumer, urls -> update.countDown());
            assertTrue(update.await(5, TimeUnit.SECONDS), "The wildcard watch should retry the same catalog index");
        } finally {
            registry.destroy();
            server.stop(0);
        }
    }

    private static URL registryUrl(HttpServer server) {
        return URL.valueOf("consul://127.0.0.1:" + server.getAddress().getPort());
    }

    private static HttpServer startConsul() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/v1/health/service/example.Service", exchange -> {
            int request = requests.incrementAndGet();
            if (request == 2) {
                unavailable(exchange);
            } else {
                respond(exchange, request == 1 ? 10 : 11, "[]");
            }
        });
        server.start();
        return server;
    }

    private static void respond(HttpExchange exchange, int index, String content) throws IOException {
        try {
            byte[] body = content.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("X-Consul-Index", String.valueOf(index));
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    private static void unavailable(HttpExchange exchange) throws IOException {
        try {
            exchange.sendResponseHeaders(503, -1);
        } finally {
            exchange.close();
        }
    }

}
