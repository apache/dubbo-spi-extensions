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

import org.apache.dubbo.common.serialize.model.Organization;
import org.apache.dubbo.common.serialize.model.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JacksonObjectInput} Unit Test
 */
public class JacksonObjectInputTest {

    private JacksonObjectInput jacksonObjectInput;

    @Test
    public void testReadBool() throws IOException {
        jacksonObjectInput = new JacksonObjectInput(new ByteArrayInputStream("true".getBytes()));
        boolean result = jacksonObjectInput.readBool();

        assertThat(result, is(true));

        jacksonObjectInput = new JacksonObjectInput(new StringReader("false"));
        result = jacksonObjectInput.readBool();

        assertThat(result, is(false));
    }

    @Test
    public void testReadByte() throws IOException {
        jacksonObjectInput = new JacksonObjectInput(new ByteArrayInputStream("123".getBytes()));
        Byte result = jacksonObjectInput.readByte();

        assertThat(result, is(Byte.parseByte("123")));
    }

    @Test
    public void testReadBytes() throws IOException {
        jacksonObjectInput = new JacksonObjectInput(new ByteArrayInputStream("123456".getBytes()));
        byte[] result = jacksonObjectInput.readBytes();

        assertThat(result, is("123456".getBytes()));
    }

    @Test
    public void testReadShort() throws IOException {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("1"));
        short result = jacksonObjectInput.readShort();

        assertThat(result, is((short) 1));
    }

    @Test
    public void testReadInt() throws IOException {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("1"));
        Integer result = jacksonObjectInput.readInt();

        assertThat(result, is(1));
    }

    @Test
    public void testReadDouble() throws IOException {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("1.88"));
        Double result = jacksonObjectInput.readDouble();

        assertThat(result, is(1.88d));
    }

    @Test
    public void testReadLong() throws IOException {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("10"));
        Long result = jacksonObjectInput.readLong();

        assertThat(result, is(10L));
    }

    @Test
    public void testReadFloat() throws IOException {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("1.66"));
        Float result = jacksonObjectInput.readFloat();

        assertThat(result, is(1.66F));
    }

    @Test
    public void testReadUTF() throws IOException {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("\"wording\""));
        String result = jacksonObjectInput.readUTF();

        assertThat(result, is("wording"));
    }

    @Test
    public void testReadObject() throws IOException, ClassNotFoundException {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("{ \"name\":\"John\", \"age\":30 }"));
        Person result = jacksonObjectInput.readObject(Person.class);

        assertThat(result, not(nullValue()));
        assertThat(result.getName(), is("John"));
        assertThat(result.getAge(), is(30));
    }

    @Test
    public void testEmptyLine() throws IOException, ClassNotFoundException {
        Assertions.assertThrows(EOFException.class, () -> {
            jacksonObjectInput = new JacksonObjectInput(new StringReader(""));

            jacksonObjectInput.readObject();
        });
    }

    @Test
    public void testEmptySpace() throws IOException, ClassNotFoundException {
        Assertions.assertThrows(EOFException.class, () -> {
            jacksonObjectInput = new JacksonObjectInput(new StringReader("  "));

            jacksonObjectInput.readObject();
        });
    }

    @Test
    public void testReadObjectWithoutClass() throws IOException, ClassNotFoundException, NoSuchFieldException {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("{ \"name\":\"John\", \"age\":30 }"));

        Map map = jacksonObjectInput.readObject(Map.class);

        assertThat(map, not(nullValue()));
        assertThat(map.get("name"), is("John"));
        assertThat(map.get("age"), is(30));
    }


    @Test
    public void testReadObjectWithTowType() throws Exception {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("[{\"name\":\"John\",\"age\":30},{\"name\":\"Born\",\"age\":24}]"));

        Method methodReturnType = getClass().getMethod("towLayer");
        Type type = methodReturnType.getGenericReturnType();
        List<Person> o = jacksonObjectInput.readObject(List.class, type);

        assertTrue(o instanceof List);
        assertTrue(o.get(0) instanceof Person);

        assertThat(o.size(), is(2));
        assertThat(o.get(1).getName(), is("Born"));
    }

    @Test
    public void testReadObjectWithThreeType() throws Exception {
        jacksonObjectInput = new JacksonObjectInput(new StringReader("{\"data\":[{\"name\":\"John\",\"age\":30},{\"name\":\"Born\",\"age\":24}]}"));

        Method methodReturnType = getClass().getMethod("threeLayer");
        Type type = methodReturnType.getGenericReturnType();
        Organization<List<Person>> o = jacksonObjectInput.readObject(Organization.class, type);

        assertTrue(o instanceof Organization);
        assertTrue(o.getData() instanceof List);
        assertTrue(o.getData().get(0) instanceof Person);

        assertThat(o.getData().size(), is(2));
        assertThat(o.getData().get(1).getName(), is("Born"));
    }

    public List<Person> towLayer() {
        return null;
    }

    public Organization<List<Person>> threeLayer() {
        return null;
    }

}
