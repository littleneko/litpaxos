package org.littleneko.core;

import org.littleneko.comm.MsgTransport;
import org.littleneko.message.*;
import org.littleneko.node.NodeInfo;
import org.littleneko.utils.PaxosTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Proposer
 * Created by little on 2017-06-14.
 */
public class Proposer extends Base {
    private enum ProposerState {
        // 初始状态
        INITIAL,
        // 进入prepare流程
        PREPARING,
        // 进入accept流程
        ACCEPTING
    }

    /**
     * 保存当前Proposer的信息
     */
    private class ProposerInfo {
        // b, 当前提议的编号
        private int curProposalID;

        // maxb，即最大编号投票对应的bal
        private BallotNumber highestOtherPreAcceptBallot;

        // v, 提议的值
        private String proposalValue;

        // 其他节点最大提议编号，被reject时收到
        private int highestOtherProposalID;

        public ProposerInfo(int initBal) {
            this.curProposalID = initBal;
            this.highestOtherPreAcceptBallot = new BallotNumber(0, 0);
            proposalValue = null;
            highestOtherProposalID = 0;
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
            highestOtherPreAcceptBallot = new BallotNumber(0, 0);
            proposalValue = null;
            highestOtherProposalID = 0;
        }

        public int getCurProposalID() {
            return curProposalID;
        }

        public String getProposalValue() {
            return proposalValue;
        }

        public void setProposalValue(String proposalValue) {
            this.proposalValue = proposalValue;
        }

        public int getHighestOtherProposalID() {
            return highestOtherProposalID;
        }

        public BallotNumber getHighestOtherPreAcceptBallot() {
            return highestOtherPreAcceptBallot;
        }
    }

    protected static final Logger logger = LoggerFactory.getLogger(Proposer.class);

    private static final int PREPARE_TIMEOUT = 5;
    private static final int ACCEPT_TIMEOUT = 5;

    // 表示当前proposer处在哪个阶段
    private ProposerState proposerState;

    // 保存收到的信息
    private MsgCounter msgCounter;

    // 保存当前proposer的相关信息
    private ProposerInfo proposerInfo;

    private PaxosTimer timer;

    // 标识proposer是否可以跳过prepare阶段直接进入accept
    private boolean canSkipPrepare;

    // 记录当前轮投票中是否被acceptor拒绝过
    private boolean wasRejectedBySomeOne;

    public Proposer(MsgTransport msgTransport, NodeInfo curNodeInfo, Instance instance, PaxosTimer timer, int allNodeCount) {
        super(msgTransport, instance, curNodeInfo);
        this.proposerState = ProposerState.INITIAL;
        this.msgCounter = new MsgCounter(allNodeCount);
        this.proposerInfo = new ProposerInfo(0);
        this.timer = timer;
        this.canSkipPrepare = false;
        this.wasRejectedBySomeOne = false;
    }

    /**
     * 开始新一轮的投票
     *
     * @param value 提议的值
     */
    public void newBallot(String value) {
        wasRejectedBySomeOne = false;
        proposerInfo.setProposalValue(value);

        if (canSkipPrepare && !wasRejectedBySomeOne) {
            logger.info("Skip prepare");
            accept();
        } else {
            logger.info("Start prepare");
            prepare(wasRejectedBySomeOne);
        }
    }

    /**
     * 开始prepare流程
     *
     * @param isRejected 标识在该轮投票中prepare请求是否被reject过<br>
     *                   isRejected == true：prepare被拒绝，需要提高自己的proposal编号<br>
     *                   isRejected == false：prepare超时或者开始新一轮的投票，使用当前编号继续提交<br>
     */
    private void prepare(boolean isRejected) {
        exitAccept();
        proposerState = ProposerState.PREPARING;
        canSkipPrepare = false;
        wasRejectedBySomeOne = false;

        // 更新proposal编号
        if (isRejected) {
            proposerInfo.newPrepare();
        }

        PrepareMsg prepareMsg = new PrepareMsg();
        prepareMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_PREPARE);
        prepareMsg.setInstanceID(getInstance().getInstanceId());
        prepareMsg.setNodeID(getCurNodeInfo().getNodeID());
        prepareMsg.setProposalID(proposerInfo.getCurProposalID());

        // 清空msgCounter
        msgCounter.startNewRound();

        //添加prepare超时定时器
        timer.addTimer(Instance.PREPARE_TIMER_ID, (id) -> prepare(wasRejectedBySomeOne), PREPARE_TIMEOUT, TimeUnit.SECONDS);

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
        // 当前以不在Prepare阶段，不处理
        // 1. 某个节点响应过慢，提案已进入到Accept阶段
        // 2. 整个提案响应过慢，提案已终止
        if (proposerState != ProposerState.PREPARING) {
            logger.info("Cur proposer is not in PREPARING State");
            return;
        }

        // 不是当前提议编号，不处理
        if (paxosMsg.getProposalID() != proposerInfo.getCurProposalID()) {
            logger.info("Cur proposer ID: {} != Recv proposal ID: {}", proposerInfo.getCurProposalID(), paxosMsg.getProposalID());
            return;
        }

        msgCounter.addReceive(paxosMsg.getNodeID());

