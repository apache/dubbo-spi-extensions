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

import org.apache.dubbo.common.serialize.ObjectOutput;

import org.apache.fury.Fury;
import org.apache.fury.io.BlockedStreamUtils;
import org.apache.fury.memory.MemoryBuffer;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fury.Fury;
import org.apache.fury.memory.MemoryBuffer;

/**
 * Fury implementation for {@link ObjectOutput}.
 *
 * @author chaokunyang
 */
public class FuryObjectOutput implements ObjectOutput {
  private final Fury fury;
  private final MemoryBuffer buffer;
  private final OutputStream output;

  public FuryObjectOutput(Fury fury, MemoryBuffer buffer, OutputStream output) {
    this.fury = fury;
    this.buffer = buffer;
    this.output = output;
  }

  public void writeObject(Object obj) {
      BlockedStreamUtils.serialize(fury, output, obj);
  }

  public void writeBool(boolean v) throws IOException {
    buffer.putBoolean(0, v);
    output.write(buffer.getHeapMemory(), 0, 1);
  }

  public void writeByte(byte v) throws IOException {
    buffer.putByte(0, v);
    output.write(buffer.getHeapMemory(), 0, 1);
  }

  public void writeShort(short v) throws IOException {
    buffer.putInt16(0, v);
    output.write(buffer.getHeapMemory(), 0, 2);
  }

  public void writeInt(int v) throws IOException {
    buffer.putInt32(0, v);
    output.write(buffer.getHeapMemory(), 0, 4);
  }

  public void writeLong(long v) throws IOException {
    buffer.putInt64(0, v);
    output.write(buffer.getHeapMemory(), 0, 8);
  }

  public void writeFloat(float v) throws IOException {
    buffer.putFloat32(0, v);
    output.write(buffer.getHeapMemory(), 0, 4);
  }

  public void writeDouble(double v) throws IOException {
    buffer.putFloat64(0, v);
    output.write(buffer.getHeapMemory(), 0, 8);
  }

  public void writeUTF(String v) throws IOException {
    // avoid `writeInt` overwrite sting data.
    buffer.writerIndex(4);
    if (v != null) {
      buffer.writeBoolean(true);
      fury.writeJavaString(buffer, v);
    } else {
      buffer.writeBoolean(false);
    }
    int size = buffer.writerIndex() - 4;
    writeInt(size);
    output.write(buffer.getHeapMemory(), 4, size);
  }

  public void writeBytes(byte[] v) throws IOException {
    writeInt(v.length);
    output.write(v);
  }

  public void writeBytes(byte[] v, int off, int len) throws IOException {
    writeInt(len);
    output.write(v, off, len);
  }

  public void flushBuffer() throws IOException {
    output.flush();
  }
}
