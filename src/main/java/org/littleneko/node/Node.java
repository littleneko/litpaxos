package org.littleneko.node;

import org.littleneko.comm.MsgReceiver;
import org.littleneko.comm.MsgReceiverImplByTCP;
import org.littleneko.comm.MsgTransport;
import org.littleneko.comm.MsgTransportImplByTCP;
import org.littleneko.message.BasePaxosMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by little on 2017-06-14.
 */
public class Node {
    protected static final Logger logger = LoggerFactory.getLogger(Node.class);

    private int nodeId;

    // group
    private Map<Integer, Group> groupMap;

    private MsgTransport msgTransport;

    private MsgReceiver msgReceiver;

    private List<NodeInfo> nodeInfos;

    public void init(int myNodeID, Map<Integer, NodeInfo> allNode, int groupCount) {
        this.nodeId = myNodeID;
        this.nodeInfos = new ArrayList<>();
        allNode.values().forEach((v) -> nodeInfos.add(v));

        int allNodeCount = nodeInfos.size();
        NodeInfo myNodeInfo = allNode.get(myNodeID);

        msgTransport = new MsgTransportImplByTCP(nodeInfos);

        this.groupMap = new HashMap<>(groupCount);
        IntStream.range(1, groupCount).forEach((i) -> groupMap.put(i, new Group(i, msgTransport, myNodeInfo, allNodeCount)));

        msgReceiver = new MsgReceiverImplByTCP(myNodeInfo.getNodeIP(), myNodeInfo.getNodePort());
    }

    public void commit(int groupId, String value) {
        Group group = groupMap.get(groupId);
    }

    public void onReciveMessage(BasePaxosMsg paxosMsg) {
        int groupID = paxosMsg.getGroupID();
        if (groupMap.containsKey(groupID)) {
            groupMap.get(groupID).getInstance().recvPaxosMsg(paxosMsg);
        }
    }
}
