package org.littleneko.comm;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.littleneko.message.TransMsgDecoder;
import org.littleneko.message.TransMsgEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by little on 2017-06-14.
 */
public class TCPServer {
    protected static final Logger logger = LoggerFactory.getLogger(TCPServer.class);

    private static final int BIND_RETRY_DELAY = 5;

    // 本机监听的IP和端口
    private String listenIP;
    private int listenPort;

    // netty相关
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap serverBootstrap;

    // 处理接收到的消息的handler
    private MsgRecvListener msgRecvListener;

    /**
     * 初始化commServer
     */
    public void init() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new TransMsgEncoder())
                                .addLast(new TransMsgDecoder())
                                .addLast(new TCPServerHandler(TCPServer.this));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    /**
     * start Server
     */
    public void startCommServer() {
        doBind();
    }

    /**
     * bind
     */
    public void doBind() {
        ChannelFuture f = serverBootstrap.bind(listenIP, listenPort);
        f.addListener((ChannelFutureListener) (future) -> {
            if (future.isSuccess()) {
                //channel = f.channel();
                logger.info("Start commServer success");

                // close future
                ChannelFuture f2 = f.channel().closeFuture();
                f2.addListener((ChannelFutureListener) (future2) -> {
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                    logger.info("Exit server....");
                });
            } else {
                logger.error("Start commServer failed, retry");
                future.channel().eventLoop().schedule(() -> doBind(), BIND_RETRY_DELAY, TimeUnit.SECONDS);
            }
        });
    }

    public void setListenIP(String listenIP) {
        this.listenIP = listenIP;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public MsgRecvListener getMsgRecvListener() {
        return msgRecvListener;
    }

    public void setMsgRecvListener(MsgRecvListener msgRecvListener) {
        this.msgRecvListener = msgRecvListener;
    }
}
