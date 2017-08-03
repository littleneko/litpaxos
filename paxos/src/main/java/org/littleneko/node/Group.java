package org.littleneko.node;

import org.littleneko.comm.MsgTransport;
import org.littleneko.core.Committer;
import org.littleneko.core.InstanceManager;

/**
 * Created by little on 2017-06-14.
 */
public class Group {
    private int groupId;

    private InstanceManager InstanceManager;
    private Committer committer;

    public Group(int groupId, MsgTransport msgTransport, NodeInfo curNode, int allNodeCount, GroupSMInfo groupSMInfo) {
        this.committer = new Committer();
        this.groupId = groupId;
        this.InstanceManager = new InstanceManager(msgTransport, curNode, committer, groupSMInfo, allNodeCount, groupId);
        InstanceManager.startInstance();
    }

    public int getGroupId() {
        return groupId;
    }

    public InstanceManager getInstanceManager() {
        return InstanceManager;
    }

    public Committer getCommitter() {
        return committer;
    }
}
