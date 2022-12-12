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

import org.apache.dubbo.common.serialize.protobuf.support.wrapper.MapValue;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * ProtobufAttachmentUtils
 */
public class ProtobufAttachmentUtils {
    private static Map<String, BuiltinMarshaller> marshallers = new HashMap<>();
    private final static String NULL_CLASS_NAME = "null";

    private final static JsonFormat.TypeRegistry typeRegistry;

    static {
        marshaller(String.class, new StringMarshaller());
        marshaller(Integer.class, new IntegerMarshaller());
        marshaller(Long.class, new LongMarshaller());
        marshaller(Boolean.class, new BooleanMarshaller());
        marshaller(Float.class, new FloatMarshaller());
        marshaller(Double.class, new DoubleMarshaller());
        marshallers.put(NULL_CLASS_NAME, new NullMarshaller());
        typeRegistry = JsonFormat.TypeRegistry
            .newBuilder()
            .add(StringValue.getDescriptor())
            .add(Int32Value.getDescriptor())
            .add(Int64Value.getDescriptor())
            .add(BoolValue.getDescriptor())
            .add(FloatValue.getDescriptor())
            .add(DoubleValue.getDescriptor())
            .add(Empty.getDescriptor())
            .add(MapValue.Attachment.getDescriptor())
            .build();
    }

    static JsonFormat.TypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    static void marshaller(Class<?> clazz, BuiltinMarshaller marshaller) {
        marshallers.put(clazz.getCanonicalName(), marshaller);
    }

    static MapValue.Map wrap(Map<String, Object> attachments) throws IOException {
        Map<String, Any> genericAttachments = new HashMap<>(attachments.size());
        Map<String, String> stringAttachments = new HashMap<>(attachments.size());
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            genericAttachments.put(entry.getKey(), marshal(entry.getValue()));
            stringAttachments.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return MapValue.Map.newBuilder().putAllAttachmentsV2(genericAttachments).putAllAttachments(stringAttachments).build();
    }

    static Map<String, Object> unwrap(MapValue.Map map) throws InvalidProtocolBufferException {
        Map<String, Object> attachments = new HashMap<>();
        //compatible with older version.
        Map<String, String> stringAttachments = map.getAttachmentsMap();
        if (stringAttachments != null) {
            stringAttachments.forEach((k, v) -> attachments.put(k, v));
        }

        Map<String, Any> genericAttachments = map.getAttachmentsV2Map();
        if (genericAttachments == null) {
            return attachments;
        }
        for (Map.Entry<String, Any> entry : genericAttachments.entrySet()) {
            attachments.put(entry.getKey(), unmarshal(entry.getValue()));
        }
        return attachments;
    }

    private static Any marshal(Object obj) throws IOException {
        String className = NULL_CLASS_NAME;
        if (obj != null) {
            className = obj.getClass().getCanonicalName();
        }
        BuiltinMarshaller marshaller = marshallers.get(className);
        if (marshaller == null) {
            throw new IllegalStateException(className + " in attachment is not supported by protobuf.");
        }

        MapValue.Attachment attachment = MapValue.Attachment.newBuilder()
            .setType(className)
            .setData(marshaller.marshal(obj))
            .build();
        return Any.pack(attachment);
    }

    private static Object unmarshal(Any any) throws InvalidProtocolBufferException {
        MapValue.Attachment attachment = any.unpack(MapValue.Attachment.class);
        String className = attachment.getType();
        BuiltinMarshaller marshaller = marshallers.get(className);
        if (marshaller == null) {
            throw new IllegalStateException(className + " in attachment is not supported by protobuf.");
        }
        return marshaller.unmarshal(attachment.getData());
    }

    private static interface BuiltinMarshaller<T> {
        Any marshal(T obj) throws IOException;

        T unmarshal(Any any) throws InvalidProtocolBufferException;
    }

    static class StringMarshaller implements BuiltinMarshaller<String> {
        @Override
        public Any marshal(String obj) throws IOException {
            return Any.pack(StringValue.newBuilder().setValue(obj).build());
        }

        @Override
        public String unmarshal(Any any) throws InvalidProtocolBufferException {
            return any.unpack(StringValue.class).getValue();
        }
    }

    static class IntegerMarshaller implements BuiltinMarshaller<Integer> {
        @Override
        public Any marshal(Integer obj) throws IOException {
            return Any.pack(Int32Value.newBuilder().setValue(obj).build());
        }

        @Override
        public Integer unmarshal(Any any) throws InvalidProtocolBufferException {
            return any.unpack(Int32Value.class).getValue();
        }

    }

    static class LongMarshaller implements BuiltinMarshaller<Long> {
        @Override
        public Any marshal(Long obj) throws IOException {
            return Any.pack(Int64Value.newBuilder().setValue(obj).build());
        }

        @Override
        public Long unmarshal(Any any) throws InvalidProtocolBufferException {
            return any.unpack(Int64Value.class).getValue();
        }
    }

    static class BooleanMarshaller implements BuiltinMarshaller<Boolean> {
        @Override
        public Any marshal(Boolean obj) throws IOException {
            return Any.pack(BoolValue.newBuilder().setValue(obj).build());
        }

        @Override
        public Boolean unmarshal(Any any) throws InvalidProtocolBufferException {
            return any.unpack(BoolValue.class).getValue();
        }
    }

    static class FloatMarshaller implements BuiltinMarshaller<Float> {
        @Override
        public Any marshal(Float obj) throws IOException {
            return Any.pack(FloatValue.newBuilder().setValue(obj).build());
        }

        @Override
        public Float unmarshal(Any any) throws InvalidProtocolBufferException {
            return any.unpack(FloatValue.class).getValue();
        }
    }

    static class DoubleMarshaller implements BuiltinMarshaller<Double> {
        @Override
        public Any marshal(Double obj) throws IOException {
            return Any.pack(DoubleValue.newBuilder().setValue(obj).build());
        }

        @Override
        public Double unmarshal(Any any) throws InvalidProtocolBufferException {
            return any.unpack(DoubleValue.class).getValue();
        }
    }

    static class NullMarshaller implements BuiltinMarshaller<Object> {

        @Override
        public Any marshal(Object obj) throws IOException {
            return Any.pack(Empty.newBuilder().build());
        }

        @Override
        public Object unmarshal(Any any) throws InvalidProtocolBufferException {
            return null;
        }
    }
}
