package org.littleneko.core;

import org.littleneko.comm.MsgTransport;
import org.littleneko.message.*;
import org.littleneko.node.NodeInfo;
import org.littleneko.utils.PaxosTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Proposer
 * Created by little on 2017-06-14.
 */
public class Proposer extends Base {
    private enum ProposerStatusEnum {
        // 初始状态
        INITIAL,
        // 进入prepare流程
        PREPARING,
        // 进入accept流程
        ACCEPTING
    }

    /**
     * 保存当前instance Proposer的信息
     */
    private class ProposerState {
        // b, 当前提议的编号
        private int curProposalID;

        // maxb，即最大编号投票对应的bal
        private BallotNumber highestOtherPreAcceptBallot;

        // v, 提议的值
        private String proposalValue;

        // 其他节点最大提议编号，被reject时收到
        private int highestOtherProposalID;

        // 表示当前proposer处在哪个阶段
        private ProposerStatusEnum proposerStatusEnum;

        // 保存收到的信息
        private MsgCounter msgCounter;

        private int instanceID;

        /**
         *
         * @param initBal
         * @param proposalValue
         * @param allNodeCount
         */
        public ProposerState(int initBal, String proposalValue, int instanceID, int allNodeCount) {
            this.curProposalID = initBal;
            this.highestOtherPreAcceptBallot = new BallotNumber(0, 0);
            this.proposalValue = proposalValue;
            this.highestOtherProposalID = 0;
            this.proposerStatusEnum = ProposerStatusEnum.INITIAL;
            this.msgCounter = new MsgCounter(allNodeCount);
            this.instanceID = instanceID;
        }

        /**
         * 开始新的prepare，更新proposal的ID
         */
        public void newPrepare() {
            // 如果被highestOtherProposalID reject过，可以直接增加proposal ID到highestOtherProposalID + 1
            int proposalID = curProposalID > highestOtherProposalID ? curProposalID : highestOtherProposalID;
            curProposalID = proposalID + 1;
        }

        /**
         * 更新 maxb, v
         *
         * @param ballotNumber  ab
         * @param proposalValue av
         */
        public void updatePreAcceptValue(BallotNumber ballotNumber, String proposalValue) {
            // ab > maxb
            if (ballotNumber.compareTo(highestOtherPreAcceptBallot) > 0 && proposalValue != null) {
                this.highestOtherPreAcceptBallot = ballotNumber;
                this.proposalValue = proposalValue;
            }
        }

        /**
         * 更新收到的其他节点的最大proposal ID
         *
         * @param highestOtherProposalID highestOtherProposalID
         */
        public void updateHighestOtherProposalID(int highestOtherProposalID) {
            if (highestOtherProposalID > this.highestOtherProposalID) {
                this.highestOtherProposalID = highestOtherProposalID;
            }
        }

        /**
         * 重置相关的值
         */
        public void reset() {
            highestOtherPreAcceptBallot.reset();
            proposalValue = null;
            highestOtherProposalID = 0;
        }

        public int getCurProposalID() {
            return curProposalID;
        }

        public String getProposalValue() {
            return proposalValue;
        }

        public MsgCounter getMsgCounter() {
            return msgCounter;
        }

        public ProposerStatusEnum getProposerStatusEnum() {
            return proposerStatusEnum;
        }

        public void setProposerStatusEnum(ProposerStatusEnum proposerStatusEnum) {
            this.proposerStatusEnum = proposerStatusEnum;
        }

        public int getInstanceID() {
            return instanceID;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Proposer.class);

    private static final int PREPARE_TIMEOUT = 5;
    private static final int ACCEPT_TIMEOUT = 5;

    // Instance ID -> ProposerState
    private Map<Integer, ProposerState> proposerStateMap;

    private PaxosTimer timer;

    // 标识proposer是否可以跳过prepare阶段直接进入accept
    private boolean canSkipPrepare;

    // 记录当前轮投票中是否被acceptor拒绝过
    private boolean wasRejectedBySomeOne;

    private int allNodeCount;

    public Proposer(MsgTransport msgTransport, NodeInfo curNodeInfo, InstanceManager InstanceManager, PaxosTimer timer, int allNodeCount) {
        super(msgTransport, InstanceManager, curNodeInfo);
        this.timer = timer;
        this.canSkipPrepare = false;
        this.wasRejectedBySomeOne = false;
        this.proposerStateMap = new HashMap<>();
        this.allNodeCount = allNodeCount;
    }

