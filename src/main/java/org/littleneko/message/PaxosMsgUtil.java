package org.littleneko.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by little on 2017-06-16.
 */
public class PaxosMsgUtil {
    public static BasePaxosMsg getPaxosMsg(PaxosMsgTypeEnum paxosMsgTypeEnum, String json) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PaxosMsgTypeEnum.class, new PaxosMsgTypeSerializer());
        Gson gson = gsonBuilder.create();
        switch (paxosMsgTypeEnum) {
            case PAXOS_PREPARE:
                PrepareMsg prepareMsg = gson.fromJson(json, PrepareMsg.class);
                break;
            case PAXOS_PREPARE_REPLAY:
                PrepareReplayMsg prepareReplayMsg = gson.fromJson(json, PrepareReplayMsg.class);
                break;
            case PAXOS_ACCEPT:
                break;
            case PAXOS_ACCEPT_REPLAY:
                break;
        }
        return null;
    }
}
