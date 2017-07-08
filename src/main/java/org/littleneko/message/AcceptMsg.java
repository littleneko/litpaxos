package org.littleneko.message;

import com.google.gson.annotations.SerializedName;

/**
 * Created by little on 2017-06-16.
 */
public class AcceptMsg extends BasePaxosMsg {
    // 提议的编号
    @SerializedName("proposal_id")
    private int proposalID;

    // 提议的值
    @SerializedName("proposal_dec")
    private String proposalDec;

    public int getProposalID() {
        return proposalID;
    }

    public void setProposalID(int proposalID) {
        this.proposalID = proposalID;
    }

    public String getProposalDec() {
        return proposalDec;
    }

    public void setProposalDec(String proposalDec) {
        this.proposalDec = proposalDec;
    }

    @Override
    public String toString() {
        return "AcceptMsg{" +
                "proposalID=" + proposalID +
                ", proposalDec='" + proposalDec + '\'' +
                '}';
    }
}
