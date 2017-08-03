package org.littleneko.example;

import com.github.luohaha.client.LightCommClient;
import com.github.luohaha.param.ClientParam;

import java.io.IOException;

public class PaxosKVClient {
    public static void main(String[] args) {
        ClientParam param = new ClientParam();
        param.setOnConnect(conn -> {
            new Thread(() -> {
                KVMessage kvMessage1 = new KVMessage(KVMessage.TypeEnum.PUT, "key1", "value1");
                KVMessage kvMessage2 = new KVMessage(KVMessage.TypeEnum.PUT, "key2", "value2");
                KVMessage kvMessage3 = new KVMessage(KVMessage.TypeEnum.PUT, "key3", "value3");
                try {
                    conn.write(kvMessage1.getJson().getBytes());
                    conn.write(kvMessage2.getJson().getBytes());
                    conn.write(kvMessage3.getJson().getBytes());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }).start();
        });
        param.setOnRead((conn, msg) -> System.out.println("Receive " + new String(msg)));
        param.setOnClose(conn -> System.out.println("Server close!"));


        try {
            LightCommClient client = new LightCommClient(4);
            client.connect("127.0.0.1", 1201, param);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
