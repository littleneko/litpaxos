package org.littleneko.sample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class PaxosKVClient {
    public static void main(String[] args) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(true);
            sc.connect(new InetSocketAddress("127.0.0.1", 1201));

            ByteBuffer buf = ByteBuffer.allocate(1024);
            KVMessage kvMessage1 = new KVMessage(KVMessage.TypeEnum.PUT, "key1", "value1");
            buf.put(kvMessage1.getJson().getBytes());
            buf.flip();
            sc.write(buf);

            buf.clear();
            KVMessage kvMessage2 = new KVMessage(KVMessage.TypeEnum.PUT, "key2", "value2");
            buf.put(kvMessage2.getJson().getBytes());
            buf.flip();
            sc.write(buf);

            buf.clear();
            KVMessage kvMessage3 = new KVMessage(KVMessage.TypeEnum.PUT, "key3", "value3");
            buf.put(kvMessage3.getJson().getBytes());
            buf.flip();
            sc.write(buf);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
