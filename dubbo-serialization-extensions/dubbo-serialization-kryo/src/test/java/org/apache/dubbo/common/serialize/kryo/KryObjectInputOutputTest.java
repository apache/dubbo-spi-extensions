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
package org.apache.dubbo.common.serialize.kryo;


import org.apache.dubbo.common.serialize.model.Person;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;


public class KryObjectInputOutputTest {
    private KryoObjectInput KryoObjectInput;
    private KryoObjectOutput KryoObjectOutput;

    private PipedOutputStream pos;
    private PipedInputStream pis;

    @BeforeEach
    public void setup() throws IOException {
        pis = new PipedInputStream();
        pos = new PipedOutputStream();
        pis.connect(pos);

        KryoObjectOutput = new KryoObjectOutput(pos);
        KryoObjectInput = new KryoObjectInput(pis);
    }

    @AfterEach
    public void clean() throws IOException {
        if (pos != null) {
            pos.close();
            pos = null;
        }
        if (pis != null) {
            pis.close();
            pis = null;
        }
    }

    @Test
    public void testWriteReadBool() throws IOException, InterruptedException {
        KryoObjectOutput.writeBool(true);
        KryoObjectOutput.flushBuffer();
        pos.close();

        boolean result = KryoObjectInput.readBool();
        assertThat(result, is(true));
    }

    @Test
    public void testWriteReadByte() throws IOException {
        KryoObjectOutput.writeByte((byte) 'a');
        KryoObjectOutput.flushBuffer();
        pos.close();

        Byte result = KryoObjectInput.readByte();

        assertThat(result, is((byte) 'a'));
    }

    @Test
    public void testWriteReadBytes() throws IOException {
        KryoObjectOutput.writeBytes("123456".getBytes());
        KryoObjectOutput.flushBuffer();
        pos.close();

        byte[] result = KryoObjectInput.readBytes();

        assertThat(result, is("123456".getBytes()));
    }

    @Test
    public void testWriteReadShort() throws IOException {
        KryoObjectOutput.writeShort((short) 1);
        KryoObjectOutput.flushBuffer();
        pos.close();

        short result = KryoObjectInput.readShort();

        assertThat(result, is((short) 1));
    }

    @Test
    public void testWriteReadInt() throws IOException {
        KryoObjectOutput.writeInt(1);
        KryoObjectOutput.flushBuffer();
        pos.close();

        Integer result = KryoObjectInput.readInt();

        assertThat(result, is(1));
    }

    @Test
    public void testReadDouble() throws IOException {
        KryoObjectOutput.writeDouble(3.14d);
        KryoObjectOutput.flushBuffer();
        pos.close();

        Double result = KryoObjectInput.readDouble();

        assertThat(result, is(3.14d));
    }

    @Test
    public void testReadLong() throws IOException {
        KryoObjectOutput.writeLong(10L);
        KryoObjectOutput.flushBuffer();
        pos.close();

        Long result = KryoObjectInput.readLong();

        assertThat(result, is(10L));
    }

    @Test
    public void testWriteReadFloat() throws IOException {
        KryoObjectOutput.writeFloat(1.66f);
        KryoObjectOutput.flushBuffer();
        pos.close();

        Float result = KryoObjectInput.readFloat();

        assertThat(result, is(1.66F));
    }

    @Test
    public void testWriteReadUTF() throws IOException {
        KryoObjectOutput.writeUTF("wording");
        KryoObjectOutput.flushBuffer();
        pos.close();

        String result = KryoObjectInput.readUTF();

        assertThat(result, is("wording"));
    }

    @Test
    public void testWriteReadObject() throws IOException, ClassNotFoundException {
        Person p = new Person();
        p.setAge(30);
        p.setName("abc");

        KryoObjectOutput.writeObject(p);
        KryoObjectOutput.flushBuffer();
        pos.close();

        Person result = KryoObjectInput.readObject(Person.class);

        assertThat(result, not(nullValue()));
        assertThat(result.getName(), is("abc"));
        assertThat(result.getAge(), is(30));
    }

    @Test
    public void testWriteReadObjectWithoutClass() throws IOException, ClassNotFoundException {
        Person p = new Person();
        p.setAge(30);
        p.setName("abc");

        KryoObjectOutput.writeObject(p);
        KryoObjectOutput.flushBuffer();
        pos.close();

        //All the information is lost here
        Object result = KryoObjectInput.readObject();

        assertThat(result, not(nullValue()));
//		assertThat(result.getName(), is("abc"));
//		assertThat(result.getAge(), is(30));
    }
}
