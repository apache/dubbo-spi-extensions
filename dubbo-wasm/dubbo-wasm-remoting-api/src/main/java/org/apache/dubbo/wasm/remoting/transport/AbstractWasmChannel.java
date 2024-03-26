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

package org.apache.dubbo.wasm.remoting.transport;

import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.AbstractChannel;
import org.apache.dubbo.wasm.WasmLoader;
import org.apache.dubbo.wasm.exception.DubboWasmException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Channels implemented in other languages should extend this class, we still need to write Java subclasses,
 * so we can reuse the convenient/powerful control of dubbo, such as {@link org.apache.dubbo.common.extension.Activate}.
 *
 * @see WasmLoader
 */
public abstract class AbstractWasmChannel extends AbstractChannel {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWasmChannel.class);

    private static final String SEND_METHOD_NAME = "send";

    private static final String CLOSE_METHOD_NAME = "closeChannel";

    private static final String GET_REMOTE_ADDRESS_HOST_METHOD_NAME = "getRemoteAddressHost";

    private static final String GET_REMOTE_ADDRESS_PORT_METHOD_NAME = "getRemoteAddressPort";

    private static final String IS_CONNECTED_METHOD_NAME = "isConnected";

    private static final String HAS_ATTRIBUTE_METHOD_NAME = "hasAttribute";

    private static final String GET_ATTRIBUTE_METHOD_NAME = "getAttribute";

    private static final String SET_ATTRIBUTE_METHOD_NAME = "setAttribute";

    private static final String REMOVE_ATTRIBUTE_METHOD_NAME = "removeAttribute";

    private static final String GET_LOCAL_ADDRESS_HOST_METHOD_NAME = "getLocalAddressHost";

    private static final String GET_LOCAL_ADDRESS_PORT_METHOD_NAME = "getLocalAddressPort";

    protected static final Map<Long, SendArgument> SEND_ARGUMENT_MAP = new ConcurrentHashMap<>();

    protected static final Map<Long, String> GET_REMOTE_ADDRESS_RESULTS = new ConcurrentHashMap<>();

    protected static final Map<Long, String> HAS_ATTRIBUTE_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, String> GET_ATTRIBUTE_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, SetAttributeArgument> SET_ATTRIBUTE_ARGUMENT_MAP = new ConcurrentHashMap<>();

    protected static final Map<Long, String> REMOVE_ATTRIBUTE_ARGUMENTS = new ConcurrentHashMap<>();

    protected static final Map<Long, String> GET_LOCAL_ADDRESS_RESULTS = new ConcurrentHashMap<>();

    private final WasmLoader wasmLoader;

    public AbstractWasmChannel(URL url, ChannelHandler handler) {
        super(url, handler);
        this.wasmLoader = new WasmLoader(this.getClass(), this::buildWasmName, this::initWasmCallJavaFunc);
    }

    protected String buildWasmName(final Class<?> clazz) {
        return clazz.getName() + ".wasm";
    }

    protected Map<String, Func> initWasmCallJavaFunc(final Store<Void> store, Supplier<ByteBuffer> buf) {
        Map<String, Func> funcMap = new HashMap<>();
        funcMap.put("setRemoteAddressHost", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
            (respId, addr, len) -> {
                ByteBuffer buffer = buf.get();
                byte[] bytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    bytes[i] = buffer.get(addr.intValue() + i);
                }
                GET_REMOTE_ADDRESS_RESULTS.put(respId, new String(bytes, StandardCharsets.UTF_8));
                return 0;
            }));
        funcMap.put("setLocalAddressHost", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
            (respId, addr, len) -> {
                ByteBuffer buffer = buf.get();
                byte[] bytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    bytes[i] = buffer.get(addr.intValue() + i);
                }
                GET_LOCAL_ADDRESS_RESULTS.put(respId, new String(bytes, StandardCharsets.UTF_8));
                return 0;
            }));
        return funcMap;
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        super.send(message, sent);
        Optional<Extern> func = wasmLoader.getWasmExtern(SEND_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(SEND_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = sendArgumentId(message, sent);
        SEND_ARGUMENT_MAP.put(argumentId, new SendArgument(message, sent));
        WasmFunctions.consumer(wasmLoader.getStore(), func.get().func(), WasmValType.I64).accept(argumentId);
        SEND_ARGUMENT_MAP.remove(argumentId);
    }

    protected abstract Long sendArgumentId(Object message, boolean sent);

    @Override
    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        Optional<Extern> func = wasmLoader.getWasmExtern(CLOSE_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(CLOSE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        WasmFunctions.consumer(wasmLoader.getStore(), func.get().func()).accept();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        Optional<Extern> hostFunc = wasmLoader.getWasmExtern(GET_REMOTE_ADDRESS_HOST_METHOD_NAME);
        if (!hostFunc.isPresent()) {
            throw new DubboWasmException(GET_REMOTE_ADDRESS_HOST_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        Optional<Extern> portFunc = wasmLoader.getWasmExtern(GET_REMOTE_ADDRESS_PORT_METHOD_NAME);
        if (!portFunc.isPresent()) {
            throw new DubboWasmException(GET_REMOTE_ADDRESS_PORT_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long respId = WasmFunctions.func(wasmLoader.getStore(), hostFunc.get().func(), WasmValType.I64).call();
        final Integer port = WasmFunctions.func(wasmLoader.getStore(), portFunc.get().func(), WasmValType.I32).call();
        return InetSocketAddress.createUnresolved(GET_REMOTE_ADDRESS_RESULTS.get(respId), port);
    }

    @Override
    public boolean isConnected() {
        Optional<Extern> func = wasmLoader.getWasmExtern(IS_CONNECTED_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(IS_CONNECTED_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        Integer result = WasmFunctions.func(wasmLoader.getStore(), func.get().func(), WasmValType.I32).call();
        return result != 0;
    }

    @Override
    public boolean hasAttribute(String key) {
        Optional<Extern> func = wasmLoader.getWasmExtern(HAS_ATTRIBUTE_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(HAS_ATTRIBUTE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = hasAttributeArgumentId(key);
        HAS_ATTRIBUTE_ARGUMENTS.put(argumentId, key);
        Integer result = WasmFunctions.func(wasmLoader.getStore(), func.get().func(), WasmValType.I64, WasmValType.I32)
            .call(argumentId);
        HAS_ATTRIBUTE_ARGUMENTS.remove(argumentId);
        return result != 0;
    }

    protected abstract Long hasAttributeArgumentId(String key);

    @Override
    public Object getAttribute(String key) {
        Optional<Extern> func = wasmLoader.getWasmExtern(GET_ATTRIBUTE_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(GET_ATTRIBUTE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = getAttributeArgumentId(key);
        GET_ATTRIBUTE_ARGUMENTS.put(argumentId, key);
        WasmFunctions.consumer(wasmLoader.getStore(), func.get().func(), WasmValType.I64).accept(argumentId);
        GET_ATTRIBUTE_ARGUMENTS.remove(argumentId);
        return getAttribute(key, argumentId);
    }

    protected abstract Long getAttributeArgumentId(String key);

    protected abstract Object getAttribute(String key, Long argumentId);

    @Override
    public void setAttribute(String key, Object value) {
        Optional<Extern> func = wasmLoader.getWasmExtern(SET_ATTRIBUTE_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(SET_ATTRIBUTE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = setAttributeArgumentId(key, value);
        SET_ATTRIBUTE_ARGUMENT_MAP.put(argumentId, new SetAttributeArgument(key, value));
        WasmFunctions.consumer(wasmLoader.getStore(), func.get().func(), WasmValType.I64).accept(argumentId);
        SET_ATTRIBUTE_ARGUMENT_MAP.remove(argumentId);
    }

    protected abstract Long setAttributeArgumentId(String key, Object value);

    @Override
    public void removeAttribute(String key) {
        Optional<Extern> func = wasmLoader.getWasmExtern(REMOVE_ATTRIBUTE_METHOD_NAME);
        if (!func.isPresent()) {
            throw new DubboWasmException(REMOVE_ATTRIBUTE_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long argumentId = removeAttributeArgumentId(key);
        REMOVE_ATTRIBUTE_ARGUMENTS.put(argumentId, key);
        WasmFunctions.consumer(wasmLoader.getStore(), func.get().func(), WasmValType.I64).accept(argumentId);
        REMOVE_ATTRIBUTE_ARGUMENTS.remove(argumentId);
    }

    protected abstract Long removeAttributeArgumentId(String key);

    @Override
    public InetSocketAddress getLocalAddress() {
        Optional<Extern> hostFunc = wasmLoader.getWasmExtern(GET_LOCAL_ADDRESS_HOST_METHOD_NAME);
        if (!hostFunc.isPresent()) {
            throw new DubboWasmException(GET_LOCAL_ADDRESS_HOST_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        Optional<Extern> portFunc = wasmLoader.getWasmExtern(GET_LOCAL_ADDRESS_PORT_METHOD_NAME);
        if (!portFunc.isPresent()) {
            throw new DubboWasmException(GET_LOCAL_ADDRESS_PORT_METHOD_NAME + " function not found in " + wasmLoader.getWasmName());
        }
        final Long respId = WasmFunctions.func(wasmLoader.getStore(), hostFunc.get().func(), WasmValType.I64).call();
        final Integer port = WasmFunctions.func(wasmLoader.getStore(), portFunc.get().func(), WasmValType.I32).call();
        return InetSocketAddress.createUnresolved(GET_LOCAL_ADDRESS_RESULTS.get(respId), port);
    }

    protected static class SendArgument {
        protected Object message;
        protected boolean sent;

        private SendArgument(Object message, boolean sent) {
            this.message = message;
            this.sent = sent;
        }
    }

    protected static class SetAttributeArgument {
        protected String key;
        protected Object value;

        private SetAttributeArgument(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }
}
