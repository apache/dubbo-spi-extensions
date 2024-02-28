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

package org.apache.dubbo.wasm.common.config.configcenter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.TreePathDynamicConfiguration;
import org.apache.dubbo.wasm.WasmLoader;
import org.apache.dubbo.wasm.exception.DubboWasmException;

import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * DynamicConfigurations implemented in other languages should extend this class, we still need to write Java subclasses,
 * so we can reuse the convenient/powerful control of dubbo, such as {@link org.apache.dubbo.common.extension.Activate}.
 *
 * @see WasmLoader
 */
public abstract class AbstractWasmDynamicConfiguration extends TreePathDynamicConfiguration {

    private static final String GET_INTERNAL_PROPERTY_SELECT_METHOD_NAME = "getInternalProperty";

    private static final String DO_PUBLISH_CONFIG_METHOD_NAME = "doPublishConfig";

    private static final String DO_GET_CONFIG_METHOD_NAME = "doGetConfig";

    private static final String DO_REMOVE_CONFIG_METHOD_NAME = "doRemoveConfig";

    private static final String DO_GET_CONFIG_KEYS_METHOD_NAME = "doGetConfigKeys";

    private static final String DO_GET_CONFIG_KEY_ITEM_METHOD_NAME = "doGetConfigKeyItem";

    private static final String DO_ADD_LISTENER_METHOD_NAME = "doAddListener";

    private static final String DO_REMOVE_LISTENER_METHOD_NAME = "doRemoveListener";

    private static final String DO_CLOSE_METHOD_NAME = "doClose";

    private static final String PUBLISH_CONFIG_CAS_METHOD_NAME = "publishConfigCas";

    protected static final Map<Long, String> GET_INTERNAL_PROPERTY_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, DoPublishConfigArgument> DO_PUBLISH_CONFIG_ARGUMENT_MAP = new ConcurrentHashMap<>();

    protected static final Map<Long, String> DO_GET_CONFIG_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, String> DO_REMOVE_CONFIG_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, String> DO_GET_CONFIG_KEYS_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, DoAddListenerArgument> DO_ADD_LISTENER_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, DoRemoveListenerArgument> DO_REMOVE_LISTENER_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, PublishConfigCasArgument> PUBLISH_CONFIG_CAS_ARGUMENTS = new ConcurrentHashMap<>();

    private final WasmLoader wasmLoader;

