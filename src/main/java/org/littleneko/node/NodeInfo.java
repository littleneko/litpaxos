package org.littleneko.node;

import com.google.gson.annotations.SerializedName;

/**
 * Created by little on 2017-06-15.
 */
public class NodeInfo {
    @SerializedName("id")
    private int nodeID;
    @SerializedName("ip")
    private String nodeIP;
    @SerializedName("port")
    private int nodePort;

    public NodeInfo(int nodeID, String nodeIP, int nodePort) {
        this.nodeID = nodeID;
        this.nodeIP = nodeIP;
        this.nodePort = nodePort;
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
    }

    public String getNodeIP() {
        return nodeIP;
    }

    public void setNodeIP(String nodeIP) {
        this.nodeIP = nodeIP;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "nodeID=" + nodeID +
                ", nodeIP='" + nodeIP + '\'' +
                ", nodePort=" + nodePort +
                '}';
    }
}
