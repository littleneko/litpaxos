package org.littleneko.logstorage;

import com.google.gson.annotations.SerializedName;

/**
 * Created by little on 2017-06-25.
 */
public class AcceptorStateData {
    @SerializedName("instance_id")
    private int instanceID;

    @SerializedName("promise_id")
    private int promiseID;
    @SerializedName("promise_node_id")
    private int promiseNodeID;

    @SerializedName("accepted_id")
    private int acceptedID;
    @SerializedName("accepted_node_id")
    private int acceptedNodeID;

    @SerializedName("accepted_value")
    private String acceptedValue;

    public int getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(int instanceID) {
        this.instanceID = instanceID;
    }

    public int getPromiseID() {
        return promiseID;
    }

    public void setPromiseID(int promiseID) {
        this.promiseID = promiseID;
    }

    public int getPromiseNodeID() {
        return promiseNodeID;
    }

    public void setPromiseNodeID(int promiseNodeID) {
        this.promiseNodeID = promiseNodeID;
    }

    public int getAcceptedID() {
        return acceptedID;
    }

    public void setAcceptedID(int acceptedID) {
        this.acceptedID = acceptedID;
    }

    public int getAcceptedNodeID() {
        return acceptedNodeID;
    }

    public void setAcceptedNodeID(int acceptedNodeID) {
        this.acceptedNodeID = acceptedNodeID;
    }

    public String getAcceptedValue() {
        return acceptedValue;
    }

    public void setAcceptedValue(String acceptedValue) {
        this.acceptedValue = acceptedValue;
    }

    @Override
    public String toString() {
        return "AcceptorStateData{" +
                "instanceID=" + instanceID +
                ", promiseID=" + promiseID +
                ", promiseNodeID=" + promiseNodeID +
                ", acceptedID=" + acceptedID +
                ", acceptedNodeID=" + acceptedNodeID +
                ", acceptedValue='" + acceptedValue + '\'' +
                '}';
    }
}
