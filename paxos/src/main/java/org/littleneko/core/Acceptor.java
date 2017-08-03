package org.littleneko.core;

import org.littleneko.comm.MsgTransport;
import org.littleneko.logstorage.AcceptorStateData;
import org.littleneko.logstorage.PaxosLog;
import org.littleneko.message.*;
import org.littleneko.node.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Acceptor
 * Created by little on 2017-06-14.
 */
public class Acceptor extends Base {
    /**
     * 保存当前acceptor的信息
     */
    private class AccepterState {
        // 承诺不再接受编号小于promiseBallotNumber的投票, pb
        private BallotNumber promiseBallotNumber;

        // 最大编号投票对应的Bal, ab
        private BallotNumber acceptedBallotNumber;

        // 最大投票编号对应的Dec, av
        private String acceptedBallotValue;

        private int instanceID;

        public AccepterState(int instanceID) {
            this.promiseBallotNumber = new BallotNumber(0, 0);
            this.acceptedBallotNumber = new BallotNumber(0, 0);
            this.acceptedBallotValue = null;
            this.instanceID = instanceID;
        }

        /**
         * 持久化当前acceptor的信息
         */
        public void persist() {
            AcceptorStateData acceptorStateData = new AcceptorStateData();
            acceptorStateData.setInstanceID(instanceID);
            acceptorStateData.setPromiseID(promiseBallotNumber.getProposalID());
            acceptorStateData.setPromiseNodeID(promiseBallotNumber.getProposalNodeID());
            acceptorStateData.setAcceptedID(acceptedBallotNumber.getProposalID());
            acceptorStateData.setAcceptedNodeID(acceptedBallotNumber.getProposalNodeID());
            acceptorStateData.setAcceptedValue(acceptedBallotValue);

            paxosLog.writeAcceptorState(acceptorStateData);
        }

        /**
         * reset the acceptedBallotNumber
         */
        public void reset() {
            acceptedBallotNumber.reset();
            acceptedBallotValue = null;
        }


        public BallotNumber getPromiseBallotNumber() {
            return promiseBallotNumber;
        }

        public void setPromiseBallotNumber(BallotNumber promiseBallotNumber) {
            this.promiseBallotNumber = promiseBallotNumber;
        }

        public BallotNumber getAcceptedBallotNumber() {
            return acceptedBallotNumber;
        }

        public void setAcceptedBallotNumber(BallotNumber acceptedBallotNumber) {
            this.acceptedBallotNumber = acceptedBallotNumber;
        }

        public String getAcceptedBallotValue() {
            return acceptedBallotValue;
        }

        public void setAcceptedBallotValue(String acceptedBallotValue) {
            this.acceptedBallotValue = acceptedBallotValue;
        }

        public int getInstanceID() {
            return instanceID;
        }
    }

    protected static final Logger logger = LoggerFactory.getLogger(Acceptor.class);

    // Instance ID -> AccepterState
    private Map<Integer, AccepterState> accepterStateMap;

    private int lastSuccessInstanceID;

    private PaxosLog paxosLog;

    public Acceptor(MsgTransport msgTransport, InstanceManager InstanceManager, NodeInfo curNodeInfo, PaxosLog paxosLog) {
        super(msgTransport, InstanceManager, curNodeInfo);
        this.accepterStateMap = new HashMap<>();
        this.paxosLog = paxosLog;
        this.lastSuccessInstanceID = -1;
    }

    /**
     * 收到prepare消息的处理
     *
     * @param paxosMsg paxosMsg
     */
    public void onPrepare(PrepareMsg paxosMsg) {
        if (!accepterStateMap.containsKey(paxosMsg.getInstanceID())) {
            accepterStateMap.put(paxosMsg.getInstanceID(), new AccepterState(paxosMsg.getInstanceID()));
        }

        AccepterState accepterState = accepterStateMap.get(paxosMsg.getInstanceID());

        PrepareReplayMsg prepareReplayMsg = new PrepareReplayMsg();
        prepareReplayMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_PREPARE_REPLAY);
        prepareReplayMsg.setInstanceID(accepterState.getInstanceID());
        prepareReplayMsg.setNodeID(getCurNodeInfo().getNodeID());
        prepareReplayMsg.setProposalID(paxosMsg.getProposalID());

        BallotNumber ballotNumber = new BallotNumber(paxosMsg.getProposalID(), paxosMsg.getNodeID());

