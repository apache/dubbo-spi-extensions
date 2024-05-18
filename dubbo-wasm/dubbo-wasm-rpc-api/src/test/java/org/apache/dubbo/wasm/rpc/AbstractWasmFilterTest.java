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

package org.apache.dubbo.wasm.rpc;

import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
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
public class AbstractWasmFilterTest {

    @Test
    void test() {
        try (RustFilter filter = new RustFilter()) {
            filter.invoke(null, null);
        }
    }

    static class RustFilter extends AbstractWasmFilter {

        @Override
        protected String buildWasmName(Class<?> clazz) {
            return TestHelper.WASM_NAME;
        }

        @Override
        protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store, Supplier<ByteBuffer> supplier) {
            return TestHelper.initWasmCallJavaFunc(store, supplier);
        }

        @Override
        protected Result doInvoke(Invoker<?> invoker, Invocation invocation, Long argumentId) {
            final String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
            return new AppResponse();
        }

        @Override
        protected Long getArgumentId(Invoker<?> invoker, Invocation invocation) {
            return 0L;
        }
    }
}
