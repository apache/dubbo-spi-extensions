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

package org.apache.dubbo.common.serialize.fury.dubbo;

import org.apache.dubbo.common.serialize.ObjectInput;

import org.apache.fury.Fury;
import org.apache.fury.io.BlockedStreamUtils;
import org.apache.fury.memory.MemoryBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

@SuppressWarnings("unchecked")
public class FuryObjectInput implements ObjectInput {
    private final Fury fury;
    private final MemoryBuffer buffer;
    private final InputStream input;

    public FuryObjectInput(Fury fury, MemoryBuffer buffer, InputStream input) {
        this.fury = fury;
        this.buffer = buffer;
        this.input = input;
    }

    @Override
    public Object readObject() {
        return BlockedStreamUtils.deserialize(fury, input);
    }

    @Override
    public <T> T readObject(Class<T> cls) {
        return (T) readObject();
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) {
        return (T) readObject();
    }

    @Override
    public boolean readBool() throws IOException {
        readBytes(buffer.getHeapMemory(), 1);
        return buffer.getBoolean(0);
    }

    @Override
    public byte readByte() throws IOException {
        readBytes(buffer.getHeapMemory(), 1);
        return buffer.getByte(0);
    }

    @Override
    public short readShort() throws IOException {
        readBytes(buffer.getHeapMemory(), 2);
        return buffer.getInt16(0);
    }

    @Override
    public int readInt() throws IOException {
        readBytes(buffer.getHeapMemory(), 4);
        return buffer.getInt32(0);
    }

    @Override
    public long readLong() throws IOException {
        readBytes(buffer.getHeapMemory(), 8);
        return buffer.getInt64(0);
    }

    @Override
    public float readFloat() throws IOException {
        readBytes(buffer.getHeapMemory(), 4);
        return buffer.getFloat32(0);
    }

    @Override
    public double readDouble() throws IOException {
        readBytes(buffer.getHeapMemory(), 8);
        return buffer.getFloat64(0);
    }

    @Override
    public String readUTF() throws IOException {
        int size = readInt();
        buffer.readerIndex(0);
        buffer.ensure(size);
        readBytes(buffer.getHeapMemory(), size);
        if (buffer.readBoolean()) {
            return fury.readJavaString(buffer);
        } else {
            return null;
        }
    }

    @Override
    public byte[] readBytes() throws IOException {
        int size = readInt();
        byte[] bytes = new byte[size];
        readBytes(bytes, size);
        return bytes;
    }

    private void readBytes(byte[] bytes, int size) throws IOException {
        int off = 0;
        while (off != size) {
            off += input.read(bytes, off, size - off);
        }
    }
}
