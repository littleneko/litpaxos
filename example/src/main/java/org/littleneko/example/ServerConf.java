package org.littleneko.example;

import com.google.gson.Gson;
import org.littleneko.node.NodeInfo;
import org.littleneko.node.PaxosConf;

import java.util.HashMap;
import java.util.Map;

public class ServerConf {
    private Map<Integer, NodeInfo> nodes = new HashMap<>();
    private int myNodeID;
    // Paxos相关的参数
    private PaxosConf paxosConf;
    // 监听客户端请求的IP和端口
    private String requestIP;
    private int requestPort;

    public Map<Integer, NodeInfo> getNodes() {
        return nodes;
    }

    public int getMyNodeID() {
        return myNodeID;
    }

    public PaxosConf getPaxosConf() {
        return paxosConf;
    }

    public String getRequestIP() {
        return requestIP;
    }

    public int getRequestPort() {
        return requestPort;
    }

    @Override
    public String toString() {
        return "ServerConf{" +
                "nodes=" + nodes +
                ", myNodeID=" + myNodeID +
                ", paxosConf=" + paxosConf +
                ", requestIP='" + requestIP + '\'' +
                ", requestPort=" + requestPort +
                '}';
    }

    public static void main(String[] args) {
        ServerConf serverConf = new ServerConf();

        NodeInfo node1 = new NodeInfo(1, "127.0.0.1", 1111);
        NodeInfo node2 = new NodeInfo(2, "127.0.0.1", 1111);
        NodeInfo node3 = new NodeInfo(3, "127.0.0.1", 1111);
        Map<Integer, NodeInfo> nodes = new HashMap<>();
        nodes.put(1, node1);
        nodes.put(2, node2);
        nodes.put(3, node3);

        PaxosConf paxosConf = new PaxosConf();
        paxosConf.setCommTimeOut(5);
        paxosConf.setLearnInterval(3);
        paxosConf.setLogFile("./server1.log");

        serverConf.nodes = nodes;
        serverConf.myNodeID = 1;
        serverConf.requestIP = "127.0.0.1";
        serverConf.requestPort = 1201;
        serverConf.paxosConf = paxosConf;

        Gson gson = new Gson();
        String json = gson.toJson(serverConf);
        System.out.printf(json);
    }
}
