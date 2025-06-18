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

package org.apache.dubbo.common.serialize.fory;

import org.apache.dubbo.common.serialize.fory.dubbo.ForyObjectInput;
import org.apache.dubbo.common.serialize.fory.dubbo.ForyObjectOutput;
import org.apache.dubbo.common.serialize.model.AnimalEnum;
import org.apache.dubbo.common.serialize.model.person.FullAddress;
import org.apache.fory.Fory;
import org.apache.fory.config.Language;
import org.apache.fory.memory.MemoryBuffer;
import org.apache.fory.memory.MemoryUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ForyObjectOutputTest {
    private ForyObjectOutput foryObjectOutput;
    private ForyObjectInput foryObjectInput;
    private ByteArrayOutputStream byteArrayOutputStream;
    private ByteArrayInputStream byteArrayInputStream;
    Fory fory = Fory.builder().withLanguage(Language.JAVA).build();
    MemoryBuffer buffer = MemoryUtils.buffer(32);

    @BeforeEach
    public void setUp() {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.foryObjectOutput = new ForyObjectOutput(fory, buffer,byteArrayOutputStream);
    }

    @AfterEach
    public void tearDown() {
        new ForyObjectInput(fory,buffer,new ByteArrayInputStream(new byte[]{0}));
    }

    @Test
    public void testWriteBool() throws IOException {
        this.foryObjectOutput.writeBool(false);
        this.flushToInput();

        boolean result = this.foryObjectInput.readBool();
        assertThat(result, is(false));
    }


    @Test
    public void testWriteUTF() throws IOException {
        this.foryObjectOutput.writeUTF("I don’t know 知りません Не знаю");
        this.flushToInput();

        String result = this.foryObjectInput.readUTF();
        assertThat(result, is("I don’t know 知りません Не знаю"));
    }

    @Test
    public void testWriteShort() throws IOException {
        this.foryObjectOutput.writeShort((short) 1);
        this.flushToInput();

        Short result = this.foryObjectInput.readShort();
        assertThat(result, is((short) 1));
    }

    @Test
    public void testWriteLong() throws IOException {
        this.foryObjectOutput.writeLong(12345678L);
        this.flushToInput();

        Long result = this.foryObjectInput.readLong();
        assertThat(result, is(12345678L));
    }

    @Test
    public void testWriteDouble() throws IOException {
        this.foryObjectOutput.writeDouble(-1.66d);
        this.flushToInput();

        Double result = this.foryObjectInput.readDouble();
        assertThat(result, is(-1.66d));
    }


    @Test
    public void testWriteInt() throws IOException {
        this.foryObjectOutput.writeInt(1);
        this.flushToInput();

        Integer result = this.foryObjectInput.readInt();
        assertThat(result, is(1));
    }

    @Test
    public void testWriteByte() throws IOException {
        this.foryObjectOutput.writeByte((byte) 222);
        this.flushToInput();

        Byte result = this.foryObjectInput.readByte();
        assertThat(result, is(((byte) 222)));
    }

    @Test
    public void testWriteBytesWithSubLength() throws IOException {
        this.foryObjectOutput.writeBytes("who are you".getBytes(), 4, 3);
        this.flushToInput();

        byte[] result = this.foryObjectInput.readBytes();
        assertThat(result, is("are".getBytes()));
    }

    @Test
    public void testWriteBytes() throws IOException {
        this.foryObjectOutput.writeBytes("who are you".getBytes());
        this.flushToInput();

        byte[] result = this.foryObjectInput.readBytes();
        assertThat(result, is("who are you".getBytes()));
    }

    @Test
    public void testWriteFloat() throws IOException {
        this.foryObjectOutput.writeFloat(-666.66f);
        this.flushToInput();

        Float result = this.foryObjectInput.readFloat();
        assertThat(result, is(-666.66f));
    }

    @Test
    public void testWriteNullBytesWithSubLength() throws IOException {
        Assertions.assertThrows(NullPointerException.class, () -> {
            this.foryObjectOutput.writeBytes(null, 4, 3);
            this.flushToInput();
            this.foryObjectInput.readBytes();
        });

    }

    @Test
    public void testWriteNullBytes() throws IOException {
        Assertions.assertThrows(NullPointerException.class, () -> {
            this.foryObjectOutput.writeBytes(null);
            this.flushToInput();
            this.foryObjectInput.readBytes();
        });
    }

    @Test
    public void testWriteObject() throws IOException, ClassNotFoundException {
        fory.register(FullAddress.class);
        FullAddress fullAddress = new FullAddress("cId", "pN", "cityId", "Nan Long Street", "51000");
        this.foryObjectOutput.writeObject(fullAddress);
        this.flushToInput();

        FullAddress result = this.foryObjectInput.readObject(FullAddress.class);
        assertThat(result, is(fullAddress));
    }

    @Test
    public void testWriteEnum() throws IOException, ClassNotFoundException {
        fory.register(AnimalEnum.class);
        this.foryObjectOutput.writeObject(AnimalEnum.cat);
        this.flushToInput();

        AnimalEnum animalEnum = (AnimalEnum) this.foryObjectInput.readObject();
        assertThat(animalEnum, is(AnimalEnum.cat));
    }

    private void flushToInput() throws IOException {
        this.foryObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.foryObjectInput = new ForyObjectInput(fory,buffer,byteArrayInputStream);
    }
}
