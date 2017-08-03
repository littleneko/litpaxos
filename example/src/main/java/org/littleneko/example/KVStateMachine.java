package org.littleneko.example;

import com.google.gson.Gson;
import org.littleneko.sm.StateMachine;

import java.util.HashMap;
import java.util.Map;

public class KVStateMachine extends StateMachine {
    private Map<String, String> kv = new HashMap<>();

    @Override
    public void execute(String value) {
        Gson gson = new Gson();
        KVMessage kvMessage = gson.fromJson(value, KVMessage.class);
        switch (kvMessage.getTypeEnum()) {
            case PUT:
                kv.put(kvMessage.getKey(), kvMessage.getValue());
                System.out.println("Put: " + kvMessage.getKey() + "-> " + kvMessage.getValue());
                break;
            case DEL:
                kv.remove(kvMessage.getKey());
                System.out.println("Del: " + kvMessage.getKey() + "-> " + kvMessage.getValue());
                break;
            case GET:
                String v = kv.get(kvMessage.getKey());
                System.out.println("Get: " + kvMessage.getKey() + "-> " + v);
                break;
        }
    }
}
