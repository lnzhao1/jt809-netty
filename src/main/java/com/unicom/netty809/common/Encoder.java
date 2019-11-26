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

import java.io.ByteArrayOutputStream;

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
        //--------------数据头----------
        buffer.writeInt(buffer.capacity()+2);  //4 加头尾
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
        byte[] bytes = buffer.array();
        byte[] formatedBytes = null;
        try {
            formatedBytes = doEscape4Receive(bytes,0,bytes.length);
        }catch (Exception e){
            logger.info(e.getMessage());
        }
        ChannelBuffer finalBuffer = ChannelBuffers.dynamicBuffer(formatedBytes.length+2);
        finalBuffer.writeByte(Message.MSG_HEAD);//1
        finalBuffer.writeBytes(formatedBytes);
        finalBuffer.writeByte(Message.MSG_TAIL);  //1
        String sendData = HexStringUtils.toHexString(finalBuffer.array());
        logger.info("sendData --- " + sendData);
        return finalBuffer;
    }

    /**
     * 报文转义
     * void
     *
     * @param bs
     * @param
     */
    public byte[] doEscape4Receive(byte[] bs, int start, int end) throws Exception {
        if (start < 0 || end > bs.length){
            throw new ArrayIndexOutOfBoundsException("doEscape4Receive error : index out of bounds(start=" + start
                    + ",end=" + end + ",bytes length=" + bs.length + ")");
        }
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            for (int i = start; i < end ; i++) {
                if (bs[i] == 0x5B) {
                    baos.write(0x5A);
                    baos.write(0x01);
                } else if (bs[i] == 0x5A) {
                    baos.write(0x5A);
                    baos.write(0x02);
                } else if(bs[i] == 0x5D){
                    baos.write(0x5E);
                    baos.write(0x01);
                }else if(bs[i] == 0x5E){
                    baos.write(0x5E);
                    baos.write(0x02);
                }else{
                    baos.write(bs[i]);
                }
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw e;
        } finally {
            if (baos != null) {
                baos.close();
                baos = null;
            }
        }
    }
}