    /**
     * @param value 提议的值
     */
    public void newBallot(String value) {
        wasRejectedBySomeOne = false;
        int curInstanceID = getInstanceManager().getInstanceID().addAndGet(1);
        ProposerState proposerState = new ProposerState(0, value, curInstanceID, allNodeCount);
        proposerStateMap.put(curInstanceID, proposerState);

        if (canSkipPrepare && !wasRejectedBySomeOne) {
            logger.info("Instance {} skip prepare", curInstanceID);
            accept(proposerState);
        } else {
            logger.info("Instance {} start prepare", curInstanceID);
            prepare(wasRejectedBySomeOne, proposerState);
        }
    }

    /**
     * 开始prepare流程
     *
     * @param isRejected 标识在该轮投票中prepare请求是否被reject过<br>
     *                   isRejected == true：prepare被拒绝，需要提高自己的proposal编号<br>
     *                   isRejected == false：prepare超时或者开始新一轮的投票，使用当前编号继续提交<br>
     */
    private void prepare(boolean isRejected, ProposerState proposerState) {
        exitAccept();
        proposerState.setProposerStatusEnum(ProposerStatusEnum.PREPARING);
        canSkipPrepare = false;
        wasRejectedBySomeOne = false;

        // 更新proposal编号
        if (isRejected) {
            proposerState.newPrepare();
        }

        PrepareMsg prepareMsg = new PrepareMsg();
        prepareMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_PREPARE);
        prepareMsg.setInstanceID(proposerState.instanceID);
        prepareMsg.setNodeID(getCurNodeInfo().getNodeID());
        prepareMsg.setProposalID(proposerState.getCurProposalID());

        // 清空msgCounter
        proposerState.getMsgCounter().startNewRound();

        //添加prepare超时定时器
        timer.addTimer(InstanceManager.PREPARE_TIMER_ID, (id) -> prepare(wasRejectedBySomeOne, proposerState), PREPARE_TIMEOUT, TimeUnit.SECONDS);

        logger.info("Send prepare msg, instance: {}, nodeID: {}, proposalID: {}", prepareMsg.getInstanceID(), prepareMsg.getNodeID(), prepareMsg.getProposalID());
        // 发送prepare到其他所有节点
        broadcastMessage(prepareMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_PREPARE);
    }

