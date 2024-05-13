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

import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.wasm.WasmLoader;
import org.apache.dubbo.wasm.exception.DubboWasmException;

import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filters implemented in other languages should extend this class, we still need to write Java subclasses,
 * so we can reuse the convenient/powerful control of dubbo, such as {@link org.apache.dubbo.common.extension.Activate}.
 *
 * @see WasmLoader
 */
public abstract class AbstractWasmFilter extends WasmLoader implements Filter {

    private static final String INVOKE_METHOD_NAME = "invoke";

    protected static final Map<Long, Argument> ARGUMENTS = new ConcurrentHashMap<>();

    @Override
    public final Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return super.getWasmExtern(INVOKE_METHOD_NAME)
            .map(extern -> {
                // call WASI function
                final Long argumentId = getArgumentId(invoker, invocation);
                ARGUMENTS.put(argumentId, new Argument(invoker, invocation));
                // WASI cannot easily pass Java objects like JNI, here we pass Long
                // then we can get the argument by Long
                WasmFunctions.consumer(super.getStore(), extern.func(), WasmValType.I64)
                    .accept(argumentId);
                ARGUMENTS.remove(argumentId);
                return doInvoke(invoker, invocation, argumentId);
            })
            .orElseThrow(() -> new DubboWasmException(
                INVOKE_METHOD_NAME + " function not found in " + super.getWasmName()));
    }

    protected abstract Result doInvoke(Invoker<?> invoker, Invocation invocation, Long argumentId);

    protected abstract Long getArgumentId(Invoker<?> invoker, Invocation invocation);

    protected static class Argument {
        protected Invoker<?> invoker;
        protected Invocation invocation;

        private Argument(Invoker<?> invoker, Invocation invocation) {
            this.invoker = invoker;
            this.invocation = invocation;
        }
    }
}
