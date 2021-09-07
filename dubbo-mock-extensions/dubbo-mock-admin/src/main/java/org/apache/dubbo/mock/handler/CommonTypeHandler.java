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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link CommonTypeHandler} will handle the common result, include basic type, json data and protobuf data.
 * If handle failed, then will use {@link UnknownTypeHandler} try to handle the result.
 */
public class CommonTypeHandler implements TypeHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(CommonTypeHandler.class);

    private List<TypeHandler> typeHandlers;

    private UnknownTypeHandler unknownTypeHandler;

    private JsonTypeHandler jsonTypeHandler;

    public CommonTypeHandler() {
        unknownTypeHandler = new UnknownTypeHandler();
        jsonTypeHandler = new JsonTypeHandler();
        typeHandlers = Arrays.asList(new StringTypeHandler(), new IntegerTypeHandler(), new LongTypeHandler(),
            new BigDecimalTypeHandler(), new ProtobufTypeHandler(), new DateTypeHandler(), new BooleanTypeHandler(),
            new ByteTypeHandler(), new DoubleTypeHandler(), new FloatTypeHandler(), new BigIntegerTypeHandler());
    }

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return false;
    }

    @Override
    public Object handleResult(ResultContext resultContext) {
        if (Objects.isNull(resultContext.getData())) {
            return null;
        }
        try {
            // support generic service.
            if (Objects.isNull(resultContext.getTargetType())) {
                Class<?> serviceType = Class.forName(resultContext.getServiceName());
                if (Objects.isNull(serviceType)) {
                    return null;
                }
                Method[] methods = serviceType.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    if (Objects.equals(resultContext.getMethodName(), method.getName())) {
                        resultContext.setTargetType(method.getReturnType());
                        break;
                    }
                }
            }

            Optional<TypeHandler> typeHandler = typeHandlers.stream()
                .filter(th -> th.isMatch(resultContext))
                .findFirst();
            if (typeHandler.isPresent()) {
                return typeHandler.get().handleResult(resultContext);
            }
            return jsonTypeHandler.handleResult(resultContext);
        } catch (Exception e) {
            logger.warn("[Dubbo Mock] handle the common result failed, will use unknown type handler.", e);
            return unknownTypeHandler.handleResult(resultContext);
        }
    }
}
