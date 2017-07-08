package org.littleneko.message;

import com.google.gson.annotations.SerializedName;

/**
 * Created by little on 2017-06-16.
 */
public class AcceptReplayMsg extends BasePaxosMsg {
    // 提示是否被接受
    @SerializedName("ok")
    private boolean ok;

    // 回显proposer提议的编号
    @SerializedName("proposal_id")
    private int proposalID;

    // 被reject时，acceptor已经promise的最大编号
    @SerializedName("reject_by_proposal_id")
    private int rejectByProposalID;

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
        return "AcceptReplayMsg{" +
                "ok=" + ok +
                ", proposalID=" + proposalID +
                ", rejectByProposalID=" + rejectByProposalID +
                '}';
    }
}
