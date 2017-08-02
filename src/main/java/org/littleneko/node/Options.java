package org.littleneko.node;

import org.littleneko.sm.StateMachine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Options {
    private Map<Integer, NodeInfo> nodes = new HashMap<>();
    private int myNodeID;
    private PaxosConf paxosConf;
    private List<GroupSMInfo> groupSMInfos;
    private int groupCount;

    public Options(int groupCount) {
        this.groupCount = groupCount;
        groupSMInfos = new ArrayList<>(groupCount);
    }

    /**
     * @param nodeInfo
     */
    public void addNode(NodeInfo nodeInfo) {
        nodes.put(nodeInfo.getNodeID(), nodeInfo);
    }

    /**
     * @param id
     * @param ip
     * @param port
     */
    public void addNode(int id, String ip, int port) {
        NodeInfo nodeInfo = new NodeInfo(id, ip, port);
        nodes.put(id, nodeInfo);
    }

    public void addGroupSMInfo(GroupSMInfo groupSMInfo) {
        this.groupSMInfos.add(groupSMInfo);
    }

    public GroupSMInfo getGroupSMInfo(int idx) {
        if (idx <0 || idx >= groupSMInfos.size()) {
            return null;
        }
        return groupSMInfos.get(idx);
    }

    public Map<Integer, NodeInfo> getNodes() {
        return nodes;
    }

    public int getMyNodeID() {
        return myNodeID;
    }

    public void setMyNodeID(int myNodeID) {
        this.myNodeID = myNodeID;
    }

    public PaxosConf getPaxosConf() {
        return paxosConf;
    }

    public void setPaxosConf(PaxosConf paxosConf) {
        this.paxosConf = paxosConf;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    public void setGroupSMInfos(List<GroupSMInfo> groupSMInfos) {
        this.groupSMInfos = groupSMInfos;
    }

    public void setNodes(Map<Integer, NodeInfo> nodes) {
        this.nodes = nodes;
    }
}
