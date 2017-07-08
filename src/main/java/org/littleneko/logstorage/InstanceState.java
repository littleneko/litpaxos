package org.littleneko.logstorage;

import com.google.gson.annotations.SerializedName;

/**
 * Created by little on 2017-06-27.
 */
public class InstanceState {
    @SerializedName("instance_id")
    private int instanceID;

    @SerializedName("value")
    private String value;

    public int getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(int instanceID) {
        this.instanceID = instanceID;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "InstanceState{" +
                "instanceID=" + instanceID +
                ", value='" + value + '\'' +
                '}';
    }
}
