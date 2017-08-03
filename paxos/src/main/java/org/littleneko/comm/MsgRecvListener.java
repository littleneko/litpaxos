package org.littleneko.comm;

import org.littleneko.message.PaxosMsgTypeEnum;
import org.littleneko.message.TransMsg;

/**
 * Created by little on 2017-06-15.
 */
public interface MsgRecvListener {
    void onMsgRecv(PaxosMsgTypeEnum paxosMsgTypeEnum, TransMsg transMsg);
}