        // b >= pb，新提案编号 >= 当前已接受的提案编号，接受此提案
        if (ballotNumber.compareTo(accepterState.getPromiseBallotNumber()) >= 0) {
            logger.info("Recv prepare msg, b >= pb, promise. b = {}, pb = {}, ab = {}, av = {}", paxosMsg.getProposalID(), accepterState.getPromiseBallotNumber(),
                    accepterState.getAcceptedBallotNumber().getProposalID(), accepterState.getAcceptedBallotValue());
            prepareReplayMsg.setOk(true);
            // set ab, av
            prepareReplayMsg.setMaxAcceptProposalID(accepterState.getAcceptedBallotNumber().getProposalID());
            prepareReplayMsg.setMaxAcceptProposalNodeID(accepterState.getAcceptedBallotNumber().getProposalNodeID());
            if (accepterState.getAcceptedBallotNumber().getProposalID() > 0) {
                prepareReplayMsg.setMaxAcceptProposalValue(accepterState.getAcceptedBallotValue());
            }

            // pb = b
            accepterState.setPromiseBallotNumber(ballotNumber);

            accepterState.persist();

            //
            lastSuccessInstanceID = paxosMsg.getInstanceID();
        } else {
            logger.info("Recv prepare msg, b < pb, reject. rejected by proposal ID: {}", accepterState.getPromiseBallotNumber().getProposalID());
            prepareReplayMsg.setOk(false);
            prepareReplayMsg.setRejectByProposalID(accepterState.getPromiseBallotNumber().getProposalID());
        }

        sendMsg(paxosMsg.getNodeID(), prepareReplayMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_PREPARE_REPLAY);
    }

    /**
     * 收到accept消息的处理
     *
     * @param paxosMsg
     */
    public void onAccept(AcceptMsg paxosMsg) {
        AcceptReplayMsg acceptReplayMsg = new AcceptReplayMsg();
        acceptReplayMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_ACCEPT_REPLAY);
        acceptReplayMsg.setInstanceID(paxosMsg.getInstanceID());
        acceptReplayMsg.setNodeID(getCurNodeInfo().getNodeID());
        acceptReplayMsg.setProposalID(paxosMsg.getProposalID());

        BallotNumber ballotNumber = new BallotNumber(paxosMsg.getProposalID(), paxosMsg.getNodeID());

        if (!accepterStateMap.containsKey(paxosMsg.getInstanceID())) {
            accepterStateMap.put(paxosMsg.getInstanceID(), new AccepterState(paxosMsg.getInstanceID()));
        }
        AccepterState accepterState = accepterStateMap.get(paxosMsg.getInstanceID());

        // b >= pb
        if (ballotNumber.compareTo(accepterState.getPromiseBallotNumber()) >= 0) {
            logger.info("Recv accept msg, b >= pb, accept. b = {}, pb = {}", paxosMsg.getProposalID(), accepterState.getPromiseBallotNumber().getProposalID());
            acceptReplayMsg.setOk(true);
            // pb = b, ab = b, av = v
            accepterState.setPromiseBallotNumber(ballotNumber);
            accepterState.setAcceptedBallotNumber(ballotNumber);
            accepterState.setAcceptedBallotValue(paxosMsg.getProposalDec());

            accepterState.persist();

            //发送当前accepter的消息给所有learner
            sendChosenValue(accepterState);
        } else {
            logger.info("Recv accept msg, b < pb, reject. rejected by proposal ID: {}", accepterState.getPromiseBallotNumber().getProposalID());
            acceptReplayMsg.setOk(false);
            acceptReplayMsg.setRejectByProposalID(accepterState.getPromiseBallotNumber().getProposalID());
        }

        sendMsg(paxosMsg.getNodeID(), acceptReplayMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_ACCEPT_REPLAY);
    }

    /**
     * 开始新一轮投票
     */
    /*public void newRound() {
        accepterState.reset();
    }*/

    /**
     * 发送chosen value，在accepter接受一个提案后发送
     */
    private void sendChosenValue(AccepterState accepterState) {
        ChosenValueMsg chosenValueMsg = new ChosenValueMsg();
        chosenValueMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_CHOSEN_VALUE);
        chosenValueMsg.setInstanceID(accepterState.getInstanceID());
        chosenValueMsg.setNodeID(getCurNodeInfo().getNodeID());
        chosenValueMsg.setAcceptedProposalID(accepterState.getAcceptedBallotNumber().getProposalID());
        chosenValueMsg.setValue(accepterState.getAcceptedBallotValue());

        broadcastMessage(chosenValueMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_CHOSEN_VALUE);
    }
}