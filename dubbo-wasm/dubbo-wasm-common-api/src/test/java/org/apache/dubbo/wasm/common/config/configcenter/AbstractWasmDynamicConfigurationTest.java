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

package org.apache.dubbo.wasm.common.config.configcenter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.wasm.test.TestHelper;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * see dubbo-wasm/dubbo-wasm-test/src/main/rust-extensions/README.md
 */
public class AbstractWasmDynamicConfigurationTest {

    @Test
    void test() {
        try (RustDynamicConfiguration configuration = new RustDynamicConfiguration()) {
            configuration.getInternalProperty("key");
            configuration.publishConfig("pathKey", "content");
            configuration.getConfig("key", "group");
            configuration.removeConfig("key", "group");
            configuration.doGetConfigKeys("groupPath");
            configuration.addListener("key", null);
            configuration.removeListener("key", null);
            configuration.publishConfigCas("key", "group", "content", new Object());
        } catch (Exception ignored) {
        }
    }

    static class RustDynamicConfiguration extends AbstractWasmDynamicConfiguration {

        public RustDynamicConfiguration() {
            super(URL.valueOf("dubbo://127.0.0.1:12345?timeout=1234&default.timeout=5678"));
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
        protected Long getInternalPropertyArgumentId(String key) {
            return 8L;
        }

        @Override
        protected Object getInternalProperty(String key, Long argumentId) {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
            return result;
        }

        @Override
        protected Long doPublishConfigArgumentId(String pathKey, String content) {
            return 8L;
        }

        @Override
        protected Long doGetConfigArgumentId(String key) {
            return null;
        }

        @Override
        protected String doGetConfig(String pathKey, Long argumentId) throws Exception {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
            return result;
        }

        @Override
        protected Long doRemoveConfigArgumentId(String key) {
            return 8L;
        }

        @Override
        protected Long doGetConfigKeysArgumentId(String groupPath) {
            return 8L;
        }

        @Override
        protected String doGetConfigKeyItem(String groupPath, Long argumentId) {
            final String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
            return result;
        }

        @Override
        protected Long doAddListenerArgumentId(String pathKey, ConfigurationListener listener, String key, String group) {
            return 8L;
        }

        @Override
        protected void doAddListener(String pathKey, ConfigurationListener listener, String key, String group, Long argumentId) {
            final String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected Long doRemoveListenerArgumentId(String pathKey, ConfigurationListener listener) {
            return 8L;
        }

        @Override
        protected void doRemoveListener(String pathKey, ConfigurationListener listener, Long argumentId) {
            final String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
        }

        @Override
        protected Long publishConfigCasArgumentId(String key, String group, String content, Object ticket) {
            return 8L;
        }
    }
}
