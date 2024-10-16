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

import org.apache.fury.Fury;
import org.apache.fury.config.Language;
import org.apache.fury.memory.MemoryBuffer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FuryObjectInputTest {

    private Fury fury;
    private MemoryBuffer buffer;
    private InputStream inputStream;

    @BeforeEach
    public void setUp() {
        fury = Fury.builder().withLanguage(Language.JAVA).build();
        buffer = MemoryBuffer.newHeapBuffer(1024); // Initialize with an arbitrary size
    }

    @Test
    public void testReadBool() throws Exception {
        byte[] inputBytes = new byte[]{1};
        inputStream = new ByteArrayInputStream(inputBytes);
        FuryObjectInput objectInput = new FuryObjectInput(fury, buffer, inputStream);

        boolean result = objectInput.readBool();
        assertTrue(result);
    }

    @Test
    public void testReadByte() throws Exception {
        byte[] inputBytes = new byte[]{0x1F};
        inputStream = new ByteArrayInputStream(inputBytes);
        FuryObjectInput objectInput = new FuryObjectInput(fury, buffer, inputStream);

        byte result = objectInput.readByte();
        assertEquals(0x1F, result);
    }

    @Test
    public void testReadInt() throws Exception {
        byte[] inputBytes = new byte[]{42, 0, 0, 0};
        inputStream = new ByteArrayInputStream(inputBytes);
        FuryObjectInput objectInput = new FuryObjectInput(fury, buffer, inputStream);

        int result = objectInput.readInt();
        assertEquals(42, result);
    }

    @Test
    public void testReadShort() throws Exception {
        byte[] inputBytes = new byte[]{42, 0};
        inputStream = new ByteArrayInputStream(inputBytes);
        FuryObjectInput objectInput = new FuryObjectInput(fury, buffer, inputStream);

        short result = objectInput.readShort();
        assertEquals(42, result);
    }


    @Test
    public void testReadLong() throws Exception {
        byte[] inputBytes = new byte[]{42, 0, 0, 0, 0, 0, 0, 0};
        inputStream = new ByteArrayInputStream(inputBytes);
        FuryObjectInput objectInput = new FuryObjectInput(fury, buffer, inputStream);

        long result = objectInput.readLong();
        assertEquals(42L, result);
    }

    @Test
    public void testReadBytes() throws Exception {
        byte[] inputBytes = new byte[]{4, 0, 0, 0, 10, 20, 30, 40};
        inputStream = new ByteArrayInputStream(inputBytes);
        FuryObjectInput objectInput = new FuryObjectInput(fury, buffer, inputStream);

        byte[] result = objectInput.readBytes();
        assertArrayEquals(new byte[]{10, 20, 30, 40}, result);
    }
}
