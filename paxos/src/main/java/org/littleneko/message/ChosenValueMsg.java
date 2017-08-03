package org.littleneko.message;

import com.google.gson.annotations.SerializedName;

/**
 * accepter发送chosen value的实体
 * Created by little on 2017-06-26.
 */
public class ChosenValueMsg extends BasePaxosMsg {
    // 当前接受的proposal ID
    @SerializedName("prposal_id")
    private int acceptedProposalID;

    @SerializedName("chosen_value")
    private String value;

    public int getAcceptedProposalID() {
        return acceptedProposalID;
    }

    public void setAcceptedProposalID(int acceptedProposalID) {
        this.acceptedProposalID = acceptedProposalID;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return super.toString() +
                "ChosenValueMsg{" +
                "acceptedProposalID=" + acceptedProposalID +
                ", value='" + value + '\'' +
                '}';
    }
}
