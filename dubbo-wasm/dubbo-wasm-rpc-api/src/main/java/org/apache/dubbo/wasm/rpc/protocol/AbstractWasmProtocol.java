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
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
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

/**
 * Protocols implemented in other languages should extend this class, we still need to write Java subclasses,
 * so we can reuse the convenient/powerful control of dubbo, such as {@link org.apache.dubbo.common.extension.Activate}.
 *
 * @see WasmLoader
 */
public abstract class AbstractWasmProtocol extends AbstractProtocol {

    private static final String REFER_METHOD_NAME = "refer";

    private static final String EXPORT_METHOD_NAME = "export";

    protected static final Map<Long, Argument<?>> REFER_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, Invoker<?>> EXPORT_ARGUMENTS = new ConcurrentHashMap<>();

    private final WasmLoader wasmLoader;

    public AbstractWasmProtocol() {
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    protected String buildWasmName(final Class<?> clazz) {
        return clazz.getName() + ".wasm";
    }

    protected Map<String, Func> initWasmCallJavaFunc(final Store<Void> store, Supplier<ByteBuffer> supplier) {
        return null;
    }

    @Override
    protected <T> Invoker<T> protocolBindingRefer(Class<T> type, URL url) throws RpcException {
        return wasmLoader.getWasmExtern(REFER_METHOD_NAME)
            .map(extern -> {
                // call WASI function
                final Long argumentId = getArgumentId(type, url);
                REFER_ARGUMENTS.put(argumentId, new Argument<>(type, url));
                // WASI cannot easily pass Java objects like JNI, here we pass Long
                // then we can get the argument by Long
                WasmFunctions.consumer(wasmLoader.getStore(), extern.func(), WasmValType.I64)
                    .accept(argumentId);
                REFER_ARGUMENTS.remove(argumentId);
                return doRefer(type, url, argumentId);
            })
            .orElseThrow(() -> new DubboWasmException(
                REFER_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract <T> Invoker<T> doRefer(Class<T> type, URL url, Long argumentId);

    protected abstract <T> Long getArgumentId(Class<T> type, URL url);

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return wasmLoader.getWasmExtern(EXPORT_METHOD_NAME)
            .map(extern -> {
                // call WASI function
                final Long argumentId = getArgumentId(invoker);
                EXPORT_ARGUMENTS.put(argumentId, invoker);
                // WASI cannot easily pass Java objects like JNI, here we pass Long
                // then we can get the argument by Long
                WasmFunctions.consumer(wasmLoader.getStore(), extern.func(), WasmValType.I64)
                    .accept(argumentId);
                EXPORT_ARGUMENTS.remove(argumentId);
                return doExport(invoker, argumentId);
            })
            .orElseThrow(() -> new DubboWasmException(
                EXPORT_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract <T> Exporter<T> doExport(Invoker<T> invoker, Long argumentId);

    protected abstract <T> Long getArgumentId(Invoker<T> invoker);

    protected static class Argument<T> {
        protected Class<T> type;
        protected URL url;

        private Argument(Class<T> type, URL url) {
            this.type = type;
            this.url = url;
        }
    }
}
