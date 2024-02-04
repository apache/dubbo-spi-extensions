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
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.wasm.WasmLoader;
import org.apache.dubbo.wasm.exception.DubboWasmException;

import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registries implemented in other languages should extend this class, we still need to write Java subclasses,
 * so we can reuse the convenient/powerful control of dubbo, such as {@link org.apache.dubbo.common.extension.Activate}.
 *
 * @see WasmLoader
 */
public abstract class AbstractWasmRegistry extends FailbackRegistry {

    private static final String DO_REGISTER_METHOD_NAME = "doRegister";

    private static final String DO_UNREGISTER_METHOD_NAME = "doUnregister";

    private static final String DO_SUBSCRIBE_METHOD_NAME = "doSubscribe";

    private static final String DO_UNSUBSCRIBE_METHOD_NAME = "doUnsubscribe";

    private static final String IS_AVAILABLE_METHOD_NAME = "isAvailable";

    protected static final Map<Long, Argument> ARGUMENTS = new ConcurrentHashMap<>();

    private final WasmLoader wasmLoader;

    public AbstractWasmRegistry(URL url) {
        super(url);
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    protected String buildWasmName(final Class<?> clazz) {
        return clazz.getName() + ".wasm";
    }

    protected Map<String, Func> initWasmCallJavaFunc(final Store<Void> store, Supplier<ByteBuffer> supplier) {
        return null;
    }

    @Override
    public void doRegister(URL url) {
        wasmLoader.getWasmExtern(DO_REGISTER_METHOD_NAME)
            .map(doRegister -> {
                Long argumentId = callWASI(url, null, doRegister);
                doRegister(url, argumentId);
                return DO_REGISTER_METHOD_NAME;
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_REGISTER_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract void doRegister(URL url, Long argumentId);

    @Override
    public void doUnregister(URL url) {
        wasmLoader.getWasmExtern(DO_UNREGISTER_METHOD_NAME)
            .map(doUnregister -> {
                Long argumentId = callWASI(url, null, doUnregister);
                doUnregister(url, argumentId);
                return DO_UNREGISTER_METHOD_NAME;
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_UNREGISTER_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract void doUnregister(URL url, Long argumentId);

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        wasmLoader.getWasmExtern(DO_SUBSCRIBE_METHOD_NAME)
            .map(doSubscribe -> {
                Long argumentId = callWASI(url, listener, doSubscribe);
                doSubscribe(url, listener, argumentId);
                return DO_SUBSCRIBE_METHOD_NAME;
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_SUBSCRIBE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract void doSubscribe(URL url, NotifyListener listener, Long argumentId);

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        wasmLoader.getWasmExtern(DO_UNSUBSCRIBE_METHOD_NAME)
            .map(doUnsubscribe -> {
                Long argumentId = callWASI(url, listener, doUnsubscribe);
                doUnsubscribe(url, listener, argumentId);
                return DO_UNSUBSCRIBE_METHOD_NAME;
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_UNSUBSCRIBE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract void doUnsubscribe(URL url, NotifyListener listener, Long argumentId);

    private Long callWASI(final URL url,
                          final NotifyListener listener,
                          final Extern doExecute) {
        // WASI cannot easily pass Java objects like JNI, here we pass Long as arg
        // then we can get the argument by Long
        final Long argumentId = getArgumentId(url, listener);
        ARGUMENTS.put(argumentId, new Argument(url, listener));
        // call WASI function
        WasmFunctions.consumer(wasmLoader.getStore(), doExecute.func(), WasmValType.I64)
            .accept(argumentId);
        ARGUMENTS.remove(argumentId);
        return argumentId;
    }

    protected abstract Long getArgumentId(URL url, NotifyListener listener);

    @Override
    public boolean isAvailable() {
        Integer result = wasmLoader.getWasmExtern(IS_AVAILABLE_METHOD_NAME)
            .map(isAvailable -> WasmFunctions.func(wasmLoader.getStore(), isAvailable.func(), WasmValType.I32).call())
            .orElseThrow(() -> new DubboWasmException(
                IS_AVAILABLE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
        return result > 0;
    }

    protected static class Argument {
        protected URL url;
        protected NotifyListener listener;

        private Argument(URL url, NotifyListener listener) {
            this.url = url;
            this.listener = listener;
        }
    }
}
