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

package org.apache.dubbo.wasm.test;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TestHelper {

    public static final String WASM_NAME = "rust_extensions.wasm";

    private static final Map<Long, String> RESULTS = new ConcurrentHashMap<>();

    public static Map<String, Func> initWasmCallJavaFunc(Store<Void> store, Supplier<ByteBuffer> buf) {
        Map<String, Func> funcMap = new HashMap<>();
        funcMap.put("get_args", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
            (argId, addr, len) -> {
                ByteBuffer buffer = buf.get();
                String config = "hello from java " + argId;
                System.out.println("java side->" + config);
                for (int i = 0; i < len && i < config.length(); i++) {
                    buffer.put(addr.intValue() + i, (byte) config.charAt(i));
                }
                return Math.min(config.length(), len);
            }));
        funcMap.put("put_result", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
            (argId, addr, len) -> {
                ByteBuffer buffer = buf.get();
                byte[] bytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    bytes[i] = buffer.get(addr.intValue() + i);
                }
                String result = new String(bytes, StandardCharsets.UTF_8);
                RESULTS.put(argId, result);
                System.out.println("java side->" + result);
                return 0;
            }));
        funcMap.put("setRemoteAddressHost", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
            (respId, addr, len) -> 0));
        funcMap.put("setLocalAddressHost", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
            (respId, addr, len) -> 0));
        return funcMap;
    }

    public static String getResult(long id) {
        return RESULTS.get(id);
    }
}
