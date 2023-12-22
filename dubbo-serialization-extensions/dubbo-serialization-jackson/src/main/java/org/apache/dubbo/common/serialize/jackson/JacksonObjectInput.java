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
package org.apache.dubbo.common.serialize.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.dubbo.common.serialize.DefaultJsonDataInput;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * Jackson object input implementation
 */
public class JacksonObjectInput implements DefaultJsonDataInput {

    private final ObjectMapper MAPPER;

    private final BufferedReader READER;

    public JacksonObjectInput(InputStream inputStream) {
        this(new InputStreamReader(inputStream));
    }

    public JacksonObjectInput(Reader reader) {
        this.READER = new BufferedReader(reader);
        this.MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule())
        ;
    }

    @Override
    public Object readObject() throws IOException {
        return readObject(Object.class);
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException {
        String json = readLine();
        return MAPPER.readValue(json, cls);
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        String json = readLine();
        return MAPPER.readValue(json, new TypeReference<T>() {
            @Override
            public Type getType() {
                return type;
            }
        });
    }

    @Override
    public byte[] readBytes() throws IOException {
        return readLine().getBytes();
    }

    private String readLine() throws IOException {
        String line = READER.readLine();
        if (line == null || line.trim().isEmpty()) {
            throw new EOFException();
        }
        return line;
    }

}
