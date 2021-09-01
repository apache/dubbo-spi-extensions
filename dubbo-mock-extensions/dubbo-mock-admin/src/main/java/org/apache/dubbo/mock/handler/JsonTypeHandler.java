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

import org.apache.dubbo.mock.exception.HandleFailException;

import com.google.gson.Gson;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:46
 */
public class JsonTypeHandler implements TypeHandler<Object> {

    private Gson gson;

    public JsonTypeHandler() {
        gson = new Gson();
    }

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return true;
    }

    @Override
    public Object handleResult(ResultContext resultContext) {
        try {
            Class<?> targetType = resultContext.getTargetType();
            Class<?> serviceType = Class.forName(resultContext.getServiceName());
            Method[] methods = serviceType.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (!Objects.equals(method.getName(), resultContext.getMethodName())) {
                    continue;
                }
                // for generic type parse
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    return gson.fromJson(resultContext.getData(), genericReturnType);
                }
            }
            return gson.fromJson(resultContext.getData(), targetType);
        } catch (Exception e) {
            throw new HandleFailException(e);
        }

    }
}
