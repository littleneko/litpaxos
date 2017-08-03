package org.littleneko.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by little on 2017-06-16.
 */
public class PaxosMsgUtil {
    public static BasePaxosMsg getPaxosMsg(PaxosMsgTypeEnum paxosMsgTypeEnum, String json) {
        BasePaxosMsg basePaxosMsg = null;
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PaxosMsgTypeEnum.class, new PaxosMsgTypeSerializer());
        Gson gson = gsonBuilder.create();
        switch (paxosMsgTypeEnum) {
            case PAXOS_PREPARE:
                basePaxosMsg = gson.fromJson(json, PrepareMsg.class);
                break;
            case PAXOS_PREPARE_REPLAY:
                basePaxosMsg = gson.fromJson(json, PrepareReplayMsg.class);
                break;
            case PAXOS_ACCEPT:
                basePaxosMsg = gson.fromJson(json, AcceptMsg.class);
                break;
            case PAXOS_ACCEPT_REPLAY:
                basePaxosMsg = gson.fromJson(json, AcceptReplayMsg.class);
                break;
            case PAXOS_CHOSEN_VALUE:
                basePaxosMsg = gson.fromJson(json, ChosenValueMsg.class);
                break;
            case PAXOS_LEARN_REQUEST:
                basePaxosMsg = gson.fromJson(json, LearnRequestMsg.class);
                break;
            case PAXOS_LEARN_RESPONSE:
                basePaxosMsg = gson.fromJson(json, LearnResponseMsg.class);
                break;
            default:
                break;
        }
        return basePaxosMsg;
    }
}
