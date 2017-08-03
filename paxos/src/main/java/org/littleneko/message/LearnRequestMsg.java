package org.littleneko.message;

import com.google.gson.annotations.SerializedName;

/**
 * Created by little on 2017-06-23.
 */
public class LearnRequestMsg extends BasePaxosMsg {
    // 该节点当前的instance id
    @SerializedName("cur_instance_id")
    private int curInstanceID;

    public int getCurInstanceID() {
        return curInstanceID;
    }

    public void setCurInstanceID(int curInstanceID) {
        this.curInstanceID = curInstanceID;
    }

    @Override
    public String toString() {
        return "LearnRequestMsg{" +
                "curInstanceID=" + curInstanceID +
                '}';
    }
}
