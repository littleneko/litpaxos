package org.littleneko.example;

import com.google.gson.Gson;

public class KVMessage {
    public enum TypeEnum {
        PUT,
        GET,
        DEL
    }

    private TypeEnum typeEnum;
    private String  key;
    private String  value;

    public KVMessage(TypeEnum typeEnum, String key, String value) {
        this.typeEnum = typeEnum;
        this.key = key;
        this.value = value;
    }

    public String getJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public TypeEnum getTypeEnum() {
        return typeEnum;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "KVMessage{" +
                "typeEnum=" + typeEnum +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
