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

package org.apache.dubbo.common.serialize.protostuff;

import org.apache.dubbo.common.serialize.base.AbstractSerializationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtostuffSerializationTest extends AbstractSerializationTest {
    {
        serialization = new ProtostuffSerialization();
    }

    @Test
    public void testReadFakeObject() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ProtostuffObjectOutput output = new ProtostuffObjectOutput(bos);
        int fakeLength = 1024*1000*2000;
        output.writeInt(fakeLength);
        output.writeInt(fakeLength);
        output.flushBuffer();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ProtostuffObjectInput inputProtostuff = new ProtostuffObjectInput(bis);
        try {
            inputProtostuff.readObject();
        } catch (Exception e) {
            assertTrue(e instanceof IOException);
            return;
        }
        Assertions.fail("notHere");
    }

    @Test
    public void testReadRealObjectOut() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ProtostuffObjectOutput output = new ProtostuffObjectOutput(bos);
        int objLength = 1000*2000;
        byte[] arr = new byte[objLength];
        output.writeObject(arr);
        output.flushBuffer();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ProtostuffObjectInput inputProtostuff = new ProtostuffObjectInput(bis);
        Object o = inputProtostuff.readObject();
        Assertions.assertEquals(Arrays.hashCode(arr), Arrays.hashCode((byte []) o));

    }
}
