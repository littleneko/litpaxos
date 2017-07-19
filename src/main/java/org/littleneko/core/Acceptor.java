package org.littleneko.core;

import org.littleneko.comm.MsgTransport;
import org.littleneko.logstorage.AcceptorStateData;
import org.littleneko.logstorage.PaxosLog;
import org.littleneko.message.*;
import org.littleneko.node.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptor
 * Created by little on 2017-06-14.
 */
public class Acceptor extends Base {
    /**
     * 保存当前acceptor的信息
     */
    private class AccepterInfo {
        // 承诺不再接受编号小于promiseBallotNumber的投票, pb
        private BallotNumber promiseBallotNumber;

        // 最大编号投票对应的Bal, ab
        private BallotNumber acceptedBallotNumber;

        // 最大投票编号对应的Dec, av
        private String acceptedBallotValue;

        public AccepterInfo() {
            promiseBallotNumber = new BallotNumber(0, 0);
            acceptedBallotNumber = new BallotNumber(0, 0);
            acceptedBallotValue = null;
        }

        /**
         * 持久化当前acceptor的信息
         */
        public void persist() {
            AcceptorStateData acceptorStateData = new AcceptorStateData();
            acceptorStateData.setInstanceID(getInstance().getInstanceId());
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
    }

    protected static final Logger logger = LoggerFactory.getLogger(Acceptor.class);

    private AccepterInfo accepterInfo;

    private PaxosLog paxosLog;

    public Acceptor(MsgTransport msgTransport, Instance instance, NodeInfo curNodeInfo, PaxosLog paxosLog) {
        super(msgTransport, instance, curNodeInfo);
        this.accepterInfo = new AccepterInfo();
        this.paxosLog = paxosLog;
    }

    /**
     * 收到prepare消息的处理
     *
     * @param paxosMsg paxosMsg
     */
    public void onPrepare(PrepareMsg paxosMsg) {
        PrepareReplayMsg prepareReplayMsg = new PrepareReplayMsg();
        prepareReplayMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_PREPARE_REPLAY);
        prepareReplayMsg.setInstanceID(getInstance().getInstanceId());
        prepareReplayMsg.setNodeID(getCurNodeInfo().getNodeID());
        prepareReplayMsg.setProposalID(paxosMsg.getProposalID());

        BallotNumber ballotNumber = new BallotNumber(paxosMsg.getProposalID(), paxosMsg.getNodeID());

        // b >= pb，新提案编号 >= 当前已接受的提案编号，接受此提案
        if (ballotNumber.compareTo(accepterInfo.getPromiseBallotNumber()) >= 0) {
            logger.info("b >= pb, promise. b = {}, pb = {}, ab = {}, av = {}", paxosMsg.getProposalID(), accepterInfo.getPromiseBallotNumber(),
                    accepterInfo.getAcceptedBallotNumber().getProposalID(), accepterInfo.getAcceptedBallotValue());
            prepareReplayMsg.setOk(true);
            // set ab, av
            prepareReplayMsg.setMaxAcceptProposalID(accepterInfo.getAcceptedBallotNumber().getProposalID());
            prepareReplayMsg.setMaxAcceptProposalNodeID(accepterInfo.getAcceptedBallotNumber().getProposalNodeID());
            if (accepterInfo.getAcceptedBallotNumber().getProposalID() > 0) {
                prepareReplayMsg.setMaxAcceptProposalValue(accepterInfo.getAcceptedBallotValue());
            }

            // pb = b
            accepterInfo.setPromiseBallotNumber(ballotNumber);

            accepterInfo.persist();
        } else {
            logger.info("b < pb, reject. rejected by proposal ID: {}", accepterInfo.getPromiseBallotNumber().getProposalID());
            prepareReplayMsg.setOk(false);
            prepareReplayMsg.setRejectByProposalID(accepterInfo.getPromiseBallotNumber().getProposalID());
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
        acceptReplayMsg.setInstanceID(getInstance().getInstanceId());
        acceptReplayMsg.setNodeID(getCurNodeInfo().getNodeID());
        acceptReplayMsg.setProposalID(paxosMsg.getProposalID());

        BallotNumber ballotNumber = new BallotNumber(paxosMsg.getProposalID(), paxosMsg.getNodeID());

        // b >= pb
        if (ballotNumber.compareTo(accepterInfo.getPromiseBallotNumber()) >= 0) {
            logger.info("b >= pb, accept. b = {}, pb = {}", paxosMsg.getProposalID(), accepterInfo.getPromiseBallotNumber().getProposalID());
            acceptReplayMsg.setOk(true);
            // pb = b, ab = b, av = v
            accepterInfo.setPromiseBallotNumber(ballotNumber);
            accepterInfo.setAcceptedBallotNumber(ballotNumber);
            accepterInfo.setAcceptedBallotValue(paxosMsg.getProposalDec());

            accepterInfo.persist();

            //发送当前accepter的消息给所有learner
            sendChosenValue();
        } else {
            logger.info("b < pb, reject. rejected by proposal ID: {}", accepterInfo.getPromiseBallotNumber().getProposalID());
            acceptReplayMsg.setOk(false);
            acceptReplayMsg.setRejectByProposalID(accepterInfo.getPromiseBallotNumber().getProposalID());
        }

        sendMsg(paxosMsg.getNodeID(), acceptReplayMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_PREPARE_REPLAY);
    }

    /**
     * 开始新一轮投票
     */
    public void newRound() {
        accepterInfo.reset();
    }

    /**
     * 发送chosen value，在accepter接受一个提案后发送
     */
    private void sendChosenValue() {
        ChosenValueMsg chosenValueMsg = new ChosenValueMsg();
        chosenValueMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_CHOSEN_VALUE);
        chosenValueMsg.setInstanceID(getInstance().getInstanceId());
        chosenValueMsg.setNodeID(getCurNodeInfo().getNodeID());
        chosenValueMsg.setAcceptedProposalID(accepterInfo.getAcceptedBallotNumber().getProposalID());
        chosenValueMsg.setValue(accepterInfo.getAcceptedBallotValue());

        broadcastMessage(chosenValueMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_CHOSEN_VALUE);
    }
}