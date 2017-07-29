package org.littleneko.node;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class PaxosConf {
    @SerializedName("nodes")
    private Map<Integer, NodeInfo> nodes;

    @SerializedName("myID")
    private int myNodeID;

    // 用于Proposer的超时定时器, 毫秒
    @SerializedName("commTimeOut")
    private int commTimeOut;
    // Learner的学习时间间隔，毫秒
    @SerializedName("learnInterval")
    private int learnInterval;
    // 日志持久化存储的位置
    @SerializedName("logFile")
    private String logFile;

    public PaxosConf() {
        nodes = new HashMap<>();
    }

    /**
     *
     * @param nodeInfo
     */
    public void addNode(NodeInfo nodeInfo) {
        nodes.put(nodeInfo.getNodeID(), nodeInfo);
    }

    /**
     *
     * @param id
     * @param ip
     * @param port
     */
    public void addNode(int id, String ip, int port) {
        NodeInfo nodeInfo = new NodeInfo(id, ip, port);
        nodes.put(id, nodeInfo);
    }

    public Map<Integer, NodeInfo> getNodes() {
        return nodes;
    }

    public void setNodes(Map<Integer, NodeInfo> nodes) {
        this.nodes = nodes;
    }

    public int getMyNodeID() {
        return myNodeID;
    }

    public void setMyNodeID(int myNodeID) {
        this.myNodeID = myNodeID;
    }

    public int getCommTimeOut() {
        return commTimeOut;
    }

    public void setCommTimeOut(int commTimeOut) {
        this.commTimeOut = commTimeOut;
    }

    public int getLearnInterval() {
        return learnInterval;
    }

    public void setLearnInterval(int learnInterval) {
        this.learnInterval = learnInterval;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    @Override
    public String toString() {
        return "PaxosConf{" +
                "nodes=" + nodes +
                ", myNodeID=" + myNodeID +
                ", commTimeOut=" + commTimeOut +
                ", learnInterval=" + learnInterval +
                ", logFile='" + logFile + '\'' +
                '}';
    }
}