    /**
     * 收到prepare回复
     *
     * @param paxosMsg 收到的回复消息
     */
    public void onPrepareReply(PrepareReplayMsg paxosMsg) {
        if (!proposerStateMap.containsKey(paxosMsg.getInstanceID())) {
            logger.warn("Recv prepare reply msg, but invalid instance ID {}", paxosMsg.getInstanceID());
            return;
        }

        ProposerState proposerState =  proposerStateMap.get(paxosMsg.getInstanceID());
        // 当前以不在Prepare阶段，不处理
        // 1. 某个节点响应过慢，提案已进入到Accept阶段
        // 2. 整个提案响应过慢，提案已终止
        if (proposerState.getProposerStatusEnum() != ProposerStatusEnum.PREPARING) {
            logger.info("Cur proposer {} is not in PREPARING State", proposerState.getInstanceID());
            return;
        }

        // 不是当前提议编号，不处理
        if (paxosMsg.getProposalID() != proposerState.getCurProposalID()) {
            logger.info("Cur proposer ID: {} != Reply proposal ID: {}", proposerState.getCurProposalID(), paxosMsg.getProposalID());
            return;
        }

        proposerState.getMsgCounter().addReceive(paxosMsg.getNodeID());

        if (paxosMsg.isOk()) {
            logger.info("Prepare ok, instance ID: {}, prepare ID: {}, max accept ID: {}",
                    proposerState.getInstanceID(), paxosMsg.getProposalID(), paxosMsg.getMaxAcceptProposalID());
            proposerState.getMsgCounter().addPromiseOrAccept(paxosMsg.getNodeID());
            BallotNumber ballotNumber = new BallotNumber(paxosMsg.getMaxAcceptProposalID(), paxosMsg.getMaxAcceptProposalNodeID());
            if (paxosMsg.getMaxAcceptProposalID() > proposerState.getCurProposalID()) {
                if (paxosMsg.getMaxAcceptProposalValue() != null) {
                    // 记录ab, av
                    logger.info("update max accepted value: {}", paxosMsg.getMaxAcceptProposalValue());
                    proposerState.updatePreAcceptValue(ballotNumber, paxosMsg.getMaxAcceptProposalValue());
                }
            }
        } else {
            logger.info("Prepare reject by proposal ID: {}", paxosMsg.getRejectByProposalID());
            proposerState.getMsgCounter().addReject(paxosMsg.getNodeID());
            wasRejectedBySomeOne = true;
            // 记录拒绝提案节点返回的Proposal ID，以便下次基于此信息重新发起提案
            proposerState.updateHighestOtherProposalID(paxosMsg.getRejectByProposalID());
        }

        // 超过qrm的人数通过，开始accept流程
        if (proposerState.getMsgCounter().isPassedOnThisRound()) {
            logger.info("Passed on this round");
            // 这里做了一个优化，一旦Prepare阶段的提案被通过后，就自动跳过Prepare阶段，以减少网络传输落盘次数
            canSkipPrepare = true;
            accept(proposerState);
        } else if (proposerState.getMsgCounter().isRejectedOnThisRound() ||
                proposerState.getMsgCounter().isAllReceiveOnThisRound()) {
            logger.info("Reject on this Round, retry after {} s", Proposer.PREPARE_TIMEOUT);
            timer.addTimer(InstanceManager.PREPARE_TIMER_ID, (id) -> prepare(wasRejectedBySomeOne, proposerState), Proposer.PREPARE_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    /**
     * 开始accept流程
     */
    private void accept(ProposerState proposerState) {
        // 改变当前状态并取消prepare超时定时器
        exitPrepare();
        proposerState.setProposerStatusEnum(ProposerStatusEnum.ACCEPTING);

        AcceptMsg acceptMsg = new AcceptMsg();
        acceptMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_ACCEPT);
        acceptMsg.setInstanceID(proposerState.getInstanceID());
        acceptMsg.setNodeID(getCurNodeInfo().getNodeID());
        acceptMsg.setProposalID(proposerState.getCurProposalID());
        acceptMsg.setProposalDec(proposerState.getProposalValue());

        proposerState.getMsgCounter().startNewRound();

        // 添加accept超时定时器
        timer.addTimer(InstanceManager.ACCEPT_TIMER_ID, (id) -> accept(proposerState), ACCEPT_TIMEOUT, TimeUnit.SECONDS);

        logger.info("Send accept msg, instance: {}, nodeID: {}, proposalID: {}, proposal value: {}", acceptMsg.getInstanceID(), acceptMsg.getNodeID(), acceptMsg.getProposalID(), acceptMsg.getProposalDec());
        // 广播accept消息
        broadcastMessage(acceptMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_ACCEPT);
    }

    /**
     * 收到accept回复
     *
     * @param paxosMsg
     */
    public void onAcceptReply(AcceptReplayMsg paxosMsg) {
        if (!proposerStateMap.containsKey(paxosMsg.getInstanceID())) {
            logger.warn("Recv accept reply msg, but invalid instance ID {}", paxosMsg.getInstanceID());
            return;
        }
        // 当前已不在Accept阶段
        // 1. 某个节点响应过慢，提案已完成
        // 2. 多个节点响应过慢，提案已重新进入Prepare阶段
        // 3. 整个提案响应过慢，提案已终止

        ProposerState proposerState =  proposerStateMap.get(paxosMsg.getInstanceID());
        if (proposerState.getProposerStatusEnum() != ProposerStatusEnum.ACCEPTING) {
            logger.info("Cur proposer is not in ACCEPTING State");
            return;
        }

        // 不是当前提议编号，不处理
        if (paxosMsg.getProposalID() != proposerState.getCurProposalID()) {
            logger.info("Cur proposer ID: {} != Recv proposal ID: {}", proposerState.getCurProposalID(), paxosMsg.getProposalID());
            return;
        }

        proposerState.getMsgCounter().addReceive(paxosMsg.getNodeID());

        if (paxosMsg.isOk()) {
            logger.info("Accept ok, accept ID: {}", paxosMsg.getProposalID());
            proposerState.getMsgCounter().addPromiseOrAccept(paxosMsg.getNodeID());
        } else {
            logger.info("Accept reject, reject by proposal ID: {}", paxosMsg.getRejectByProposalID());
            proposerState.getMsgCounter().addReject(paxosMsg.getNodeID());
            wasRejectedBySomeOne = true;
            proposerState.updateHighestOtherProposalID(paxosMsg.getRejectByProposalID());
        }

        if (proposerState.getMsgCounter().isPassedOnThisRound()) {
            // 本轮提议完成
            logger.info("Accept passed on this round");
            exitAccept();
        } else {
            // 重新提交
            logger.info("Accept reject on this round, retry");
            timer.addTimer(InstanceManager.ACCEPT_TIMER_ID, (id) -> accept(proposerState), Proposer.ACCEPT_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    /**
     * 开始新一轮的提议
     */
    public void newRound() {
        logger.info("New Round");
        exitPrepare();
        exitAccept();
    }

    /**
     * 结束prepare流程
     */
    private void exitPrepare() {
        timer.cancelTimer(InstanceManager.PREPARE_TIMER_ID);
    }

    /**
     * 结束accept流程
     */
    private void exitAccept() {
        timer.cancelTimer(InstanceManager.ACCEPT_TIMER_ID);
    }
}