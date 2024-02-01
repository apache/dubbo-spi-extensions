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

package org.apache.dubbo.wasm.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.RouterResult;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * see dubbo-wasm/dubbo-wasm-cluster-api/src/test/rust-router/README.md
 */
public class AbstractWasmRouterTest {

    @Test
    void test() {
        RustRouter router = new RustRouter();
        RouterResult<Invoker<Object>> result = router.route(null, null, null, false);
        Assertions.assertNull(result.getResult());
    }

    static class RustRouter extends AbstractWasmRouter {

        private static final Map<Long, String> RESULTS = new ConcurrentHashMap<>();

        @Override
        protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store) {
            Map<String, Func> funcMap = new HashMap<>();
            funcMap.put("get_args", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
                (argId, addr, len) -> {
                    String config = "hello from java " + argId;
                    System.out.println("java side->" + config);
                    assertEquals("hello from java 0", config);
                    ByteBuffer buf = super.getBuffer();
                    for (int i = 0; i < len && i < config.length(); i++) {
                        buf.put(addr.intValue() + i, (byte) config.charAt(i));
                    }
                    return Math.min(config.length(), len);
                }));
            funcMap.put("put_result", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
                (argId, addr, len) -> {
                    ByteBuffer buf = super.getBuffer();
                    byte[] bytes = new byte[len];
                    for (int i = 0; i < len; i++) {
                        bytes[i] = buf.get(addr.intValue() + i);
                    }
                    String result = new String(bytes, StandardCharsets.UTF_8);
                    assertEquals("rust result", result);
                    RESULTS.put(argId, result);
                    System.out.println("java side->" + result);
                    return 0;
                }));
            return funcMap;
        }

        @Override
        protected <T> RouterResult<Invoker<T>> doRoute(List<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage, Long argumentId) {
            final String result = RESULTS.get(argumentId);
            assertEquals("rust result", result);
            return new RouterResult<>(invokers);
        }

        @Override
        protected <T> Long getArgumentId(List<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage) {
            return 0L;
        }
    }
}
