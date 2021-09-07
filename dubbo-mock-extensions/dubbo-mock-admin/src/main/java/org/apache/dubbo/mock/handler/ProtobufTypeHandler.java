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

package org.apache.dubbo.mock.handler;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.mock.exception.HandleFailException;
import org.apache.dubbo.mock.utils.ProtobufUtil;

import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

import java.lang.reflect.Method;

/**
 * handle the Protobuf object, if handle failed will throw {@link HandleFailException}.
 */
public class ProtobufTypeHandler implements TypeHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ProtobufTypeHandler.class);

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return ProtobufUtil.isProtobufClass(resultContext.getTargetType());
    }

    @Override
    public Object handleResult(ResultContext resultContext) {
        try {
            Method buildMethod = resultContext.getTargetType().getMethod("newBuilder");
            Message.Builder message = (Message.Builder) buildMethod.invoke(null);
            JsonFormat.merge(resultContext.getData(), message);
            return message.build();
        } catch (Exception e) {
            logger.warn("[Dubbo Mock] handle protobuf object failed", e);
            throw new HandleFailException(e);
        }
    }
}
