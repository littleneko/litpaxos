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
        if (this.proposalID != ballotNumber.proposalID) {
            return this.proposalID - ballotNumber.proposalID;
        } else {
            return this.proposalNodeID = ballotNumber.proposalNodeID;
        }
    }

    /**
     * proposalID和proposalNodeID 两个作为唯一提案编号
     *
     * @param ballotNumber
     * @return
     */
    public boolean equals(BallotNumber ballotNumber) {
        return this.proposalID == ballotNumber.proposalID && this.proposalNodeID == ballotNumber.proposalNodeID;
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
