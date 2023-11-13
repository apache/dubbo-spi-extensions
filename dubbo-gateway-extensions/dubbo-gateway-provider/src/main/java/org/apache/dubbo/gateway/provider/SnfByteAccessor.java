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

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.ByteAccessor;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;

import java.io.InputStream;

/**
 * Customize byte parsing so that execution can continue when the service does not exist
 *
 * @since 3.2.0
 */
public class SnfByteAccessor implements ByteAccessor {

    private final FrameworkModel frameworkModel;

    public SnfByteAccessor(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public DecodeableRpcInvocation getRpcInvocation(Channel channel, Request req, InputStream is, byte proto) {

        return new SnfDecodeableRpcInvocation(frameworkModel, channel, req, is, proto);
    }
}
