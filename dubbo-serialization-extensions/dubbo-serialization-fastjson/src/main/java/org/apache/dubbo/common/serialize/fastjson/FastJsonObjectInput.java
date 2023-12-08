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
package org.apache.dubbo.common.serialize.fastjson;

import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import org.apache.dubbo.common.serialize.DefaultJsonDataInput;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.common.utils.ClassUtils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * FastJson object input implementation
 */
public class FastJsonObjectInput implements DefaultJsonDataInput {

    private InputStream is;

    public FastJsonObjectInput(InputStream in) {
        this.is = in;
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException {
        return readObject(cls, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException {
        int length = readLength();
        byte[] bytes = new byte[length];
        int read = is.read(bytes, 0, length);
        if (read != length) {
            throw new IllegalArgumentException(
                    "deserialize failed. expected read length: " + length + " but actual read: " + read);
        }
        ParserConfig parserConfig = new ParserConfig();
        parserConfig.setAutoTypeSupport(true);

        Object result = JSON.parseObject(new String(bytes), cls,
                parserConfig,
                Feature.SupportNonPublicField,
                Feature.SupportAutoType
        );
        if (result != null && cls != null && !ClassUtils.isMatch(result.getClass(), cls)) {
            throw new IllegalArgumentException(
                    "deserialize failed. expected class: " + cls + " but actual class: " + result.getClass());
        }
        return (T) result;

    }

    @Override
    public byte[] readBytes() throws IOException {
        int length = is.read();
        byte[] bytes = new byte[length];
        int read = is.read(bytes, 0, length);
        if (read != length) {
            throw new IllegalArgumentException(
                    "deserialize failed. expected read length: " + length + " but actual read: " + read);
        }
        return bytes;
    }

    private int readLength() throws IOException {
        byte[] bytes = new byte[Integer.BYTES];
        int read = is.read(bytes, 0, Integer.BYTES);
        if (read != Integer.BYTES) {
            throw new IllegalArgumentException(
                    "deserialize failed. expected read length: " + Integer.BYTES + " but actual read: " + read);
        }
        int value = 0;
        for (byte b : bytes) {
            value = (value << 8) + (b & 0xFF);
        }
        return value;
    }
}
