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
package org.apache.dubbo.serialize.hessian;


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


public class NativeHessianObjectInputOutputTest {
    private Hessian2ObjectInput hessian2ObjectInput;
    private Hessian2ObjectOutput hessian2ObjectOutput;

    private PipedOutputStream pos;
    private PipedInputStream pis;

    @BeforeEach
    public void setup() throws IOException {
        pis = new PipedInputStream();
        pos = new PipedOutputStream();
        pis.connect(pos);

        hessian2ObjectOutput = new Hessian2ObjectOutput(pos);
        hessian2ObjectInput = new Hessian2ObjectInput(pis);
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
        hessian2ObjectOutput.writeBool(true);
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        boolean result = hessian2ObjectInput.readBool();
        assertThat(result, is(true));
    }

    @Test
    public void testWriteReadByte() throws IOException {
        hessian2ObjectOutput.writeByte((byte) 'a');
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        Byte result = hessian2ObjectInput.readByte();

        assertThat(result, is((byte) 'a'));
    }

    @Test
    public void testWriteReadBytes() throws IOException {
        hessian2ObjectOutput.writeBytes("123456".getBytes());
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        byte[] result = hessian2ObjectInput.readBytes();

        assertThat(result, is("123456".getBytes()));
    }

    @Test
    public void testWriteReadShort() throws IOException {
        hessian2ObjectOutput.writeShort((short) 1);
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        short result = hessian2ObjectInput.readShort();

        assertThat(result, is((short) 1));
    }

    @Test
    public void testWriteReadInt() throws IOException {
        hessian2ObjectOutput.writeInt(1);
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        Integer result = hessian2ObjectInput.readInt();

        assertThat(result, is(1));
    }

    @Test
    public void testReadDouble() throws IOException {
        hessian2ObjectOutput.writeDouble(3.14d);
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        Double result = hessian2ObjectInput.readDouble();

        assertThat(result, is(3.14d));
    }

    @Test
    public void testReadLong() throws IOException {
        hessian2ObjectOutput.writeLong(10L);
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        Long result = hessian2ObjectInput.readLong();

        assertThat(result, is(10L));
    }

    @Test
    public void testWriteReadFloat() throws IOException {
        hessian2ObjectOutput.writeFloat(1.66f);
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        Float result = hessian2ObjectInput.readFloat();

        assertThat(result, is(1.66F));
    }

    @Test
    public void testWriteReadUTF() throws IOException {
        hessian2ObjectOutput.writeUTF("wording");
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        String result = hessian2ObjectInput.readUTF();

        assertThat(result, is("wording"));
    }

    @Test
    public void testWriteReadObject() throws IOException, ClassNotFoundException {
        Person p = new Person();
        p.setAge(30);
        p.setName("abc");

        hessian2ObjectOutput.writeObject(p);
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        Person result = hessian2ObjectInput.readObject(Person.class);

        assertThat(result, not(nullValue()));
        assertThat(result.getName(), is("abc"));
        assertThat(result.getAge(), is(30));
    }

    @Test
    public void testWriteReadObjectWithoutClass() throws IOException, ClassNotFoundException {
        Person p = new Person();
        p.setAge(30);
        p.setName("abc");

        hessian2ObjectOutput.writeObject(p);
        hessian2ObjectOutput.flushBuffer();
        pos.close();

        //All the information is lost here
        Object result = hessian2ObjectInput.readObject();

        assertThat(result, not(nullValue()));
//		assertThat(result.getName(), is("abc"));
//		assertThat(result.getAge(), is(30));
    }
}
