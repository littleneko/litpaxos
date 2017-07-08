package org.littleneko.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by little on 2017-06-14.
 */
public class TransMsgDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 小于头长度
        if (in.readableBytes() < TransMsgHead.TRANS_MSG_HEAD_LENGTH) {
            //System.out.printf("readableBytes < 8, readableBytes: %d\n", byteBuf.readableBytes());
            return;
        }

        // 标记一下当前的readIndex的位置
        in.markReaderIndex();

        int version = in.readShort();
        int msgType = in.readShort();
        int id = in.readInt();
        int length = in.readInt();

        if (length < 0) {
            ctx.close();
        }

        // 读到的消息体长度如果小于我们传送过来的消息长度，则resetReaderIndex. 这个配合markReaderIndex使用的。把readIndex重置到mark的地方
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        byte[] body = new byte[length];
        in.readBytes(body);

        TransMsgHead transMsgHead = new TransMsgHead(version, PaxosMsgTypeEnum.valueOfId(msgType), id, length);
        TransMsg transMsg = new TransMsg(transMsgHead, body);

        out.add(transMsg);
    }
}
