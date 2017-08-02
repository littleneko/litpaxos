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

import java.util.HashMap;
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
    private Map<String, Integer> acceptedValues;

    private int learnInterval;

    private PaxosTimer timer;

    public Learner(MsgTransport msgTransport, Instance instance, NodeInfo curNodeInfo,
                   int allNodeCount,
                   int learnInterval,
                   PaxosTimer timer) {
        super(msgTransport, instance, curNodeInfo);
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
        timer.addSchedule(Instance.LEARNER_TIMER_ID, (id) -> requestLearn(), learnInterval, TimeUnit.MINUTES);
    }


    /**
     * 发送learn request消息<br>
     * 用于实例对齐，学习已经确定值的instance
     */
    public void requestLearn() {
        LearnRequestMsg learnRequestMsg = new LearnRequestMsg();
        learnRequestMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_LEARN_REQUEST);
        learnRequestMsg.setNodeID(getCurNodeInfo().getNodeID());
        learnRequestMsg.setInstanceID(getInstance().getInstanceId());
        learnRequestMsg.setCurInstanceID(getInstance().getInstanceId());

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
        if (learnRequestMsg.getCurInstanceID() > getInstance().getInstanceId()) {
            return;
        }

        LearnResponseMsg learnResponseMsg = new LearnResponseMsg();
        learnResponseMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_LEARN_RESPONSE);
        learnResponseMsg.setNodeID(getCurNodeInfo().getNodeID());
        learnResponseMsg.setInstanceID(getInstance().getInstanceId());
        learnResponseMsg.setStartInstanceID(learnRequestMsg.getCurInstanceID());

        for (int i = learnRequestMsg.getCurInstanceID(); i < getInstance().getInstanceId(); i++) {
            if (getInstance().getInstanceValues().containsKey(i)) {
                learnResponseMsg.addInstanceState(i, getInstance().getInstanceValues().get(i));
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
        if (learnResponseMsg.getInstanceID() <= getInstance().getInstanceId()) {
            return;
        }

        learnResponseMsg.getValues().forEach((k, v) -> {
            if (k >= getInstance().getInstanceId()) {
                getInstance().saveValue(v);
                getInstance().getStateMachines().forEach((smv) -> smv.execute(v));
                // 学习instance并提高当前instance ID
                getInstance().newInstance();
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
        if (chosenValueMsg.getInstanceID() == getInstance().getInstanceId()) {
            if (acceptedValues.containsKey(chosenValueMsg.getValue())) {
                acceptedValues.put(chosenValueMsg.getValue(), acceptedValues.get(chosenValueMsg.getValue() + 1));
            } else {
                acceptedValues.put(chosenValueMsg.getValue(), 1);
            }

            acceptedValues.forEach((k, v) -> {
                // 被超过一半的acceptor接受
                if (v > allNodeCount / 2 + 1) {
                    logger.info("Proposal {} has been accepted by qrm acceptor", k);
                    // instance 保存该值并持久化
                    getInstance().saveValue(k);
                    // 执行sm
                    getInstance().getStateMachines().forEach((smv) -> smv.execute(k));
                    // 更新当前instance + 1
                    getInstance().newInstance();
                }
            });

            acceptedValues.clear();
        }
    }
}
