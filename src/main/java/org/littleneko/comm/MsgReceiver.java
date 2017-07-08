package org.littleneko.comm;

/**
 * Created by little on 2017-06-27.
 */
public interface MsgReceiver {
    void startServer();
    void addMsgReceiverListener(MsgRecvListener msgRecvListener);
}