        if (paxosMsg.isOk()) {
            logger.info("Prepare ok, prepare ID: {}, max accept ID: {}", paxosMsg.getProposalID(), paxosMsg.getMaxAcceptProposalID());
            msgCounter.addPromiseOrAccept(paxosMsg.getNodeID());
            BallotNumber ballotNumber = new BallotNumber(paxosMsg.getMaxAcceptProposalID(), paxosMsg.getMaxAcceptProposalNodeID());
            if (paxosMsg.getMaxAcceptProposalID() > proposerInfo.getCurProposalID()) {
                if (paxosMsg.getMaxAcceptProposalValue() != null) {
                    // 记录ab, av
                    logger.info("update max accepted value: {}", paxosMsg.getMaxAcceptProposalValue());
                    proposerInfo.updatePreAcceptValue(ballotNumber, paxosMsg.getMaxAcceptProposalValue());
                }
            }
        } else {
            logger.info("Prepare reject by proposal ID: {}", paxosMsg.getRejectByProposalID());
            msgCounter.addReject(paxosMsg.getNodeID());
            wasRejectedBySomeOne = true;
            // 记录拒绝提案节点返回的Proposal ID，以便下次基于此信息重新发起提案
            proposerInfo.updateHighestOtherProposalID(paxosMsg.getRejectByProposalID());
        }

        // 超过qrm的人数通过，开始accept流程
        if (msgCounter.isPassedOnThisRound()) {
            logger.info("Passed on this round");
            // 这里做了一个优化，一旦Prepare阶段的提案被通过后，就自动跳过Prepare阶段，以减少网络传输落盘次数
            canSkipPrepare = true;
            accept();
        } else if (msgCounter.isRejectedOnThisRound() || msgCounter.isAllReceiveOnThisRound()) {
            logger.info("Reject on this Round, retry after {} s", Proposer.PREPARE_TIMEOUT);
            timer.addTimer(Instance.PREPARE_TIMER_ID, (id) -> prepare(wasRejectedBySomeOne), Proposer.PREPARE_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    /**
     * 开始accept流程
     */
    private void accept() {
        // 改变当前状态并取消prepare超时定时器
        exitPrepare();
        proposerState = ProposerState.ACCEPTING;

        AcceptMsg acceptMsg = new AcceptMsg();
        acceptMsg.setMsgType(PaxosMsgTypeEnum.PAXOS_ACCEPT);
        acceptMsg.setInstanceID(getInstance().getInstanceId());
        acceptMsg.setNodeID(getCurNodeInfo().getNodeID());
        acceptMsg.setProposalID(proposerInfo.getCurProposalID());
        acceptMsg.setProposalDec(proposerInfo.getProposalValue());

        msgCounter.startNewRound();

        // 添加accept超时定时器
        timer.addTimer(Instance.ACCEPT_TIMER_ID, (id) -> accept(), ACCEPT_TIMEOUT, TimeUnit.SECONDS);

        logger.info("Send prepare msg, instance: {}, nodeID: {}, proposalID: {}, proposal value: {}", acceptMsg.getInstanceID(), acceptMsg.getNodeID(), acceptMsg.getProposalID(), acceptMsg.getProposalDec());
        // 广播accept消息
        broadcastMessage(acceptMsg.getMsgJson(), PaxosMsgTypeEnum.PAXOS_ACCEPT);
    }

    /**
     * 收到accept回复
     *
     * @param paxosMsg
     */
    public void onAcceptReply(AcceptReplayMsg paxosMsg) {
        // 当前已不在Accept阶段
        // 1. 某个节点响应过慢，提案已完成
        // 2. 多个节点响应过慢，提案已重新进入Prepare阶段
        // 3. 整个提案响应过慢，提案已终止
        if (proposerState != ProposerState.ACCEPTING) {
            logger.info("Cur proposer is not in ACCEPTING State");
            return;
        }

        // 不是当前提议编号，不处理
        if (paxosMsg.getProposalID() != proposerInfo.getCurProposalID()) {
            logger.info("Cur proposer ID: {} != Recv proposal ID: {}", proposerInfo.getCurProposalID(), paxosMsg.getProposalID());
            return;
        }

        msgCounter.addReceive(paxosMsg.getNodeID());

        if (paxosMsg.isOk()) {
            logger.info("Accept ok, accept ID: {}", paxosMsg.getProposalID());
            msgCounter.addPromiseOrAccept(paxosMsg.getNodeID());
        } else {
            logger.info("Accept reject, reject by proposal ID: {}", paxosMsg.getRejectByProposalID());
            msgCounter.addReject(paxosMsg.getNodeID());
            wasRejectedBySomeOne = true;
            proposerInfo.updateHighestOtherProposalID(paxosMsg.getRejectByProposalID());
        }

        if (msgCounter.isPassedOnThisRound()) {
            // 本轮提议完成
            logger.info("Accept passed on this round");
            exitAccept();
        } else {
            // 重新提交
            logger.info("Accept reject on this round, retry");
            timer.addTimer(Instance.ACCEPT_TIMER_ID, (id) -> accept(), Proposer.ACCEPT_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    /**
     * 开始新一轮的提议
     */
    public void newRound() {
        logger.info("New Round");
        exitPrepare();
        exitAccept();
        msgCounter.startNewRound();
        proposerInfo.reset();
        proposerState = ProposerState.INITIAL;
    }

    /**
     * 结束prepare流程
     */
    private void exitPrepare() {
        timer.cancelTimer(Instance.PREPARE_TIMER_ID);
    }

    /**
     * 结束accept流程
     */
    private void exitAccept() {
        timer.cancelTimer(Instance.ACCEPT_TIMER_ID);
    }
}