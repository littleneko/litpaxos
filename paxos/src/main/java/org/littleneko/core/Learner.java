package org.littleneko.core;

import org.littleneko.comm.MsgTransport;
import org.littleneko.message.ChosenValueMsg;
import org.littleneko.message.LearnRequestMsg;
import org.littleneko.message.LearnResponseMsg;
import org.littleneko.message.PaxosMsgTypeEnum;
import org.littleneko.node.NodeInfo;
import org.littleneko.utils.PaxosTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Learner
 * Created by little on 2017-06-14.
 */
public class Learner extends Base {
    /**
     * 保存当前learner的信息
     */
    private class LearnerInfo {
        // 当前学习到的值
        private String learnedValue;

        // 标识是否成功学习
        private boolean isLearned;

        public LearnerInfo() {
            isLearned = false;
        }

        /**
         * Learn a Value
         *
         * @param learnedValue
         */
        public void LearnValue(String learnedValue) {
            this.learnedValue = learnedValue;
            this.isLearned = true;
        }

        public String getLearnedValue() {
            return learnedValue;
        }

        public boolean isLearned() {
            return isLearned;
        }

        public void setLearned(boolean learned) {
            isLearned = learned;
        }
    }

    protected static final Logger logger = LoggerFactory.getLogger(Learner.class);

    private LearnerInfo learnerInfo;

    private int allNodeCount;

    // 临时保存收到的values
    private Map<Integer, Map<String, Integer>> acceptedValues;

    private int learnInterval;

    private PaxosTimer timer;

    public Learner(MsgTransport msgTransport, InstanceManager InstanceManager, NodeInfo curNodeInfo,
                   int allNodeCount,
                   int learnInterval,
                   PaxosTimer timer) {
        super(msgTransport, InstanceManager, curNodeInfo);
        this.acceptedValues = new HashMap<>();
        this.allNodeCount = allNodeCount;
        this.learnInterval = learnInterval;
        this.timer = timer;
        this.learnerInfo = new LearnerInfo();
    }

    /**
     * 开始发送learner request线程
     */
    public void startLearner() {
        timer.addSchedule(InstanceManager.LEARNER_TIMER_ID, (id) -> requestLearn(), learnInterval, TimeUnit.MINUTES);
    }


    /**
     * 发送learn request消息<br>
     * 用于实例对齐，学习已经确定值的instance
     */
    public void requestLearn() {
        LearnRequestMsg learnRequestMsg = new LearnRequestMsg();
        learnRequestMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_LEARN_REQUEST);
        learnRequestMsg.setNodeID(getCurNodeInfo().getNodeID());
        learnRequestMsg.setInstanceID(getInstanceManager().getInstanceID().get());
        learnRequestMsg.setCurInstanceID(getInstanceManager().getInstanceID().get());

        logger.info("Send learn request: {}", learnRequestMsg);
        broadcastMessage(learnRequestMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_LEARN_REQUEST);
    }

    /**
     * 收到learn request消息的处理
     *
     * @param learnRequestMsg
     */
    public void onRequestLearn(LearnRequestMsg learnRequestMsg) {
        logger.info("Recv learn request msg: {}", learnRequestMsg);
        // 当前instance ID更小
        if (learnRequestMsg.getCurInstanceID() > getInstanceManager().getInstanceID().get()) {
            return;
        }

        LearnResponseMsg learnResponseMsg = new LearnResponseMsg();
        learnResponseMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_LEARN_RESPONSE);
        learnResponseMsg.setNodeID(getCurNodeInfo().getNodeID());
        learnResponseMsg.setInstanceID(getInstanceManager().getInstanceID().get());
        learnResponseMsg.setStartInstanceID(learnRequestMsg.getCurInstanceID());

        for (int i = learnRequestMsg.getCurInstanceID(); i < getInstanceManager().getInstanceID().get(); i++) {
            if (getInstanceManager().getInstanceValues().containsKey(i)) {
                learnResponseMsg.addInstanceState(i, getInstanceManager().getInstanceValues().get(i));
            }
        }

        logger.info("Send learn response: {}", learnResponseMsg);
        sendMsg(learnRequestMsg.getNodeID(), learnResponseMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_LEARN_RESPONSE);
    }

    /**
     * 收到learn response消息的处理
     *
     * @param learnResponseMsg
     */
    public void onLearnResponse(LearnResponseMsg learnResponseMsg) {
        logger.info("Recv learn response: {}", learnResponseMsg);
        // 发送response消息的节点的instance ID比当前结点的instance小或者相等，说明当前节点实例已经对齐
        if (learnResponseMsg.getInstanceID() <= getInstanceManager().getInstanceID().get()) {
            return;
        }

        learnResponseMsg.getValues().forEach((k, v) -> {
            if (k >= getInstanceManager().getInstanceID().get()) {
                getInstanceManager().saveValue(v);
                getInstanceManager().getStateMachines().forEach((smv) -> smv.execute(v));
                // 学习instance并提高当前instance ID
                getInstanceManager().newInstance();
            }
        });
    }

    /**
     * 收到的chosen value的处理
     *
     * @param chosenValueMsg
     */
    public void onRecvChosenValue(ChosenValueMsg chosenValueMsg) {
        logger.info("Recv chosen value: {}", chosenValueMsg);
        if (!acceptedValues.containsKey(chosenValueMsg.getInstanceID())) {
            acceptedValues.put(chosenValueMsg.getInstanceID(), new HashMap<>());
        }
        Map<String, Integer> values = acceptedValues.get(chosenValueMsg.getInstanceID());
        if (values.containsKey(chosenValueMsg.getValue())) {
            values.put(chosenValueMsg.getValue(), values.get(chosenValueMsg.getValue()) + 1);
        } else {
            values.put(chosenValueMsg.getValue(), 1);
        }

        List<Integer> delKey = new ArrayList<>();
        acceptedValues.forEach((k, v) ->
                v.forEach((k1, v1) -> {
                    // 被超过一半的acceptor接受
                    if (v1 > allNodeCount / 2 + 1) {
                        logger.info("Proposal {} has been accepted by qrm acceptor", k1);
                        // instance 保存该值并持久化
                        getInstanceManager().saveValue(k1);
                        // 执行sm
                        getInstanceManager().getStateMachines().forEach((smv) -> smv.execute(k1));
                        // 更新当前instance + 1
                        getInstanceManager().newInstance();

                        delKey.add(k);
                    }
                }));

        for (int key : delKey) {
            acceptedValues.remove(key);
        }
    }
}
