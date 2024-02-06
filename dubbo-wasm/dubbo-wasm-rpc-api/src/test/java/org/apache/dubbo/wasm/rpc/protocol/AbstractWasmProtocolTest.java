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

package org.apache.dubbo.wasm.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.wasm.rpc.protocol.AbstractWasmExporter;
import org.apache.dubbo.wasm.rpc.protocol.AbstractWasmInvoker;
import org.apache.dubbo.wasm.rpc.protocol.AbstractWasmProtocol;
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
public class AbstractWasmProtocolTest {

    @Test
    void test() {
        RustProtocol protocol = new RustProtocol();
        protocol.refer(Object.class, null);
        protocol.export(new RustInvoker<>(Object.class));
    }

    static class RustProtocol extends AbstractWasmProtocol {

        @Override
        protected String buildWasmName(Class<?> clazz) {
            return TestHelper.WASM_NAME;
        }

        @Override
        protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store, Supplier<ByteBuffer> supplier) {
            return TestHelper.initWasmCallJavaFunc(store, supplier);
        }

        @Override
        protected <T> Invoker<T> doRefer(Class<T> type, URL url, Long argumentId) {
            final String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
            return new RustInvoker<>(type);
        }

        @Override
        protected <T> Long getArgumentId(Class<T> type, URL url) {
            return 5L;
        }

        @Override
        protected <T> Exporter<T> doExport(Invoker<T> invoker, Long argumentId) {
            final String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
            return new RustExporter<>(invoker);
        }

        @Override
        protected <T> Long getArgumentId(Invoker<T> invoker) {
            return 6L;
        }

        @Override
        public int getDefaultPort() {
            return 0;
        }
    }

    static class RustInvoker<T> extends AbstractWasmInvoker<T> {

        public RustInvoker(Class<T> type) {
            super(type, URL.valueOf("dubbo://127.0.0.1:12345?timeout=1234&default.timeout=5678"));
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
        protected Result doInvoke(Invocation invocation, Long argumentId) {
            String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
            return new AppResponse();
        }

        @Override
        protected Long getArgumentId(Invocation invocation) {
            return 4L;
        }
    }

    static class RustExporter<T> extends AbstractWasmExporter<T> {

        public RustExporter(Invoker<T> invoker) {
            super(invoker);
        }

        @Override
        protected String buildWasmName(Class<?> clazz) {
            return TestHelper.WASM_NAME;
        }

        @Override
        protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store, Supplier<ByteBuffer> supplier) {
            return TestHelper.initWasmCallJavaFunc(store, supplier);
        }
    }
}
