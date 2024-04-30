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

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.AbstractExporter;
import org.apache.dubbo.wasm.WasmLoader;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractWasmExporter<T> extends AbstractExporter<T> {

    private static final String AFTER_UN_EXPORT_METHOD_NAME = "afterUnExport";

    private final WasmLoader wasmLoader;

    public AbstractWasmExporter(Invoker<T> invoker) {
        super(invoker);
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    protected String buildWasmName(final Class<?> clazz) {
        return clazz.getName() + ".wasm";
    }

    protected Map<String, Func> initWasmCallJavaFunc(final Store<Void> store, Supplier<ByteBuffer> supplier) {
        return null;
    }

    @Override
    public void afterUnExport() {
        wasmLoader.getWasmExtern(AFTER_UN_EXPORT_METHOD_NAME).ifPresent(afterUnExport ->
            WasmFunctions.consumer(wasmLoader.getStore(), afterUnExport.func()).accept());
    }
}
