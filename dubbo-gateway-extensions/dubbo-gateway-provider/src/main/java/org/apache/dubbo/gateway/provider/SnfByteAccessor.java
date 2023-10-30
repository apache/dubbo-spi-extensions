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

package org.apache.dubbo.gateway.provider;

import org.apache.dubbo.common.io.UnsafeByteArrayInputStream;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.ByteAccessor;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.apache.dubbo.common.constants.CommonConstants.BYTE_ACCESSOR_KEY;

/**
 * Customize byte parsing so that execution can continue when the service does not exist
 *
 * @since 3.2.0
 */
public class SnfByteAccessor implements ByteAccessor {

    private final FrameworkModel frameworkModel;

    private final ByteAccessor customByteAccessor;

    public SnfByteAccessor(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        customByteAccessor = Optional.ofNullable(System.getProperty(BYTE_ACCESSOR_KEY))
            .filter(StringUtils::isNotBlank)
            .map(key -> frameworkModel.getExtensionLoader(ByteAccessor.class).getExtension(key))
            .orElse(null);
    }

    @Override
    public DecodeableRpcInvocation getRpcInvocation(Channel channel, Request req, InputStream is, byte proto) {

        return new SnfDecodeableRpcInvocation(frameworkModel, channel, req, is, proto);
    }

    @Override
    public DecodeableRpcResult getRpcResult(Channel channel, Response res, InputStream is, Invocation invocation, byte proto) {
        try {
            return customByteAccessor.getRpcResult(channel, res,
                new UnsafeByteArrayInputStream(readMessageData(is)),
                invocation, proto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] readMessageData(InputStream is) throws IOException {
        if (is.available() > 0) {
            byte[] result = new byte[is.available()];
            is.read(result);
            return result;
        }
        return new byte[]{};
    }
}
