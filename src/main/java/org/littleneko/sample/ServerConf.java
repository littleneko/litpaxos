package org.littleneko.sample;

import org.littleneko.node.PaxosConf;

public class ServerConf {
    // Paxos相关的参数
    private PaxosConf paxosConf;
    // 监听客户端请求的IP和端口
    private String requestIP;
    private int requestPort;

    public PaxosConf getPaxosConf() {
        return paxosConf;
    }

    public void setPaxosConf(PaxosConf paxosConf) {
        this.paxosConf = paxosConf;
    }

    public String getRequestIP() {
        return requestIP;
    }

    public void setRequestIP(String requestIP) {
        this.requestIP = requestIP;
    }

    public int getRequestPort() {
        return requestPort;
    }

    public void setRequestPort(int requestPort) {
        this.requestPort = requestPort;
    }

    @Override
    public String toString() {
        return "ServerConf{" +
                "paxosConf=" + paxosConf +
                ", requestIP='" + requestIP + '\'' +
                ", requestPort=" + requestPort +
                '}';
    }
}
