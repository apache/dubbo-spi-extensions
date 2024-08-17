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

package org.apache.dubbo.common.serialize.fury;

import org.apache.dubbo.common.serialize.fury.dubbo.FuryObjectInput;
import org.apache.dubbo.common.serialize.fury.dubbo.FuryObjectOutput;
import org.apache.dubbo.common.serialize.model.AnimalEnum;
import org.apache.dubbo.common.serialize.model.person.FullAddress;
import org.apache.fury.Fury;
import org.apache.fury.config.Language;
import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.memory.MemoryUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FuryObjectOutputTest {
    private FuryObjectOutput furyObjectOutput;
    private FuryObjectInput furyObjectInput;
    private ByteArrayOutputStream byteArrayOutputStream;
    private ByteArrayInputStream byteArrayInputStream;
    Fury fury = Fury.builder().withLanguage(Language.JAVA).build();
    MemoryBuffer buffer = MemoryUtils.buffer(32);

    @BeforeEach
    public void setUp() {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.furyObjectOutput = new FuryObjectOutput(fury, buffer,byteArrayOutputStream);
    }

    @AfterEach
    public void tearDown() {
        new FuryObjectInput(fury,buffer,new ByteArrayInputStream(new byte[]{0}));
    }

    @Test
    public void testWriteBool() throws IOException {
        this.furyObjectOutput.writeBool(false);
        this.flushToInput();

        boolean result = this.furyObjectInput.readBool();
        assertThat(result, is(false));
    }


    @Test
    public void testWriteUTF() throws IOException {
        this.furyObjectOutput.writeUTF("I don’t know 知りません Не знаю");
        this.flushToInput();

        String result = this.furyObjectInput.readUTF();
        assertThat(result, is("I don’t know 知りません Не знаю"));
    }

    @Test
    public void testWriteShort() throws IOException {
        this.furyObjectOutput.writeShort((short) 1);
        this.flushToInput();

        Short result = this.furyObjectInput.readShort();
        assertThat(result, is((short) 1));
    }

    @Test
    public void testWriteLong() throws IOException {
        this.furyObjectOutput.writeLong(12345678L);
        this.flushToInput();

        Long result = this.furyObjectInput.readLong();
        assertThat(result, is(12345678L));
    }

    @Test
    public void testWriteDouble() throws IOException {
        this.furyObjectOutput.writeDouble(-1.66d);
        this.flushToInput();

        Double result = this.furyObjectInput.readDouble();
        assertThat(result, is(-1.66d));
    }


    @Test
    public void testWriteInt() throws IOException {
        this.furyObjectOutput.writeInt(1);
        this.flushToInput();

        Integer result = this.furyObjectInput.readInt();
        assertThat(result, is(1));
    }

    @Test
    public void testWriteByte() throws IOException {
        this.furyObjectOutput.writeByte((byte) 222);
        this.flushToInput();

        Byte result = this.furyObjectInput.readByte();
        assertThat(result, is(((byte) 222)));
    }

    @Test
    public void testWriteBytesWithSubLength() throws IOException {
        this.furyObjectOutput.writeBytes("who are you".getBytes(), 4, 3);
        this.flushToInput();

        byte[] result = this.furyObjectInput.readBytes();
        assertThat(result, is("are".getBytes()));
    }

    @Test
    public void testWriteBytes() throws IOException {
        this.furyObjectOutput.writeBytes("who are you".getBytes());
        this.flushToInput();

        byte[] result = this.furyObjectInput.readBytes();
        assertThat(result, is("who are you".getBytes()));
    }

    @Test
    public void testWriteFloat() throws IOException {
        this.furyObjectOutput.writeFloat(-666.66f);
        this.flushToInput();

        Float result = this.furyObjectInput.readFloat();
        assertThat(result, is(-666.66f));
    }

    @Test
    public void testWriteNullBytesWithSubLength() throws IOException {
        Assertions.assertThrows(NullPointerException.class, () -> {
            this.furyObjectOutput.writeBytes(null, 4, 3);
            this.flushToInput();
            this.furyObjectInput.readBytes();
        });

    }

    @Test
    public void testWriteNullBytes() throws IOException {
        Assertions.assertThrows(NullPointerException.class, () -> {
            this.furyObjectOutput.writeBytes(null);
            this.flushToInput();
            this.furyObjectInput.readBytes();
        });
    }


    @Test
    public void testWriteObject() throws IOException{
        fury.register(FullAddress.class);
        FullAddress fullAddress = new FullAddress("cId", "pN", "cityId", "Nan Long Street", "51000");
        Object result = fury.deserialize(fury.serialize(fullAddress));
        assertThat(result, is(fullAddress));
    }

    @Test
    public void testWriteEnum() throws IOException {
        fury.register(AnimalEnum.class);
        Object animalEnum = fury.deserialize(fury.serialize(AnimalEnum.cat));
        assertThat(animalEnum, is(AnimalEnum.cat));
    }

    private void flushToInput() throws IOException {
        this.furyObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.furyObjectInput = new FuryObjectInput(fury,buffer,byteArrayInputStream);
    }
}
