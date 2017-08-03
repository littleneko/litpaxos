package org.littleneko.message;


/**
 * Created by little on 2017-06-14.
 */
public enum PaxosMsgTypeEnum {
    PAXOS_UNKNOWN(0),
    PAXOS_PREPARE(1),
    PAXOS_PREPARE_REPLAY(2),
    PAXOS_ACCEPT(3),
    PAXOS_ACCEPT_REPLAY(4),
    PAXOS_LEARN_REQUEST(5),
    PAXOS_LEARN_RESPONSE(6),
    PAXOS_CHOSEN_VALUE(7);

    private final int msgId;

    PaxosMsgTypeEnum(int msgId) {
        this.msgId = msgId;
    }

    public int getMsgId() {
        return msgId;
    }

    public static PaxosMsgTypeEnum valueOfId(int id) {
        for (PaxosMsgTypeEnum type : PaxosMsgTypeEnum.values()) {
            if (type.getMsgId() == id)
                return type;
        }
        return PAXOS_UNKNOWN;
    }
}
