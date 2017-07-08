package org.littleneko.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by little on 2017-06-14.
 */
public abstract class BasePaxosMsg {
    @SerializedName("msg_type")
    private PaxosMsgTypeEnum msgType;

    // 发送消息节点的id
    @SerializedName("node_id")
    private int nodeID;

    // 发送消息节点当时的instance ID
    @SerializedName("instance_id")
    private int instanceID;

    @SerializedName("group_id")
    private int groupID;

    @Expose
    private static Gson gson;

    public BasePaxosMsg() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PaxosMsgTypeEnum.class, new PaxosMsgTypeSerializer());
        gson = gsonBuilder.create();
    }

    /**
     * 打包消息为json格式
     *
     * @return json格式的字符串
     */
    public String getMsgJson() {
        return gson.toJson(this);
    }

    public PaxosMsgTypeEnum getMsgType() {
        return msgType;
    }

    public void setMsgType(PaxosMsgTypeEnum msgType) {
        this.msgType = msgType;
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
    }

    public int getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(int instanceID) {
        this.instanceID = instanceID;
    }

    public int getGroupID() {
        return groupID;
    }

    public void setGroupID(int groupID) {
        this.groupID = groupID;
    }

    public static Gson getGson() {
        return gson;
    }

    @Override
    public String toString() {
        return "BasePaxosMsg{" +
                "msgType=" + msgType +
                ", nodeID=" + nodeID +
                ", instanceID=" + instanceID +
                ", groupID=" + groupID +
                '}';
    }
}
