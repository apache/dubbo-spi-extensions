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
package org.apache.dubbo.remoting.transport.quic;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.AbstractClient;
import org.apache.dubbo.remoting.utils.UrlUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicClientCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamType;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * NettyClient.
 */
public class QuicNettyClient extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(QuicNettyClient.class);
    /**
     * netty client bootstrap
     */
    private static final EventLoopGroup EVENT_LOOP_GROUP = QuicNettyEventLoopFactory.eventLoopGroup(Constants.DEFAULT_IO_THREADS, "QuicNettyClientWorker");


    private Bootstrap bootstrap;

    /**
     * current channel. Each successful invocation of {@link QuicNettyClient#doConnect()} will
     * replace this with new channel and close old channel.
     * <b>volatile, please copy reference to use.</b>
     */
    private volatile Channel qchannel;

    private volatile Channel channel;

    private volatile Channel schannel;

    /**
     * The constructor of NettyClient.
     * It wil init and start netty.
     */
    public QuicNettyClient(final URL url, final ChannelHandler handler) throws RemotingException {
        // you can customize name and type of client thread pool by THREAD_NAME_KEY and THREADPOOL_KEY in CommonConstants.
        // the handler will be wrapped: MultiMessageHandler->HeartbeatHandler->handler
        super(url, wrapChannelHandler(url, handler));
    }

    /**
     * Init bootstrap
     *
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        QuicSslContext context = QuicSslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).
            applicationProtocols("http/0.9").build();
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        io.netty.channel.ChannelHandler codec = new QuicClientCodecBuilder()
            .sslContext(context)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000)
            .build();

        Bootstrap bs = new Bootstrap();
        qchannel = bs.group(group)
            .channel(NioDatagramChannel.class)
            .handler(codec)
            .bind(0).sync().channel();
        logger.info("quic client do open finish");
    }

    @Override
    protected void doConnect() throws Throwable {
        logger.info("quic client do connect");
        final QuicNettyClientHandler nettyClientHandler = new QuicNettyClientHandler(getUrl(), this);
        InetSocketAddress address = getConnectAddress();
        logger.info("quic connect address:" + address);
        QuicChannel quicChannel = QuicChannel.newBootstrap(qchannel)
            .streamHandler(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) {
                    ctx.close();
                }
            })
            .remoteAddress(address)
            .connect()
            .get(5, TimeUnit.SECONDS);

        this.schannel = quicChannel.createStream(QuicStreamType.BIDIRECTIONAL,
            new ChannelInitializer<QuicStreamChannel>() {
                @Override
                protected void initChannel(QuicStreamChannel quicStreamChannel) throws Exception {

                    NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), QuicNettyClient.this);
                    int heartbeatInterval = UrlUtils.getHeartbeat(getUrl());

                    quicStreamChannel.pipeline().addLast(
                            new ChannelInboundHandlerAdapter() {
                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                    if (evt == ChannelInputShutdownReadComplete.INSTANCE) {
                                        ((QuicChannel) ctx.channel().parent()).close(true, 0,
                                            ctx.alloc().directBuffer(16)
                                                .writeBytes(new byte[]{'k', 't', 'h', 'x', 'b', 'y', 'e'}));
                                    }
                                }
                            }
                        )
                        .addLast("decoder", adapter.getDecoder())
                        .addLast("encoder", adapter.getEncoder())
                        .addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, TimeUnit.MILLISECONDS))
                        .addLast("handler", nettyClientHandler);
                    ;
                }
            }
        ).sync().getNow();
    }

    @Override
    public boolean isConnected() {
        if (this.schannel == null) {
            return false;
        }
        return schannel.isActive();
    }


    @Override
    protected void doDisConnect() throws Throwable {
        try {
            QuicNettyChannel.removeChannelIfDisconnected(channel);
        } catch (Throwable t) {
            logger.warn(t.getMessage());
        }
    }

    @Override
    protected void doClose() throws Throwable {
        // can't shutdown nioEventLoopGroup because the method will be invoked when closing one channel but not a client,
        // but when and how to close the nioEventLoopGroup ?
        // nioEventLoopGroup.shutdownGracefully();
    }

    @Override
    protected org.apache.dubbo.remoting.Channel getChannel() {
        Channel c = this.schannel;
        if (c == null) {
            return null;
        }
        return QuicNettyChannel.getOrAddChannel(c, getUrl(), this);
    }

    Channel getNettyChannel() {
        return schannel;
    }

    @Override
    public boolean canHandleIdle() {
        return true;
    }
}
