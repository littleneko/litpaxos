package org.littleneko.node;

import org.littleneko.comm.*;
import org.littleneko.message.BasePaxosMsg;
import org.littleneko.message.PaxosMsgTypeEnum;
import org.littleneko.message.PaxosMsgUtil;
import org.littleneko.message.TransMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
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

    private class MsgRecvListenerImpl implements MsgRecvListener {
        @Override
        public void onMsgRecv(PaxosMsgTypeEnum paxosMsgTypeEnum, TransMsg transMsg) {
            try {
                String str = new String(transMsg.getPaxosMsg(), "UTF-8");
                BasePaxosMsg paxosMsg = PaxosMsgUtil.getPaxosMsg(paxosMsgTypeEnum, str);
                onReciveMessage(paxosMsg);
            } catch (UnsupportedEncodingException e) {
                //e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
    }

    private int nodeId;
    // group
    private Map<Integer, Group> groupMap;

    private MsgTransport msgTransport;
    private MsgReceiver msgReceiver;

    private List<NodeInfo> nodeInfos;

    public void runNode(Options options) {
        this.nodeId = options.getMyNodeID();
        this.nodeInfos = new ArrayList<>();
        options.getNodes().values().forEach((v) -> nodeInfos.add(v));

        int allNodeCount = nodeInfos.size();
        NodeInfo myNodeInfo = options.getNodes().get(this.nodeId);

        msgTransport = new MsgTransportImplByTCP(nodeInfos);

        this.groupMap = new HashMap<>(options.getGroupCount());
        IntStream.range(0, options.getGroupCount()).forEach((i) -> groupMap.put(i, new Group(i, msgTransport, myNodeInfo, allNodeCount, options.getGroupSMInfo(i))));

        msgReceiver = new MsgReceiverImplByTCP(myNodeInfo.getNodeIP(), myNodeInfo.getNodePort());
        msgReceiver.addMsgReceiverListener(new MsgRecvListenerImpl());
        msgReceiver.startServer();
    }

    /**
     * Client 提交一个值
     *
     * @param groupId
     * @param value
     */
    public void commit(int groupId, String value) {
        Group group = groupMap.get(groupId);
        group.getCommitter().newCommitValue(value);
    }

    /**
     * 把消息交给对应的Group
     * @param paxosMsg
     */
    private void onReciveMessage(BasePaxosMsg paxosMsg) {
        int groupID = paxosMsg.getGroupID();
        if (groupMap.containsKey(groupID)) {
            groupMap.get(groupID).getInstanceManager().recvPaxosMsg(paxosMsg);
        } else {
            logger.warn("Mismatch groupID {}", groupID);
        }
    }
}
