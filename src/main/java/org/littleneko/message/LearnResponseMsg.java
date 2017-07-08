package org.littleneko.message;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by little on 2017-06-23.
 */
public class LearnResponseMsg extends BasePaxosMsg {
    @SerializedName("proposal_id")
    private int proposalID;

    @SerializedName("proposal_node_id")
    private int proposalNodeID;

    // learn request请求中的instance id，即values中的起始ID
    @SerializedName("start_instance_id")
    private int startInstanceID;

    @SerializedName("values")
    private Map<Integer, String> values;

    public LearnResponseMsg() {
        values = new HashMap<>();
    }

    public int getProposalID() {
        return proposalID;
    }

    public void addInstanceState(int id, String value) {
        values.put(id, value);
    }

    public Map<Integer, String> getValues() {
        return values;
    }

    public void setProposalID(int proposalID) {
        this.proposalID = proposalID;
    }

    public int getProposalNodeID() {
        return proposalNodeID;
    }

    public void setProposalNodeID(int proposalNodeID) {
        this.proposalNodeID = proposalNodeID;
    }

    public int getStartInstanceID() {
        return startInstanceID;
    }

    public void setStartInstanceID(int startInstanceID) {
        this.startInstanceID = startInstanceID;
    }

    @Override
    public String toString() {
        return "LearnResponseMsg{" +
                "proposalID=" + proposalID +
                ", proposalNodeID=" + proposalNodeID +
                ", startInstanceID=" + startInstanceID +
                ", values=" + values +
                '}';
    }
}
