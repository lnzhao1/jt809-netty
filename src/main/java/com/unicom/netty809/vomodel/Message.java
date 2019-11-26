package com.unicom.netty809.vomodel;

import lombok.Data;
import org.jboss.netty.buffer.ChannelBuffer;

@Data
public class Message {
    public static final int MSG_HEAD = 0x5b;
    public static final int MSG_TAIL = 0x5d;

    //报文中除数据体外，固定的数据长度(减去头尾)
    public static final int MSG_FIX_LENGTH = 24;

    //报文序列号，自增。
    private static int internalMsgNo = 0;
    private long msgLength, encryptFlag = 1, msgGesscenterId, encryptKey;
    private int crcCode, msgId, msgSn;
    private ChannelBuffer msgBody;
    private byte[] versionFlag = {0, 0, 1};
    private String receiveData;

    //下行报文标识，值为1时，代表发送的数据；默认为0，代表接收的报文
    //private int downFlag = 0;

    public Message() {
    }

    public Message(int msgId) {
        //下行报文需要填充报文序列号
        synchronized ((Integer) internalMsgNo) {
            if (internalMsgNo == Integer.MAX_VALUE) {
                internalMsgNo = 0;
            }
        }
        this.msgSn = ++internalMsgNo;
        this.msgId = msgId;
        //this.downFlag = 1;
    }

}
