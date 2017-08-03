package org.littleneko.node;

import org.littleneko.sm.StateMachine;

import java.util.ArrayList;
import java.util.List;

public class GroupSMInfo {
    private int groupIdx;
    private List<StateMachine> smList = new ArrayList<>();

    public GroupSMInfo(int groupIdx) {
        this.groupIdx = groupIdx;
    }

    public void addSM(StateMachine stateMachine) {
        smList.add(stateMachine);
    }

    public void setGroupIdx(int groupIdx) {
        this.groupIdx = groupIdx;
    }

    public int getGroupIdx() {
        return groupIdx;
    }

    public List<StateMachine> getSmList() {
        return smList;
    }
}