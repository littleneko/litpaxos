package org.littleneko.message;

/**
 *  在网络中传送的消息格式
 * Created by little on 2017-06-14.
 */
public class TransMsg {
    private TransMsgHead transMsgHead;
    // 实际传送的消息数据
    private byte[] paxosMsg;

    public TransMsg(TransMsgHead transMsgHead, byte[] paxosMsg) {
        this.transMsgHead = transMsgHead;
        this.paxosMsg = paxosMsg;
    }

    public TransMsgHead getTransMsgHead() {
        return transMsgHead;
    }

    public void setTransMsgHead(TransMsgHead transMsgHead) {
        this.transMsgHead = transMsgHead;
    }

    public byte[] getPaxosMsg() {
        return paxosMsg;
    }

    public void setPaxosMsg(byte[] paxosMsg) {
        this.paxosMsg = paxosMsg;
    }
}
