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

import java.lang.reflect.Constructor;

/**
 * @author chenglu
 * @date 2021-08-30 19:25
 */
public class UnknownTypeHandler implements TypeHandler<Object> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return false;
    }

    @Override
    public Object handleResult(ResultContext resultContext) {
        try {
            Class<?> targetType = resultContext.getTargetType();
            Constructor<?>[] constructors = targetType.getDeclaredConstructors();
            for (int i = 0; i < constructors.length; i++) {
                try {
                    Constructor<?> constructor = constructors[i];
                    constructor.setAccessible(true);
                    return constructor.newInstance(resultContext.getData());
                } catch (Exception ignore) {
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