    public AbstractWasmDynamicConfiguration(URL url) {
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
    public Object getInternalProperty(String key) {
        return wasmLoader.getWasmExtern(GET_INTERNAL_PROPERTY_SELECT_METHOD_NAME)
            .map(extern -> {
                final Long argumentId = getInternalPropertyArgumentId(key);
                GET_INTERNAL_PROPERTY_ARGUMENTS.put(argumentId, key);
                WasmFunctions.consumer(wasmLoader.getStore(), extern.func(), WasmValType.I64).accept(argumentId);
                GET_INTERNAL_PROPERTY_ARGUMENTS.remove(argumentId);
                return getInternalProperty(key, argumentId);
            })
            .orElse(null);
    }

    protected Long getInternalPropertyArgumentId(String key) {
        return 0L;
    }

    protected Object getInternalProperty(String key, Long argumentId) {
        return null;
    }

    @Override
    protected boolean doPublishConfig(String pathKey, String content) throws Exception {
        return wasmLoader.getWasmExtern(DO_PUBLISH_CONFIG_METHOD_NAME)
            .map(extern -> {
                final Long argumentId = doPublishConfigArgumentId(pathKey, content);
                DO_PUBLISH_CONFIG_ARGUMENT_MAP.put(argumentId, new DoPublishConfigArgument(pathKey, content));
                Integer result = WasmFunctions.func(wasmLoader.getStore(), extern.func(), WasmValType.I64, WasmValType.I32)
                    .call(argumentId);
                DO_PUBLISH_CONFIG_ARGUMENT_MAP.remove(argumentId);
                return result != 0;
            })
            .orElseThrow(() -> new DubboWasmException(
                DO_PUBLISH_CONFIG_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }

    protected abstract Long doPublishConfigArgumentId(String pathKey, String content);

    @Override
    protected String doGetConfig(String pathKey) throws Exception {
        Optional<Extern> func = wasmLoader.getWasmExtern(DO_GET_CONFIG_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(DO_GET_CONFIG_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = doGetConfigArgumentId(pathKey);
        DO_GET_CONFIG_ARGUMENTS.put(argumentId, pathKey);
        WasmFunctions.consumer(wasmLoader.getStore(), func.get().func(), WasmValType.I64).accept(argumentId);
        DO_GET_CONFIG_ARGUMENTS.remove(argumentId);
        return doGetConfig(pathKey, argumentId);
    }

    protected abstract Long doGetConfigArgumentId(String key);

    protected abstract String doGetConfig(String pathKey, Long argumentId) throws Exception;

    @Override
    protected boolean doRemoveConfig(String pathKey) throws Exception {
        Optional<Extern> func = wasmLoader.getWasmExtern(DO_REMOVE_CONFIG_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(DO_REMOVE_CONFIG_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = doRemoveConfigArgumentId(pathKey);
        DO_REMOVE_CONFIG_ARGUMENTS.put(argumentId, pathKey);
        Integer result = WasmFunctions.func(wasmLoader.getStore(), func.get().func(), WasmValType.I64, WasmValType.I32)
            .call(argumentId);
        DO_REMOVE_CONFIG_ARGUMENTS.remove(argumentId);
        return result != 0;
    }

    protected abstract Long doRemoveConfigArgumentId(String key);

    @Override
    protected Collection<String> doGetConfigKeys(String groupPath) {
        Optional<Extern> func = wasmLoader.getWasmExtern(DO_GET_CONFIG_KEYS_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(DO_GET_CONFIG_KEYS_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = doGetConfigKeysArgumentId(groupPath);
        DO_GET_CONFIG_KEYS_ARGUMENTS.put(argumentId, groupPath);
        Integer count = WasmFunctions.func(wasmLoader.getStore(), func.get().func(), WasmValType.I64, WasmValType.I32)
            .call(argumentId);
        Optional<Extern> itemFunc = wasmLoader.getWasmExtern(DO_GET_CONFIG_KEY_ITEM_METHOD_NAME);
        if (!itemFunc.isPresent()) {
            throw new DubboWasmException(DO_GET_CONFIG_KEY_ITEM_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        Collection<String> keys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            WasmFunctions.consumer(wasmLoader.getStore(), itemFunc.get().func(), WasmValType.I64, WasmValType.I32)
                .accept(argumentId, i);
            keys.add(doGetConfigKeyItem(groupPath, argumentId));
        }
        DO_GET_CONFIG_KEYS_ARGUMENTS.remove(argumentId);
        return keys;
    }

    protected abstract Long doGetConfigKeysArgumentId(String groupPath);

    protected abstract String doGetConfigKeyItem(String groupPath, Long argumentId);

    @Override
    protected void doAddListener(String pathKey, ConfigurationListener listener, String key, String group) {
        Optional<Extern> func = wasmLoader.getWasmExtern(DO_ADD_LISTENER_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(DO_ADD_LISTENER_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = doAddListenerArgumentId(pathKey, listener, key, group);
        DO_ADD_LISTENER_ARGUMENTS.put(argumentId, new DoAddListenerArgument(pathKey, listener, key, group));
        WasmFunctions.consumer(wasmLoader.getStore(), func.get().func(), WasmValType.I64).accept(argumentId);
        DO_ADD_LISTENER_ARGUMENTS.remove(argumentId);
        doAddListener(pathKey, listener, key, group, argumentId);
    }

    protected abstract Long doAddListenerArgumentId(String pathKey, ConfigurationListener listener, String key, String group);

    protected abstract void doAddListener(String pathKey, ConfigurationListener listener, String key, String group, Long argumentId);

    @Override
    protected void doRemoveListener(String pathKey, ConfigurationListener listener) {
        Optional<Extern> func = wasmLoader.getWasmExtern(DO_REMOVE_LISTENER_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(DO_REMOVE_LISTENER_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = doRemoveListenerArgumentId(pathKey, listener);
        DO_REMOVE_LISTENER_ARGUMENTS.put(argumentId, new DoRemoveListenerArgument(pathKey, listener));
        WasmFunctions.consumer(wasmLoader.getStore(), func.get().func(), WasmValType.I64).accept(argumentId);
        DO_REMOVE_LISTENER_ARGUMENTS.remove(argumentId);
        doRemoveListener(pathKey, listener, argumentId);
    }

    protected abstract Long doRemoveListenerArgumentId(String pathKey, ConfigurationListener listener);

    protected abstract void doRemoveListener(String pathKey, ConfigurationListener listener, Long argumentId);

    @Override
    protected void doClose() throws Exception {
        Optional<Extern> func = wasmLoader.getWasmExtern(DO_CLOSE_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(DO_CLOSE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        WasmFunctions.consumer(wasmLoader.getStore(), func.get().func()).accept();
    }

    @Override
    public boolean publishConfigCas(String key, String group, String content, Object ticket) throws UnsupportedOperationException {
        return wasmLoader.getWasmExtern(PUBLISH_CONFIG_CAS_METHOD_NAME)
            .map(func -> {
                final Long argumentId = publishConfigCasArgumentId(key, group, content, ticket);
                PUBLISH_CONFIG_CAS_ARGUMENTS.put(argumentId, new PublishConfigCasArgument(key, group, content, ticket));
                Integer result = WasmFunctions.func(wasmLoader.getStore(), func.func(), WasmValType.I64, WasmValType.I32)
                    .call(argumentId);
                PUBLISH_CONFIG_CAS_ARGUMENTS.remove(argumentId);
                return result != 0;
            })
            .orElse(false);
    }

    protected Long publishConfigCasArgumentId(String key, String group, String content, Object ticket) {
        return 0L;
    }

    protected static class DoPublishConfigArgument {
        protected String pathKey;
        protected String content;

        private DoPublishConfigArgument(String pathKey, String content) {
            this.pathKey = pathKey;
            this.content = content;
        }
    }

    protected static class DoAddListenerArgument {
        protected String pathKey;
        protected ConfigurationListener listener;
        protected String key;
        protected String group;

        private DoAddListenerArgument(String pathKey, ConfigurationListener listener, String key, String group) {
            this.pathKey = pathKey;
            this.listener = listener;
            this.key = key;
            this.group = group;
        }
    }

    protected static class DoRemoveListenerArgument {
        protected String pathKey;
        protected ConfigurationListener listener;

        private DoRemoveListenerArgument(String pathKey, ConfigurationListener listener) {
            this.pathKey = pathKey;
            this.listener = listener;
        }
    }

    protected static class PublishConfigCasArgument {
        protected String key;
        protected String group;
        protected String content;
        protected Object ticket;

        private PublishConfigCasArgument(String key, String group, String content, Object ticket) {
            this.key = key;
            this.group = group;
            this.content = content;
            this.ticket = ticket;
        }
    }
}
