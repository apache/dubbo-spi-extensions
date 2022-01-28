package org.smartboot.dubbo.remoting.transport;

import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.buffer.DynamicChannelBuffer;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/28
 */
public class SmartSocketProtocol implements Protocol<Object> {
    private final Codec2 codec;


    public SmartSocketProtocol(Codec2 codec) {
        this.codec = codec;
    }

    @Override
    public Object decode(ByteBuffer readBuffer, AioSession session) {
        if (!readBuffer.hasRemaining()) {
            return null;
        }
        SmartSocketChannel channel = session.getAttachment();
        ChannelBuffer frame = channel.getFrame();
        if (frame.readable()) {
            if (frame instanceof DynamicChannelBuffer) {
                frame.writeBytes(readBuffer);
            } else {
                int size = frame.readableBytes() + readBuffer.remaining();
                ChannelBuffer newFrame = ChannelBuffers.dynamicBuffer(size);
                newFrame.writeBytes(frame, frame.readableBytes());
                newFrame.writeBytes(readBuffer);
                frame = newFrame;
                channel.setFrame(newFrame);
            }
        } else {
//            frame = ChannelBuffers.wrappedBuffer(readBuffer);
            frame = ChannelBuffers.dynamicBuffer(readBuffer.remaining());
            frame.writeBytes(readBuffer);
            channel.setFrame(frame);
            readBuffer.position(readBuffer.position() + readBuffer.remaining());
        }


        Object msg;
        int savedReadIndex;

        savedReadIndex = frame.readerIndex();
        try {
            msg = codec.decode(channel, frame);
        } catch (Exception e) {
            throw new DecoderException(e);
        }
        if (msg == Codec2.DecodeResult.NEED_MORE_INPUT) {
            frame.readerIndex(savedReadIndex);
        } else {
            if (savedReadIndex == frame.readerIndex()) {
                throw new DecoderException("Decode without read data.");
            }
            if (msg != null) {
                if (frame.readable()) {
                    frame.discardReadBytes();
                    //粘包，回退一个字节以唤起下一轮回调
                    frame.writerIndex(frame.writerIndex() - 1);
                    readBuffer.position(readBuffer.position() - 1);
                } else {
                    //释放内存
                    channel.setFrame(ChannelBuffers.EMPTY_BUFFER);
                }
                return msg;
            }
        }
        return null;
    }
}
