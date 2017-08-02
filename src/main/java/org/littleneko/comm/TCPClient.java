package org.littleneko.comm;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.littleneko.message.TransMsg;
import org.littleneko.message.TransMsgDecoder;
import org.littleneko.message.TransMsgEncoder;
import org.littleneko.node.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Client 用于发送数据
 * Created by little on 2017-06-14.
 */
public class TCPClient {
    protected static final Logger logger = LoggerFactory.getLogger(TCPClient.class);

    private static final int CONNECT_RETRY_DELAY = 5;
    private static final int MAX_MSG_QUEUE_SIZE = 10;

    // 保存server的信息
    private ConcurrentHashMap<Integer, NodeInfo> nodeInfoMap;

    // 保存Channel的信息
    private ConcurrentHashMap<Integer, Channel> channelMap;

    // 保存channel和Node的映射
    private ConcurrentHashMap<ChannelId, NodeInfo> channelNodeMap;

    // 保存待发送数据的map
    //private ConcurrentHashMap<Integer, BlockingQueue<TransMsg>> dataQueueMap;

    // netty相关
    private EventLoopGroup group;
    private Bootstrap bootstrap;

    /**
     * 初始化
     *
     */
    public void init() {
        nodeInfoMap = new ConcurrentHashMap<>();
        channelMap = new ConcurrentHashMap<>();
        channelNodeMap = new ConcurrentHashMap<>();
        //dataQueueMap = new ConcurrentHashMap<>();

        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new TransMsgEncoder());
                        p.addLast(new TransMsgDecoder());
                        p.addLast(new TCPClientHandler(TCPClient.this));
                    }
                })
                .option(ChannelOption.SO_REUSEADDR, true);
    }

    /**
     * 开启comm client
     */
    public void startClient() {
        for (NodeInfo nodeInfo : nodeInfoMap.values()) {
            doConnect(nodeInfo);
        }
    }

    /**
     * 添加server
     * @param nodeInfos serverInfo的list
     */
    public void addNodeInfo(List<NodeInfo> nodeInfos) {
        // 保存server信息
        for (NodeInfo nodeInfo : nodeInfos) {
            nodeInfoMap.put(nodeInfo.getNodeID(), nodeInfo);
            //dataQueueMap.put(serverInfo.getServerId(), new ArrayBlockingQueue<>(MAX_MSG_QUEUE_SIZE));
        }
    }

    /**
     * 更新server list， 如果server id不存在则添加，存在则更新
     *
     * @param nodeInfo nodeInfo
     */
    public void updateNode(NodeInfo nodeInfo) {
        // 更新服务器信息
        if (nodeInfoMap.containsKey(nodeInfo.getNodeID())) {
            Channel channel = channelMap.get(nodeInfo.getNodeID());
            if (channel != null) {
                channel.close();
            }
        }

        // 连接到服务器
        doConnect(nodeInfo);
        nodeInfoMap.put(nodeInfo.getNodeID(), nodeInfo);
        //dataQueueMap.put(nodeInfo.getServerId(), new ArrayBlockingQueue<TransMsg>(MAX_MSG_QUEUE_SIZE));
    }

    /**
     * 删除一个server
     *
     * @param id
     */
    public void deleteNode(int id) {
        NodeInfo nodeInfo = nodeInfoMap.get(id);
        if (nodeInfo != null) {
            nodeInfoMap.remove(id);
        }

        Channel channel = channelMap.get(id);
        if (channel != null) {
            channel.close();
            channelMap.remove(id);
        }

        //dataQueueMap.remove(id);
    }

    /**
     * 发送数据到指定server，异步
     *
     * @param serverId  server
     * @param transMsg  待发送的消息
     */
    public void sendMsg(int serverId, TransMsg transMsg, MsgSendListener msgSendListener) {
        Channel channel = channelMap.get(serverId);
        if (channel == null || !channel.isActive()) {
            NodeInfo nodeInfo = nodeInfoMap.get(serverId);
            doConnect(nodeInfo);
            return;
        }

        // 添加数据到待发送队列
        //dataQueueMap.get(serverId).add(transMsg);

        ChannelFuture future = channel.writeAndFlush(transMsg);
        future.addListener((future1) -> {
            if (future1.isSuccess()) {
                msgSendListener.onMsgSendRet(MsgSendListener.MsgSendRetEnum.SUCCESS, transMsg.getTransMsgHead().getId());
            } else {
                msgSendListener.onMsgSendRet(MsgSendListener.MsgSendRetEnum.FAIL, transMsg.getTransMsgHead().getId());
            }
        });
    }

    /**
     * 根据channel id获取对应的server信息
     *
     * @param channelId channelId
     * @return ServerInfo 或者 null
     */
    public NodeInfo getServerByChannel(ChannelId channelId) {
        NodeInfo nodeInfo = channelNodeMap.get(channelId);
        return nodeInfo;
    }

    /**
     * 连接到server
     *
     * @param nodeInfo nodeInfo
     */
    public int doConnect(NodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return -1;
        }

        Channel channel = channelMap.get(nodeInfo.getNodeID());
        if (channel != null && channel.isActive()) {
            return -1;
        }

        ChannelFuture future = bootstrap.connect(nodeInfo.getNodeIP(), nodeInfo.getNodePort());
        future.addListener((ChannelFutureListener) (futureListener) -> {
            if (futureListener.isSuccess()) {
                Channel ch = futureListener.channel();
                channelMap.put(nodeInfo.getNodeID(), ch);
                channelNodeMap.put(ch.id(), nodeInfo);

                logger.info("Connect to server {} successfully!", nodeInfo);
            } else {
                logger.info("Failed to connect to server {}, try connect after 10s", nodeInfo);
                futureListener.channel().eventLoop().schedule(() -> doConnect(nodeInfo), CONNECT_RETRY_DELAY, TimeUnit.SECONDS);
            }
        });
        return 0;
    }
}
