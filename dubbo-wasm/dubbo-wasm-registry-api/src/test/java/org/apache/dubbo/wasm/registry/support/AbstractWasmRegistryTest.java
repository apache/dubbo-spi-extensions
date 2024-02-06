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

package org.apache.dubbo.wasm.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.wasm.test.TestHelper;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * see dubbo-wasm/dubbo-wasm-test/src/main/rust-extensions/README.md
 */
public class AbstractWasmRegistryTest {

    @Test
    void test() {
        URL url = URL.valueOf("dubbo://127.0.0.1:12345?timeout=1234&default.timeout=5678");
        RustRegistry registry = new RustRegistry(url);
        registry.register(url);
        registry.unregister(url);

        MyNotifyListener notifyListener = new MyNotifyListener();
        registry.subscribe(url, notifyListener);
        registry.unsubscribe(url, notifyListener);

        assertTrue(registry.isAvailable());
    }

    static class RustRegistry extends AbstractWasmRegistry {

        public RustRegistry(URL url) {
            super(url);
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
        protected void doRegister(URL url, Long argumentId) {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected void doUnregister(URL url, Long argumentId) {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected void doSubscribe(URL url, NotifyListener listener, Long argumentId) {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected void doUnsubscribe(URL url, NotifyListener listener, Long argumentId) {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected Long getArgumentId(URL url, NotifyListener listener) {
            return 6L;
        }
    }

    static class MyNotifyListener implements NotifyListener {
        @Override
        public void notify(List<URL> urls) {

        }
    }
}
