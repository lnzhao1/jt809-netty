package com.unicom.netty809.common;

import com.unicom.netty809.util.HexStringUtils;
import com.unicom.netty809.util.Util;
import com.unicom.netty809.vomodel.Message;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 报文编码
 */
public class Encoder extends SimpleChannelHandler{

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception
    {
        ChannelBuffer buffer = buildMessage((Message)e.getMessage());
        if (buffer != null) {
            Channels.write(ctx, e.getFuture(), buffer);
        }
    }

    private ChannelBuffer buildMessage(Message msg)
    {
        int bodyLength = msg.getMsgBody().capacity();
        ChannelBuffer buffer = ChannelBuffers.buffer(bodyLength + Message.MSG_FIX_LENGTH);
        buffer.writeByte(Message.MSG_HEAD);  //1
        //--------------数据头----------
        buffer.writeInt(buffer.capacity());  //4
        buffer.writeInt(msg.getMsgSn());     //4
        buffer.writeShort(msg.getMsgId());   //2
        buffer.writeInt(1); //4
        buffer.writeBytes(msg.getVersionFlag());//3
        buffer.writeByte(0);//1
        buffer.writeInt(20000000);//4
        //--------------数据体----------
        buffer.writeBytes(msg.getMsgBody());
        //------------crc校验码---------
        byte[] b = ChannelBuffers.buffer(bodyLength + 22).array();
        buffer.getBytes(1, b);
        int crcValue = Util.crc16(b);
        buffer.writeShort(crcValue);//2
        buffer.writeByte(Message.MSG_TAIL);//1
        String sendData = HexStringUtils.toHexString(buffer.array());
        logger.info("sendData --- " + sendData);
        return buffer;
    }

}
