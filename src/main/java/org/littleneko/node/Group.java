package org.littleneko.node;

import org.littleneko.comm.MsgTransport;
import org.littleneko.core.Instance;

/**
 * Created by little on 2017-06-14.
 */
public class Group {
    private int groupId;

    private Instance instance;

    public Group(int groupId, MsgTransport msgTransport, NodeInfo curNode, int allNodeCount) {
        this.groupId = groupId;
        instance = new Instance(msgTransport, curNode, groupId, allNodeCount);
    }

    public int getGroupId() {
        return groupId;
    }
}
