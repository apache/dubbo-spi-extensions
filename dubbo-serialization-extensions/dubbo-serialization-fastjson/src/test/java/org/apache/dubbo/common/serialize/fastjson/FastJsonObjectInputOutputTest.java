package org.apache.dubbo.common.serialize.fastjson;

import com.example.test.TestPojo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class FastJsonObjectInputOutputTest {
    private FastJsonObjectOutput fastJsonObjectOutput;
    private FastJsonObjectInput fastJsonObjectInput;

    private PipedOutputStream pos;
    private PipedInputStream pis;


    /**
     * Sets up the test environment by initializing PipedInputStream,
     * PipedOutputStream, and connecting them before each test.
     *
     * @throws IOException if there's an issue initializing the streams.
     */
    @BeforeEach
    public void setup() throws IOException {
        pis = new PipedInputStream();
        pos = new PipedOutputStream();
        pis.connect(pos);

        fastJsonObjectOutput = new FastJsonObjectOutput(pos);
        fastJsonObjectInput = new FastJsonObjectInput(pis);
    }

    /**
     * Cleans up the test environment by closing PipedInputStream and
     * PipedOutputStream after each test.
     *
     * @throws IOException if there's an issue closing the streams.
     */
    @AfterEach
    public void clean() throws IOException {
        if (pos != null) {
            pos.close();
        }
        if (pis != null) {
            pis.close();
        }
    }

    @Test
    public void testWriteReadString() throws IOException {
        String testData = "Test String";
        fastJsonObjectOutput.writeUTF(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        String result = fastJsonObjectInput.readObject(String.class);
        Assertions.assertEquals(testData, result);
    }

    @Test
    public void testWriteReadBool() throws IOException {
        fastJsonObjectOutput.writeBool(true);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        boolean result = fastJsonObjectInput.readObject(Boolean.class);
        Assertions.assertTrue(result);
    }

    @Test
    public void testWriteReadByte() throws IOException {
        byte testData = 42;
        fastJsonObjectOutput.writeByte(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        Byte result = fastJsonObjectInput.readObject(Byte.class);
        Assertions.assertEquals(testData, result);
    }

    @Test
    public void testWriteReadBytes() throws IOException {
        byte[] testData = "example".getBytes();
        fastJsonObjectOutput.writeBytes(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        byte[] result = fastJsonObjectInput.readBytes();
        Assertions.assertArrayEquals(testData, result);
    }

    @Test
    public void testWriteReadShort() throws IOException {
        short testData = 32767;
        fastJsonObjectOutput.writeShort(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        Short result = fastJsonObjectInput.readObject(Short.class);
        Assertions.assertEquals(testData, result);
    }

    @Test
    public void testWriteReadInt() throws IOException {
        int testData = 123456;
        fastJsonObjectOutput.writeInt(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        Integer result = fastJsonObjectInput.readObject(Integer.class);
        Assertions.assertEquals(testData, result);
    }

    @Test
    public void testWriteReadLong() throws IOException {
        long testData = 123456789L;
        fastJsonObjectOutput.writeLong(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        Long result = fastJsonObjectInput.readObject(Long.class);
        Assertions.assertEquals(testData, result);
    }

    @Test
    public void testWriteReadFloat() throws IOException {
        float testData = 3.14f;
        fastJsonObjectOutput.writeFloat(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        Float result = fastJsonObjectInput.readObject(Float.class);
        Assertions.assertEquals(testData, result, 0.0001f);
    }

    @Test
    public void testWriteReadDouble() throws IOException {
        double testData = 3.14159;
        fastJsonObjectOutput.writeDouble(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        Double result = fastJsonObjectInput.readObject(Double.class);
        Assertions.assertEquals(testData, result, 0.000001);
    }

    @Test
    public void testWriteReadUTF() throws IOException {
        String testData = "Hello, World!";
        fastJsonObjectOutput.writeUTF(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        String result = fastJsonObjectInput.readObject(String.class);
        Assertions.assertEquals(testData, result);
    }

    @Test
    public void testWriteReadObject() throws IOException {
        TestPojo testData = new TestPojo("Alice");
        fastJsonObjectOutput.writeObject(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        TestPojo result = fastJsonObjectInput.readObject(TestPojo.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(testData.getData(), result.getData());
    }

    @Test
    public void testWriteReadObjectWithoutClass() throws IOException {

        TestPojo testData = new TestPojo("Bob");
        fastJsonObjectOutput.writeObject(testData);
        fastJsonObjectOutput.flushBuffer();
        pos.close();

        Object result = fastJsonObjectInput.readObject();
        Assertions.assertNotNull(result);

        Assertions.assertEquals("Bob", ((TestPojo) result).getData());
    }
}

