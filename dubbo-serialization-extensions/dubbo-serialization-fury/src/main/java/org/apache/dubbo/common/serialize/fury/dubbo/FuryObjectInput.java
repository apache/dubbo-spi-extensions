package org.apache.dubbo.common.serialize.fury.dubbo;

import io.fury.Fury;
import io.fury.memory.MemoryBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.apache.dubbo.common.serialize.ObjectInput;

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
    return fury.deserializeJavaObjectAndClass(input);
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
    return buffer.get(0);
  }

  @Override
  public short readShort() throws IOException {
    readBytes(buffer.getHeapMemory(), 2);
    return buffer.getShort(0);
  }

  @Override
  public int readInt() throws IOException {
    readBytes(buffer.getHeapMemory(), 4);
    return buffer.getInt(0);
  }

  @Override
  public long readLong() throws IOException {
    readBytes(buffer.getHeapMemory(), 8);
    return buffer.getLong(0);
  }

  @Override
  public float readFloat() throws IOException {
    readBytes(buffer.getHeapMemory(), 4);
    return buffer.getFloat(0);
  }

  @Override
  public double readDouble() throws IOException {
    readBytes(buffer.getHeapMemory(), 8);
    return buffer.getDouble(0);
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
