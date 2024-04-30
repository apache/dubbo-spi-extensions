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

package org.apache.dubbo.wasm.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ApplicationModel;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * ServiceDiscoveries implemented in other languages should extend this class, we still need to write Java subclasses,
 * so we can reuse the convenient/powerful control of dubbo, such as {@link org.apache.dubbo.common.extension.Activate}.
 *
 * @see WasmLoader
 */
public abstract class AbstractWasmServiceDiscovery extends AbstractServiceDiscovery {

    private static final String DO_REGISTER_METHOD_NAME = "doRegister";

    private static final String DO_UNREGISTER_METHOD_NAME = "doUnregister";

    private static final String DO_DESTROY_METHOD_NAME = "doDestroy";

    private static final String GET_SERVICES_METHOD_NAME = "getServices";

    private static final String GET_INSTANCES_METHOD_NAME = "getInstances";

    private static final String ADD_SERVICE_INSTANCES_CHANGED_LISTENER_METHOD_NAME = "addServiceInstancesChangedListener";

    private static final String REMOVE_SERVICE_INSTANCES_CHANGED_LISTENER_METHOD_NAME = "removeServiceInstancesChangedListener";

    protected static final Map<Long, ServiceInstance> REGISTER_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, String> GET_INSTANCES_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, ServiceInstancesChangedListener> SERVICE_INSTANCES_CHANGED_LISTENER_ARGUMENTS
        = new ConcurrentHashMap<>();

    private final WasmLoader wasmLoader;

    public AbstractWasmServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    public AbstractWasmServiceDiscovery(String serviceName, URL registryURL) {
        super(serviceName, registryURL);
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    protected String buildWasmName(final Class<?> clazz) {
        return clazz.getName() + ".wasm";
    }

    protected Map<String, Func> initWasmCallJavaFunc(final Store<Void> store, Supplier<ByteBuffer> supplier) {
        return null;
    }

    @Override
    protected void doRegister(ServiceInstance serviceInstance) throws RuntimeException {
        wasmLoader.getWasmExtern(DO_REGISTER_METHOD_NAME)
            .map(doRegister -> {
                Long argumentId = callWASI(serviceInstance, doRegister);
                doRegister(serviceInstance, argumentId);
                return DO_REGISTER_METHOD_NAME;
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_REGISTER_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract void doRegister(ServiceInstance serviceInstance, Long argumentId) throws RuntimeException;

    @Override
    protected void doUnregister(ServiceInstance serviceInstance) {
        wasmLoader.getWasmExtern(DO_UNREGISTER_METHOD_NAME)
            .map(doUnregister -> {
                Long argumentId = callWASI(serviceInstance, doUnregister);
                doUnregister(serviceInstance, argumentId);
                return DO_UNREGISTER_METHOD_NAME;
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_UNREGISTER_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract void doUnregister(ServiceInstance serviceInstance, Long argumentId);

    private Long callWASI(final ServiceInstance serviceInstance,
                          final Extern doExecute) {
        // WASI cannot easily pass Java objects like JNI, here we pass Long as arg
        // then we can get the argument by Long
        final Long argumentId = getArgumentId(serviceInstance);
        REGISTER_ARGUMENTS.put(argumentId, serviceInstance);
        // call WASI function
        WasmFunctions.consumer(wasmLoader.getStore(), doExecute.func(), WasmValType.I64)
            .accept(argumentId);
        REGISTER_ARGUMENTS.remove(argumentId);
        return argumentId;
    }

    protected abstract Long getArgumentId(ServiceInstance serviceInstance);

    @Override
    protected void doDestroy() {
        wasmLoader.getWasmExtern(DO_DESTROY_METHOD_NAME)
            .map(doDestroy -> {
                WasmFunctions.consumer(wasmLoader.getStore(), doDestroy.func()).accept();
                return DO_DESTROY_METHOD_NAME;
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_DESTROY_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    @Override
    public Set<String> getServices() {
        return wasmLoader.getWasmExtern(GET_SERVICES_METHOD_NAME)
            .map(extern -> {
                Integer count = WasmFunctions.func(wasmLoader.getStore(), extern.func(), WasmValType.I32).call();
                return doGetServices(count);
            })
            .orElseThrow(() -> new DubboWasmException(
                GET_SERVICES_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract Set<String> doGetServices(Integer count);

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return wasmLoader.getWasmExtern(GET_INSTANCES_METHOD_NAME)
            .map(extern -> {
                // WASI cannot easily pass Java objects like JNI, here we pass Long as arg
                // then we can get the argument by Long
                final Long argumentId = getArgumentId(serviceName);
                GET_INSTANCES_ARGUMENTS.put(argumentId, serviceName);
                // call WASI function
                Integer count = WasmFunctions.func(wasmLoader.getStore(), extern.func(),
                    WasmValType.I64, WasmValType.I32).call(argumentId);
                GET_INSTANCES_ARGUMENTS.remove(argumentId);
                return doGetInstances(serviceName, argumentId, count);
            })
            .orElseThrow(() -> new DubboWasmException(
                GET_INSTANCES_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract List<ServiceInstance> doGetInstances(String serviceName, Long argumentId, Integer count);

    protected abstract Long getArgumentId(String serviceName);

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
        throws NullPointerException, IllegalArgumentException {
        wasmLoader.getWasmExtern(ADD_SERVICE_INSTANCES_CHANGED_LISTENER_METHOD_NAME)
            .map(addServiceInstancesChangedListener -> {
                Long argumentId = callWASI(listener, addServiceInstancesChangedListener);
                addServiceInstancesChangedListener(listener, argumentId);
                return ADD_SERVICE_INSTANCES_CHANGED_LISTENER_METHOD_NAME;
            })
            .orElseThrow(() -> new DubboWasmException(
                ADD_SERVICE_INSTANCES_CHANGED_LISTENER_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener, Long argumentId)
        throws NullPointerException, IllegalArgumentException;

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        wasmLoader.getWasmExtern(REMOVE_SERVICE_INSTANCES_CHANGED_LISTENER_METHOD_NAME)
            .map(addServiceInstancesChangedListener -> {
                Long argumentId = callWASI(listener, addServiceInstancesChangedListener);
                removeServiceInstancesChangedListener(listener, argumentId);
                return REMOVE_SERVICE_INSTANCES_CHANGED_LISTENER_METHOD_NAME;
            })
            .orElseThrow(() -> new DubboWasmException(
                REMOVE_SERVICE_INSTANCES_CHANGED_LISTENER_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener, Long argumentId)
        throws IllegalArgumentException;

    private Long callWASI(final ServiceInstancesChangedListener listener,
                          final Extern doExecute) {
        // WASI cannot easily pass Java objects like JNI, here we pass Long as arg
        // then we can get the argument by Long
        final Long argumentId = getArgumentId(listener);
        SERVICE_INSTANCES_CHANGED_LISTENER_ARGUMENTS.put(argumentId, listener);
        // call WASI function
        WasmFunctions.consumer(wasmLoader.getStore(), doExecute.func(), WasmValType.I64)
            .accept(argumentId);
        SERVICE_INSTANCES_CHANGED_LISTENER_ARGUMENTS.remove(argumentId);
        return argumentId;
    }

    protected abstract Long getArgumentId(ServiceInstancesChangedListener listener);
}
