package org.littleneko.message;

import com.google.gson.annotations.SerializedName;

/**
 * Created by little on 2017-06-16.
 */
public class PrepareReplayMsg extends BasePaxosMsg {
    // 是否接受该prepare
    @SerializedName("ok")
    private boolean ok;

    // 回显proposer提议的编号
    @SerializedName("proposal_id")
    private int proposalID;

    // ab, 当前已accept的最大提议的编号
    @SerializedName("max_accept_proposal_id")
    private int maxAcceptProposalID;

    // 当前已accept的最大提议编号的Node ID
    @SerializedName("max_accept_proposal_node_id")
    private int maxAcceptProposalNodeID;

    // av, 当前已accept的最大提议编号的值
    @SerializedName("max_accept_proposal_dec")
    private String maxAcceptProposalValue;

    // 被reject时，acceptor已经promise的最大编号
    @SerializedName("reject_by_proposal_id")
    private int rejectByProposalID;

    public int getMaxAcceptProposalID() {
        return maxAcceptProposalID;
    }

    public void setMaxAcceptProposalID(int maxAcceptProposalID) {
        this.maxAcceptProposalID = maxAcceptProposalID;
    }

    public int getMaxAcceptProposalNodeID() {
        return maxAcceptProposalNodeID;
    }

    public void setMaxAcceptProposalNodeID(int maxAcceptProposalNodeID) {
        this.maxAcceptProposalNodeID = maxAcceptProposalNodeID;
    }

    public String getMaxAcceptProposalValue() {
        return maxAcceptProposalValue;
    }

    public void setMaxAcceptProposalValue(String maxAcceptProposalValue) {
        this.maxAcceptProposalValue = maxAcceptProposalValue;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getProposalID() {
        return proposalID;
    }

    public void setProposalID(int proposalID) {
        this.proposalID = proposalID;
    }

    public int getRejectByProposalID() {
        return rejectByProposalID;
    }

    public void setRejectByProposalID(int rejectByProposalID) {
        this.rejectByProposalID = rejectByProposalID;
    }

    @Override
    public String toString() {
        return "PrepareReplayMsg{" +
                "ok=" + ok +
                ", proposalID=" + proposalID +
                ", maxAcceptProposalID=" + maxAcceptProposalID +
                ", maxAcceptProposalNodeID=" + maxAcceptProposalNodeID +
                ", maxAcceptProposalValue='" + maxAcceptProposalValue + '\'' +
                ", rejectByProposalID=" + rejectByProposalID +
                '}';
    }
}
