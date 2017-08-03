package org.littleneko.comm;

/**
 * Created by little on 2017-06-15.
 */
public interface MsgSendListener {
    public static enum MsgSendRetEnum {
        SUCCESS,
        FAIL,
        OTHER
    }

    void onMsgSendRet(MsgSendRetEnum msgSendRetEnum, int msgId);

    // 不做任何事的listener
    MsgSendListener DO_NOTHING = (msgSendRetEnum, msgId) -> {};
}
