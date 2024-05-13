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

package org.apache.dubbo.wasm.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.wasm.test.TestHelper;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * see dubbo-wasm/dubbo-wasm-test/src/main/rust-extensions/README.md
 */
public class AbstractWasmServiceDiscoveryTest {

    @Test
    void test() {
        RustServiceDiscovery discovery = new RustServiceDiscovery();
        discovery.register();
        discovery.unregister();

        Set<String> services = discovery.getServices();
        assertEquals(Collections.singleton("rust result"), services);

        List<ServiceInstance> instances = discovery.getInstances("test");
        assertEquals(1, instances.size());
        assertEquals("rust result", instances.get(0).getRegistryCluster());

        ServiceInstancesChangedListener listener = new ServiceInstancesChangedListener(new HashSet<>(), discovery);
        discovery.addServiceInstancesChangedListener(listener);
        discovery.removeServiceInstancesChangedListener(listener);
    }

    static class RustServiceDiscovery extends AbstractWasmServiceDiscovery {

        public RustServiceDiscovery() {
            super("RustServiceDiscovery", URL.valueOf("dubbo://127.0.0.1:12345?timeout=1234&default.timeout=5678"));
        }

        @Override
        protected String buildWasmName(Class<?> clazz) {
            return TestHelper.WASM_NAME;
        }

        @Override
        protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store, Supplier<ByteBuffer> supplier) {
            return TestHelper.initWasmCallJavaFunc(store, supplier);
        }

        @Override
        protected void doRegister(ServiceInstance serviceInstance, Long argumentId) throws RuntimeException {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected void doUnregister(ServiceInstance serviceInstance, Long argumentId) {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected Long getArgumentId(ServiceInstance serviceInstance) {
            return 7L;
        }

        @Override
        protected Set<String> doGetServices(Integer count) {
            Set<String> set = new HashSet<>(count);
            for (int i = 0; i < count; i++) {
                set.add(TestHelper.getResult(7));
            }
            return set;
        }

        @Override
        protected List<ServiceInstance> doGetInstances(String serviceName, Long argumentId, Integer count) {
            List<ServiceInstance> instances = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                DefaultServiceInstance instance = new DefaultServiceInstance();
                instance.setServiceName(serviceName);
                instance.setRegistryCluster(TestHelper.getResult(argumentId));
                instances.add(instance);
            }
            return instances;
        }

        @Override
        protected Long getArgumentId(String serviceName) {
            return 7L;
        }

        @Override
        protected void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener, Long argumentId) throws NullPointerException, IllegalArgumentException {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener, Long argumentId) throws IllegalArgumentException {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected Long getArgumentId(ServiceInstancesChangedListener listener) {
            return 7L;
        }
    }
}
