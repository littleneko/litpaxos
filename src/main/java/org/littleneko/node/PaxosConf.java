package org.littleneko.node;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class PaxosConf {
    // 用于Proposer的超时定时器, 毫秒
    @SerializedName("commTimeOut")
    private int commTimeOut;
    // Learner的学习时间间隔，毫秒
    @SerializedName("learnInterval")
    private int learnInterval;
    // 日志持久化存储的位置
    @SerializedName("logFile")
    private String logFile;

    public int getCommTimeOut() {
        return commTimeOut;
    }

    public void setCommTimeOut(int commTimeOut) {
        this.commTimeOut = commTimeOut;
    }

    public int getLearnInterval() {
        return learnInterval;
    }

    public void setLearnInterval(int learnInterval) {
        this.learnInterval = learnInterval;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    @Override
    public String toString() {
        return "PaxosConf{" +
                ", commTimeOut=" + commTimeOut +
                ", learnInterval=" + learnInterval +
                ", logFile='" + logFile + '\'' +
                '}';
    }
}
