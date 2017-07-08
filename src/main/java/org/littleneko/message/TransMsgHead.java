package org.littleneko.message;

/**
 * 传输的消息头
 * <p>
 * 格式：<br>
 * 0        8        16       24       32<br>
 * +--------+--------+--------+--------+<br>
 * |     version     |       type      |<br>
 * +--------+--------+--------+--------+<br>
 * |                id                 |<br>
 * +--------+--------+--------+--------+<br>
 * |                length             |<br>
 * +--------+--------+--------+--------+<br>
 * Created by little on 2017-06-14.
 */
public class TransMsgHead {
    public static final int TRANS_MSG_HEAD_LENGTH = 12;

    // Version, 2 bytes
    private int version;

    // 消息类型, 2 bytes
    private PaxosMsgTypeEnum msgType;

    // 消息id， 4 bytes
    private int id;

    // 消息体的长度，不包括头. 4 bytes
    private int msgLength;

    public TransMsgHead(int version, PaxosMsgTypeEnum msgType, int id,  int msgLength) {
        this.version = version;
        this.msgType = msgType;
        this.id = id;
        this.msgLength = msgLength;
    }

    public int getMsgLength() {
        return msgLength;
    }

    public void setMsgLength(int msgLength) {
        this.msgLength = msgLength;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PaxosMsgTypeEnum getMsgType() {
        return msgType;
    }

    public void setMsgType(PaxosMsgTypeEnum msgType) {
        this.msgType = msgType;
    }
}
