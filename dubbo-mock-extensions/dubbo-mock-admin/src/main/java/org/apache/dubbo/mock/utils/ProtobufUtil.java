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

package org.apache.dubbo.mock.utils;

import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

import java.util.Objects;

/**
 * ProtobufUtil works on protobuf object and class solve.
 */
public class ProtobufUtil {

    private ProtobufUtil() {
    }

    public static boolean isProtobufClass(Class<?> targetType) {
        if (Objects.isNull(targetType)) {
            return false;
        }
        Class<?> superType = targetType.getSuperclass();
        if (Objects.isNull(superType)) {
            return false;
        }
        String superTypeName = superType.getName();
        return Objects.equals(superTypeName, "com.google.protobuf.GeneratedMessageV3")
            || Objects.equals(superTypeName, "com.google.protobuf.GeneratedMessage");
    }

    public static String protobufToJson(Object o) {
        try {
            return JsonFormat.printToString((Message) o);
        } catch (Exception e) {
            return o.toString();
        }
    }
}
