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

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;

import org.apache.dubbo.common.serialize.protobuf.support.wrapper.MapValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * ProtobufAttachmentUtils
 */
public class ProtobufAttachmentUtils {
    private static Map<String, BuiltinMarshaller> marshallers = new HashMap<>();
    private final static String NULL_CLASS_NAME = "null";

    static {
        marshaller(String.class, new StringMarshaller());
        marshaller(Integer.class, new IntegerMarshaller());
        marshaller(Long.class, new LongMarshaller());
        marshaller(Boolean.class, new BooleanMarshaller());
        marshaller(Float.class, new FloatMarshaller());
        marshaller(Double.class, new DoubleMarshaller());
        marshallers.put(NULL_CLASS_NAME, new NullMarshaller());
    }

    static void marshaller(Class<?> clazz, BuiltinMarshaller marshaller) {
        marshallers.put(clazz.getCanonicalName(), marshaller);
    }

    static MapValue.Map wrap(Map<String, Object> attachments) throws IOException {
        Map<String, Any> genericAttachments = new HashMap<>(attachments.size());
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            genericAttachments.put(entry.getKey(), marshal(entry.getValue()));
        }
        return MapValue.Map.newBuilder().putAllAttachmentsV2(genericAttachments).build();
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

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ProtobufUtils.serialize(StringValue.newBuilder().setValue(className).build(), stream);
        marshaller.marshal(obj, stream);
        stream.flush();
        return Any.newBuilder().setValue(ByteString.copyFrom(stream.toByteArray())).build();
    }

    private static Object unmarshal(Any any) throws InvalidProtocolBufferException {
        InputStream stream = new ByteArrayInputStream(any.getValue().toByteArray());
        String className = ProtobufUtils.deserialize(stream, StringValue.class).getValue();
        BuiltinMarshaller marshaller = marshallers.get(className);
        return marshaller.unmarshal(stream);
    }

    private static interface BuiltinMarshaller<T> {
        void marshal(T obj, OutputStream stream) throws IOException;

        T unmarshal(InputStream stream) throws InvalidProtocolBufferException;
    }

    static class StringMarshaller implements BuiltinMarshaller<String> {
        @Override
        public void marshal(String obj, OutputStream stream) throws IOException {
            ProtobufUtils.serialize(StringValue.newBuilder().setValue(obj).build(), stream);
        }

        @Override
        public String unmarshal(InputStream stream) throws InvalidProtocolBufferException {
            return ProtobufUtils.deserialize(stream, StringValue.class).getValue();
        }
    }

    static class IntegerMarshaller implements BuiltinMarshaller<Integer> {
        @Override
        public void marshal(Integer obj, OutputStream stream) throws IOException {
            ProtobufUtils.serialize(Int32Value.newBuilder().setValue(obj).build(), stream);
        }

        @Override
        public Integer unmarshal(InputStream stream) throws InvalidProtocolBufferException {
            return ProtobufUtils.deserialize(stream, Int32Value.class).getValue();
        }
    }

    static class LongMarshaller implements BuiltinMarshaller<Long> {
        @Override
        public void marshal(Long obj, OutputStream stream) throws IOException {
            ProtobufUtils.serialize(Int64Value.newBuilder().setValue(obj).build(), stream);
        }

        @Override
        public Long unmarshal(InputStream stream) throws InvalidProtocolBufferException {
            return ProtobufUtils.deserialize(stream, Int64Value.class).getValue();
        }
    }

    static class BooleanMarshaller implements BuiltinMarshaller<Boolean> {
        @Override
        public void marshal(Boolean obj, OutputStream stream) throws IOException {
            ProtobufUtils.serialize(BoolValue.newBuilder().setValue(obj).build(), stream);
        }

        @Override
        public Boolean unmarshal(InputStream stream) throws InvalidProtocolBufferException {
            return ProtobufUtils.deserialize(stream, BoolValue.class).getValue();
        }
    }

    static class FloatMarshaller implements BuiltinMarshaller<Float> {
        @Override
        public void marshal(Float obj, OutputStream stream) throws IOException {
            ProtobufUtils.serialize(FloatValue.newBuilder().setValue(obj).build(), stream);
        }

        @Override
        public Float unmarshal(InputStream stream) throws InvalidProtocolBufferException {
            return ProtobufUtils.deserialize(stream, FloatValue.class).getValue();
        }
    }

    static class DoubleMarshaller implements BuiltinMarshaller<Double> {
        @Override
        public void marshal(Double obj, OutputStream stream) throws IOException {
            ProtobufUtils.serialize(DoubleValue.newBuilder().setValue(obj).build(), stream);
        }

        @Override
        public Double unmarshal(InputStream stream) throws InvalidProtocolBufferException {
            return ProtobufUtils.deserialize(stream, DoubleValue.class).getValue();
        }
    }

    static class NullMarshaller implements BuiltinMarshaller<Object> {

        @Override
        public void marshal(Object obj, OutputStream stream) throws IOException {
            ProtobufUtils.serialize(Empty.newBuilder().build(), stream);
        }

        @Override
        public Object unmarshal(InputStream stream) throws InvalidProtocolBufferException {
            return null;
        }
    }
}
