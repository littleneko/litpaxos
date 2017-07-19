package org.littleneko.core;

/**
 * 描述Ballot的类
 * Created by little on 2017-06-20.
 */
public class BallotNumber {
    private int proposalID;
    private int proposalNodeID;

    public BallotNumber() {
        this.proposalID = 0;
        this.proposalNodeID = 0;
    }

    public BallotNumber(int proposalID, int proposalNodeID) {
        this.proposalID = proposalID;
        this.proposalNodeID = proposalNodeID;
    }

    /**
     * 比较ballot的值
     *
     * @param ballotNumber
     * @return
     */
    public int compareTo(BallotNumber ballotNumber) {
        return this.proposalID - ballotNumber.getProposalID();
    }

    /**
     * 只判断proposalID是否相等
     *
     * @param ballotNumber
     * @return
     */
    public boolean equals(BallotNumber ballotNumber) {
        return this.compareTo(ballotNumber) == 0;
    }

    public void reset() {
        proposalID = 0;
        proposalNodeID = 0;
    }

    public int getProposalID() {
        return proposalID;
    }

    public int getProposalNodeID() {
        return proposalNodeID;
    }
}
