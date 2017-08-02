package org.littleneko.node;

import org.littleneko.comm.MsgTransport;
import org.littleneko.core.Committer;
import org.littleneko.core.Instance;
import org.littleneko.sm.StateMachine;

import java.util.List;
import java.util.Map;

/**
 * Created by little on 2017-06-14.
 */
public class Group {
    private int groupId;

    private Instance instance;
    private Committer committer;

    public Group(int groupId, MsgTransport msgTransport, NodeInfo curNode, int allNodeCount, GroupSMInfo groupSMInfo) {
        this.committer = new Committer();
        this.groupId = groupId;
        this.instance = new Instance(msgTransport, curNode, committer, groupSMInfo, allNodeCount, groupId);
        instance.startInstance();
    }

    public int getGroupId() {
        return groupId;
    }

    public Instance getInstance() {
        return instance;
    }

    public Committer getCommitter() {
        return committer;
    }
}
