package org.littleneko.core;

import org.littleneko.comm.MsgTransport;
import org.littleneko.logstorage.InstanceState;
import org.littleneko.logstorage.PaxosLog;
import org.littleneko.message.*;
import org.littleneko.node.NodeInfo;
import org.littleneko.sm.StateMachine;
import org.littleneko.utils.PaxosTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Instance
 * Created by little on 2017-06-14.
 */
public class Instance {
    protected static final Logger logger = LoggerFactory.getLogger(Instance.class);

    public static final int PREPARE_TIMER_ID = 0;
    public static final int ACCEPT_TIMER_ID = 1;
    public static final int LEARNER_TIMER_ID = 2;

    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;

    private volatile int instanceId;

    // 一个instance可以有多个SM
    private Map<Integer, StateMachine> stateMachineMap;

    // 保存每个instance的value
    private Map<Integer, String> instanceValues;

    private PaxosLog paxosLog;
    private PaxosTimer timer;

    // 存储接受的paxos消息
    private BlockingQueue<BasePaxosMsg> paxosMsgs;

    public Instance(MsgTransport msgTransport, NodeInfo nodeInfo, int allNodeCount, int groupID) {
        paxosLog = new PaxosLog(groupID);
        timer = new PaxosTimer();
        proposer = new Proposer(msgTransport, nodeInfo, this, timer, allNodeCount);
        acceptor = new Acceptor(msgTransport, this, nodeInfo, paxosLog);
        learner = new Learner(msgTransport, this, nodeInfo, allNodeCount, 500, timer);

        instanceId = 0;
        stateMachineMap = new HashMap<>();
        instanceValues = new HashMap<>();

        paxosMsgs = new LinkedBlockingQueue<>();
    }

    /**
     * 开始等待读取消息
     */
    public void startInstance() {
        new Thread(() -> {
            while (true) {
                try {
                    BasePaxosMsg paxosMsg = paxosMsgs.take();
                    if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_PREPARE ||
                            paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_ACCEPT) {
                        recvMsgForAcceptor(paxosMsg);
                    } else if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_PREPARE_REPLAY ||
                            paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_ACCEPT_REPLAY) {
                        recvMsgForProposer(paxosMsg);
                    } else if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_CHOSEN_VALUE ||
                            paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_LEARN_REQUEST ||
                            paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_LEARN_RESPONSE) {
                        recvMsgForLearner(paxosMsg);
                    }
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 开始新的instance
     */
    public void newInstance() {
        proposer.newRound();
        acceptor.newRound();
        instanceId++;
        logger.info("New instance ID: {}", instanceId);
    }

    /**
     * 保存当前instance的value
     *
     * @param value
     */
    public void saveValue(String value) {
        logger.info("Persist instance: {}, value: {}", instanceId, value);
        instanceValues.put(instanceId, value);
        persist();
    }

    /**
     * 添加一个sm
     *
     * @param stateMachine
     */
    public void addSM(StateMachine stateMachine) {
        stateMachineMap.put(stateMachine.getSMID(), stateMachine);
    }

    /**
     * 接受paxos消息，实际上是把消息加入队列
     *
     * @param paxosMsg
     */
    public void recvPaxosMsg(BasePaxosMsg paxosMsg) {
        paxosMsgs.offer(paxosMsg);
    }

    /**
     * 持久化当前instance信息
     */
    private void persist() {
        InstanceState instanceState = new InstanceState();
        instanceState.setInstanceID(instanceId);
        instanceState.setValue(instanceValues.get(instanceId));
        paxosLog.appendInstanceState(instanceState);
    }

    /**
     * @param paxosMsg
     */
    private void recvMsgForProposer(BasePaxosMsg paxosMsg) {
        logger.info("Recv msg for proposal: {}", paxosMsg);
        if (paxosMsg.getInstanceID() == instanceId) {
            if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_PREPARE_REPLAY) {
                proposer.onPrepareReply((PrepareReplayMsg) paxosMsg);
            } else if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_ACCEPT_REPLAY) {
                proposer.onAcceptReply((AcceptReplayMsg) paxosMsg);
            }
        } else {
            logger.warn("Not same instance, recv instance: {}, myinstance: {}", paxosMsg.getInstanceID(), instanceId);
        }
    }

    /**
     * @param paxosMsg
     */
    private void recvMsgForAcceptor(BasePaxosMsg paxosMsg) {
        logger.info("Recv msg for acceptor: {}", paxosMsg);
        if (paxosMsg.getInstanceID() == instanceId) {
            if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_PREPARE) {
                acceptor.onPrepare((PrepareMsg) paxosMsg);
            } else if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_ACCEPT) {
                acceptor.onAccept((AcceptMsg) paxosMsg);
            }
        } else {
            logger.warn("Not same instance, recv instance: {}, myinstance: {}", paxosMsg.getInstanceID(), instanceId);
        }
    }

    /**
     * @param paxosMsg
     */
    private void recvMsgForLearner(BasePaxosMsg paxosMsg) {
        logger.info("Recv msg for learner: {}", paxosMsg);
        if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_CHOSEN_VALUE) {
            learner.onRecvChosenValue((ChosenValueMsg) paxosMsg);
        } else if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_LEARN_REQUEST) {
            learner.onRequestLearn((LearnRequestMsg) paxosMsg);
        } else if (paxosMsg.getMsgType() == PaxosMsgTypeEnum.PAXOS_LEARN_RESPONSE) {
            learner.onLearnResponse((LearnResponseMsg) paxosMsg);
        }
    }

    public int getInstanceId() {
        return instanceId;
    }

    public Map<Integer, String> getInstanceValues() {
        return instanceValues;
    }

    public Map<Integer, StateMachine> getStateMachineMap() {
        return stateMachineMap;
    }
}
