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

package org.apache.dubbo.wasm.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.loadbalance.AbstractLoadBalance;
import org.apache.dubbo.wasm.WasmLoader;
import org.apache.dubbo.wasm.exception.DubboWasmException;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * LoadBalancers implemented in other languages should extend this class, we still need to write Java subclasses,
 * so we can reuse the convenient/powerful control of dubbo, such as {@link org.apache.dubbo.common.extension.Activate}.
 *
 * @see WasmLoader
 */
public abstract class AbstractWasmLoadBalance extends AbstractLoadBalance {

    private static final String DO_SELECT_METHOD_NAME = "doSelect";

    protected static final Map<Long, Argument<?>> ARGUMENTS = new ConcurrentHashMap<>();

    private final WasmLoader wasmLoader;

    public AbstractWasmLoadBalance() {
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    protected String buildWasmName(final Class<?> clazz) {
        return clazz.getName() + ".wasm";
    }

    protected Map<String, Func> initWasmCallJavaFunc(final Store<Void> store, Supplier<ByteBuffer> supplier) {
        return null;
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        return wasmLoader.getWasmExtern(DO_SELECT_METHOD_NAME)
            .map(extern -> {
                // call WASI function
                final Long argumentId = getArgumentId(invokers, url, invocation);
                ARGUMENTS.put(argumentId, new Argument<>(invokers, url, invocation));
                // WASI cannot easily pass Java objects like JNI, here we pass Long
                // then we can get the argument by Long
                final Integer index = WasmFunctions.func(wasmLoader.getStore(), extern.func(), WasmValType.I64, WasmValType.I32)
                    .call(argumentId);
                ARGUMENTS.remove(argumentId);
                return invokers.get(index);
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_SELECT_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected <T> Long getArgumentId(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        return 0L;
    }

    protected static class Argument<T> {
        protected List<Invoker<T>> invokers;
        protected URL url;
        protected Invocation invocation;

        private Argument(List<Invoker<T>> invokers, URL url, Invocation invocation) {
            this.invokers = invokers;
            this.url = url;
            this.invocation = invocation;
        }
    }
}
