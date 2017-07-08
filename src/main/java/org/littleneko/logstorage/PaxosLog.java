package org.littleneko.logstorage;

import com.google.gson.Gson;
import org.littleneko.core.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 持久化到磁盘
 * Created by little on 2017-06-23.
 */
public class PaxosLog {
    private static final Logger logger = LoggerFactory.getLogger(PaxosLog.class);

    // 每个group都有一个独立的paxoslog
    private int groupID;

    public PaxosLog(int groupID) {
        this.groupID = groupID;
    }

    /**
     * 存储当前acceptor的信息到文件
     *
     * @param acceptorStateData
     */
    public void writeAcceptorState(AcceptorStateData acceptorStateData) {
        Gson gson = new Gson();
        String string = gson.toJson(acceptorStateData);
        logger.info("AAAAAA {}", string);
    }

    /**
     * 读取当前acceptor state<br>
     * 用于程序崩溃后恢复
     *
     * @return
     */
    public AcceptorStateData readAcceptorState() {
        return null;
    }

    /**
     * 持久化instance信息
     *
     * @param instanceState
     */
    public void appendInstanceState(InstanceState instanceState) {
        Gson gson = new Gson();
        String string = gson.toJson(instanceState);
        logger.info("BBBBB {}", string);
    }

    /**
     * 从文件中读取instance state
     *
     * @return
     */
    public Map<Integer, InstanceState> readInstanceState() {
        return null;
    }

    /**
     * 从文件中读取instance state，指定开始读取的instance ID
     *
     * @param startInstanceID
     * @return
     */
    public Map<Integer, InstanceState> readInstanceState(int startInstanceID) {
        return null;
    }
}
