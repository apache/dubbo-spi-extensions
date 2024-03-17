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
package org.apache.dubbo.common.serialize.protobuf.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.protocol.injvm.DefaultParamDeepCopyUtil;
import org.apache.dubbo.rpc.protocol.injvm.ParamDeepCopyUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_DESERIALIZE;

public class ProtobufParamDeepCopyUtil implements ParamDeepCopyUtil {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultParamDeepCopyUtil.class);

    private ParamDeepCopyUtil delegate;

    public ProtobufParamDeepCopyUtil(ParamDeepCopyUtil delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T copy(URL url, Object src, Class<T> targetClass, Type type) {

        return copy(url, src, targetClass);
    }

    @Override
    public <T> T copy(URL url, Object src, Class<T> targetClass) {
        boolean isProtobufTypeSupported = ProtobufUtils.isSupported(targetClass);
        if (isProtobufTypeSupported) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ProtobufUtils.serialize(src, outputStream);

                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                    T deserialize = ProtobufUtils.deserialize(inputStream, targetClass);
                    return deserialize;
                } catch (IOException e) {
                    logger.error(PROTOCOL_ERROR_DESERIALIZE, "", "", "Unable to deep copy parameter to target class.", e);
                }

            } catch (Throwable e) {
                logger.error(PROTOCOL_ERROR_DESERIALIZE, "", "", "Unable to deep copy parameter to target class.", e);
            }

            if (src.getClass().equals(targetClass)) {
                return (T) src;
            } else {
                return null;
            }
        }
        return delegate.copy(url, src, targetClass);
    }


}
