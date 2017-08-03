package org.littleneko.core;

import java.util.HashSet;
import java.util.Set;

/**
 * 用于保存接收到的信息
 * Created by little on 2017-06-16.
 */
public class MsgCounter {
    private Set<Integer> receivedMsgNodeIDSet;
    private Set<Integer> promiseOrAcceptMsgNodeIDSet;
    private Set<Integer> rejectMsgNodeIDSet;

    private int allNodeCount;
    private int qrmNodeCount;

    public MsgCounter(int allNodeCount) {
        this.allNodeCount = allNodeCount;
        this.qrmNodeCount = allNodeCount / 2 + 1;
        this.receivedMsgNodeIDSet = new HashSet<>();
        this.promiseOrAcceptMsgNodeIDSet = new HashSet<>();
        this.rejectMsgNodeIDSet = new HashSet<>();
    }

    /**
     * 添加一个接收的ID
     * @param nodeID
     */
    public void addReceive(int nodeID) {
        this.receivedMsgNodeIDSet.add(nodeID);
    }

    /**
     * 添加一个拒绝的ID
     * @param nodeID
     */
    public void addReject(int nodeID) {
        this.rejectMsgNodeIDSet.add(nodeID);
    }

    /**
     * 添加一个通过的ID
     * @param nodeID
     */
    public void addPromiseOrAccept(int nodeID) {
        this.promiseOrAcceptMsgNodeIDSet.add(nodeID);
    }

    /**
     * 收到超过半数以上的投票，该轮投票通过
     * @return
     */
    public boolean isPassedOnThisRound() {
        if (promiseOrAcceptMsgNodeIDSet.size() >= qrmNodeCount) {
            return true;
        }
        return false;
    }

    /**
     * 收到半数以上的拒绝
     * @return
     */
    public boolean isRejectedOnThisRound() {
        if (rejectMsgNodeIDSet.size() >= qrmNodeCount) {
            return true;
        }
        return false;
    }

    /**
     * 是否已经收到所有的accepter的回复
     * @return
     */
    public boolean isAllReceiveOnThisRound() {
        if (receivedMsgNodeIDSet.size() == allNodeCount) {
            return true;
        }
        return false;
    }

    /**
     * 清空该轮投票数据
     */
    public void startNewRound() {
        this.receivedMsgNodeIDSet.clear();
        this.promiseOrAcceptMsgNodeIDSet.clear();
        this.rejectMsgNodeIDSet.clear();
    }
}
