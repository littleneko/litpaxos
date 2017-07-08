package org.littleneko.node;

import org.littleneko.comm.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by little on 2017-06-14.
 */
public class Node {
    private int nodeId;

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

    }
}
