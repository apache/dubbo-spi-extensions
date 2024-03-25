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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.wasm.WasmLoader;
import org.apache.dubbo.wasm.exception.DubboWasmException;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class AbstractWasmInvoker<T> extends AbstractInvoker<T> {

    private static final String DO_INVOKE_METHOD_NAME = "doInvoke";

    private static final String DESTROY_METHOD_NAME = "destroy";

    private static final String DESTROY_ALL_METHOD_NAME = "destroyAll";

    protected static final Map<Long, Invocation> ARGUMENTS = new ConcurrentHashMap<>();

    private final WasmLoader wasmLoader;

    public AbstractWasmInvoker(Class<T> type, URL url) {
        super(type, url);
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    public AbstractWasmInvoker(Class<T> type, URL url, String[] keys) {
        super(type, url, keys);
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    public AbstractWasmInvoker(Class<T> type, URL url, Map<String, Object> attachment) {
        super(type, url, attachment);
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    protected String buildWasmName(final Class<?> clazz) {
        return clazz.getName() + ".wasm";
    }

    protected Map<String, Func> initWasmCallJavaFunc(final Store<Void> store, Supplier<ByteBuffer> supplier) {
        return null;
    }

    @Override
    protected Result doInvoke(Invocation invocation) {
        return wasmLoader.getWasmExtern(DO_INVOKE_METHOD_NAME)
            .map(extern -> {
                // call WASI function
                final Long argumentId = getArgumentId(invocation);
                ARGUMENTS.put(argumentId, invocation);
                // WASI cannot easily pass Java objects like JNI, here we pass Long
                // then we can get the argument by Long
                WasmFunctions.consumer(wasmLoader.getStore(), extern.func(), WasmValType.I64)
                    .accept(argumentId);
                ARGUMENTS.remove(argumentId);
                return doInvoke(invocation, argumentId);
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_INVOKE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract Result doInvoke(Invocation invocation, Long argumentId);

    protected abstract Long getArgumentId(Invocation invocation);

    @Override
    public void destroy() {
        wasmLoader.getWasmExtern(DESTROY_METHOD_NAME).ifPresent(destroyAll ->
            WasmFunctions.consumer(wasmLoader.getStore(), destroyAll.func()).accept());
        super.destroy();
    }


    public void destroyAll() {
        wasmLoader.getWasmExtern(DESTROY_ALL_METHOD_NAME).ifPresent(destroyAll ->
            WasmFunctions.consumer(wasmLoader.getStore(), destroyAll.func()).accept());
    }
}
