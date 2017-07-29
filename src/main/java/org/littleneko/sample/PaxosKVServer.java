package org.littleneko.sample;

import com.google.gson.Gson;
import org.littleneko.node.Node;
import org.littleneko.sm.StateMachine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PaxosKVServer {
    private Selector selector;
    private Node node = new Node();

    public void initServe(String ip, int port) throws IOException {
        selector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(ip, port));
        ssc.configureBlocking(false);
        ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void startServer() {
        try {
            while (true) {
                int readyChannels = selector.select();
                if (readyChannels == 0)
                    continue;
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    iterator.remove();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        System.out.println("Accept");
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel client = ssc.accept();
        client.configureBlocking(false);
        client.register(key.selector(), SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) throws IOException {
        System.out.println("Read");
        SocketChannel sc = (SocketChannel) key.channel();

        StringBuilder sb = new StringBuilder();

        ByteBuffer buff = ByteBuffer.allocate(1024);
        int buffRead = sc.read(buff);
        while (buffRead > 0) {
            buff.flip();
            String receiveText = new String(buff.array(), 0, buffRead);
            sb.append(receiveText);
            buff.clear();
            buffRead = sc.read(buff);
        }

        String recvJson = sb.toString();
        System.out.printf(recvJson);
        this.node.commit(0, recvJson);

        sc.register(selector, SelectionKey.OP_READ);
    }

    public static void main(String[] args) {
        PaxosKVServer server = new PaxosKVServer();

        // Read configure file
        String confFile = System.getProperty("user.dir") + "/sample_conf/" + "server1.json";
        Gson gson = new Gson();
        ServerConf serverConf = gson.fromJson(FileUtils.readFromFile(confFile), ServerConf.class);

        // Init State Machine
        Map<Integer, Map<Integer, StateMachine>> sms = new HashMap<>();
        Map<Integer, StateMachine> sm = new HashMap<>();
        sm.put(1, new KVStateMachine());
        sms.put(1, sm);

        // Init Node
        server.node.init(serverConf.getPaxosConf(), sms, 1);

        // Init Server, start accept client request
        try {
            server.initServe(serverConf.getRequestIP(), serverConf.getRequestPort());
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}