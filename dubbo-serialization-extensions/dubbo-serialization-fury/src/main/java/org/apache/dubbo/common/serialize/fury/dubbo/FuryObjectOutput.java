package org.apache.dubbo.common.serialize.fury.dubbo;

import io.fury.Fury;
import io.fury.memory.MemoryBuffer;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.dubbo.common.serialize.ObjectOutput;

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
    fury.serializeJavaObjectAndClass(output, obj);
  }

  public void writeBool(boolean v) throws IOException {
    buffer.unsafePutBoolean(0, v);
    output.write(buffer.getHeapMemory(), 0, 1);
  }

  public void writeByte(byte v) throws IOException {
    buffer.unsafePut(0, v);
    output.write(buffer.getHeapMemory(), 0, 1);
  }

  public void writeShort(short v) throws IOException {
    buffer.unsafePutShort(0, v);
    output.write(buffer.getHeapMemory(), 0, 2);
  }

  public void writeInt(int v) throws IOException {
    buffer.unsafePutInt(0, v);
    output.write(buffer.getHeapMemory(), 0, 4);
  }

  public void writeLong(long v) throws IOException {
    buffer.unsafePutLong(0, v);
    output.write(buffer.getHeapMemory(), 0, 8);
  }

  public void writeFloat(float v) throws IOException {
    buffer.unsafePutFloat(0, v);
    output.write(buffer.getHeapMemory(), 0, 4);
  }

  public void writeDouble(double v) throws IOException {
    buffer.unsafePutDouble(0, v);
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
