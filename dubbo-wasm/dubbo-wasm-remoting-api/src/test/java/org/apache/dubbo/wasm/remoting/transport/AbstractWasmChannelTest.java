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

package org.apache.dubbo.wasm.remoting.transport;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;
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
public class AbstractWasmChannelTest {

    @Test
    void test() {
        RustChannel channel = new RustChannel();
        try {
            channel.send("key");
            channel.getRemoteAddress();
            channel.isConnected();
            channel.hasAttribute("key");
            channel.getAttribute("key");
            channel.setAttribute("key", "value");
            channel.removeAttribute("key");
            channel.getLocalAddress();
        } catch (Exception ignored) {
        } finally {
            channel.close();
        }
    }

    static class RustChannel extends AbstractWasmChannel {
        public RustChannel() {
            super(URL.valueOf("dubbo://127.0.0.1:12345?timeout=1234&default.timeout=5678"), new ChannelHandlerAdapter());
        }

        @Override
        protected String buildWasmName(Class<?> clazz) {
            return TestHelper.WASM_NAME;
        }

        @Override
        protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store, Supplier<ByteBuffer> supplier) {
            Map<String, Func> funcs = super.initWasmCallJavaFunc(store, supplier);
            funcs.putAll(TestHelper.initWasmCallJavaFunc(store, supplier));
            return funcs;
        }

        @Override
        protected Long sendArgumentId(Object message, boolean sent) {
            return 9L;
        }

        @Override
        protected Long hasAttributeArgumentId(String key) {
            return 9L;
        }

        @Override
        protected Long getAttributeArgumentId(String key) {
            return 9L;
        }

        @Override
        protected Object getAttribute(String key, Long argumentId) {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
            return result;
        }

        @Override
        protected Long setAttributeArgumentId(String key, Object value) {
            return 9L;
        }

        @Override
        protected Long removeAttributeArgumentId(String key) {
            return 9L;
        }
    }
}
