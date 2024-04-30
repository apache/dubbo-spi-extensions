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

package org.apache.dubbo.wasm.cluster.router;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.RouterResult;
import org.apache.dubbo.wasm.cluster.router.AbstractWasmRouter;
import org.apache.dubbo.wasm.test.TestHelper;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * see dubbo-wasm/dubbo-wasm-test/src/main/rust-extensions/README.md
 */
public class AbstractWasmRouterTest {

    @Test
    void test() {
        RustRouter router = new RustRouter();
        RouterResult<Invoker<Object>> result = router.route(null, null, null, false);
        Assertions.assertNull(result.getResult());
    }

    static class RustRouter extends AbstractWasmRouter {

        @Override
        protected String buildWasmName(Class<?> clazz) {
            return TestHelper.WASM_NAME;
        }

        @Override
        protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store, Supplier<ByteBuffer> supplier) {
            return TestHelper.initWasmCallJavaFunc(store, supplier);
        }

        @Override
        protected <T> RouterResult<Invoker<T>> doRoute(List<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage, Long argumentId) {
            final String result = TestHelper.getResult(argumentId);
            assertEquals("rust result", result);
            return new RouterResult<>(invokers);
        }

        @Override
        protected <T> Long getArgumentId(List<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage) {
            return 2L;
        }
    }
}
