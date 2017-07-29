package org.littleneko.core;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 保存client request的值，实现批量提交
 */
public class Committer {
    private class CommitValue {
        private UUID valueID;
        private String value;

        public CommitValue(UUID valueID, String value) {
            this.valueID = valueID;
            this.value = value;
        }

        public UUID getValueID() {
            return valueID;
        }

        public void setValueID(UUID valueID) {
            this.valueID = valueID;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private BlockingQueue<CommitValue> commitValues = new LinkedBlockingDeque<>();

    public UUID newCommitValue(String value) {
        UUID uuid = UUID.randomUUID();
        CommitValue commitValue = new CommitValue(uuid, value);
        commitValues.offer(commitValue);
        return uuid;
    }

    public String GetCommitValue() {
        try {
            String value = commitValues.take().getValue();
            commitValues.remove(value);
            return value;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
