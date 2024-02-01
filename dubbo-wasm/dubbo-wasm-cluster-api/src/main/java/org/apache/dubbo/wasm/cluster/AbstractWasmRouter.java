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
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;
import org.apache.dubbo.rpc.cluster.router.RouterResult;
import org.apache.dubbo.wasm.WasmLoader;
import org.apache.dubbo.wasm.exception.DubboWasmException;

import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routers implemented in other languages should extend this class, we still need to write Java subclasses,
 * so we can reuse the convenient/powerful control of dubbo, such as {@link org.apache.dubbo.common.extension.Activate}.
 *
 * @see WasmLoader
 */
public abstract class AbstractWasmRouter extends AbstractRouter {

    private static final String NOTIFY_METHOD_NAME = "notify";

    private static final String ROUTE_METHOD_NAME = "route";

    private static final String STOP_METHOD_NAME = "stop";

    protected static final Map<Long, Argument<?>> ARGUMENTS = new ConcurrentHashMap<>();

    private final WasmLoader wasmLoader;

    public AbstractWasmRouter() {
        this.wasmLoader = new WasmLoader(this.getClass(), this::initWasmCallJavaFunc);
    }

    protected Map<String, Func> initWasmCallJavaFunc(final Store<Void> store) {
        return null;
    }

    protected ByteBuffer getBuffer() {
        return wasmLoader.getBuffer();
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        wasmLoader.getWasmExtern(NOTIFY_METHOD_NAME).ifPresent(notify ->
            callWASI(invokers, null, null, false, notify));
    }

    @Override
    public <T> RouterResult<Invoker<T>> route(List<Invoker<T>> invokers,
                                              URL url,
                                              Invocation invocation,
                                              boolean needToPrintMessage) throws RpcException {
        return wasmLoader.getWasmExtern(ROUTE_METHOD_NAME)
            .map(route -> {
                final Long argumentId = callWASI(invokers, url, invocation, needToPrintMessage, route);
                return doRoute(invokers, url, invocation, needToPrintMessage, argumentId);
            })
            .orElseThrow(() -> new DubboWasmException(
                ROUTE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract <T> RouterResult<Invoker<T>> doRoute(List<Invoker<T>> invokers,
                                                            URL url,
                                                            Invocation invocation,
                                                            boolean needToPrintMessage,
                                                            Long argumentId);

    private <T> Long callWASI(final List<Invoker<T>> invokers,
                              final URL url,
                              final Invocation invocation,
                              boolean needToPrintMessage,
                              final Extern doExecute) {
        // WASI cannot easily pass Java objects like JNI, here we pass Long as arg
        // then we can get the argument by Long
        final Long argumentId = getArgumentId(invokers, url, invocation, needToPrintMessage);
        ARGUMENTS.put(argumentId, new Argument<>(invokers, url, invocation, needToPrintMessage));
        // call WASI function
        WasmFunctions.consumer(wasmLoader.getStore(), doExecute.func(), WasmValType.I64)
            .accept(argumentId);
        ARGUMENTS.remove(argumentId);
        return argumentId;
    }

    protected abstract <T> Long getArgumentId(List<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage);

    @Override
    public void stop() {
        wasmLoader.getWasmExtern(STOP_METHOD_NAME).ifPresent(stop ->
            WasmFunctions.consumer(wasmLoader.getStore(), stop.func()).accept());
    }

    protected static class Argument<T> {
        protected List<Invoker<T>> invokers;
        protected URL url;
        protected Invocation invocation;
        protected boolean needToPrintMessage;

        private Argument(List<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage) {
            this.invokers = invokers;
            this.url = url;
            this.invocation = invocation;
            this.needToPrintMessage = needToPrintMessage;
        }
    }
}
