package com.unicom.netty809.common;


import com.unicom.netty809.util.HexStringUtils;
import com.unicom.netty809.vomodel.Message;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

/**
 * 报文解码
 */
public class Decoder extends FrameDecoder{

    private Logger log = LoggerFactory.getLogger(getClass());

    private String developer;

    public Decoder(String developer) {
        super();
        this.developer = developer;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {

//        log.info("进入rocoder 缓存长度 : " + buffer.readableBytes());
        int head = buffer.getByte(buffer.readerIndex());
        log.info("头：>>>>>>"+head);
        //缓存长度大于1000 异常数据
        if (buffer.readableBytes() > 1000) {
        log.info("字节长度："+buffer.readableBytes());
            buffer.skipBytes(buffer.readableBytes());
            return null;
        }
        log.info("buffer.readerIndex() UP;"+buffer.readerIndex());
        log.info("buffer.writerIndex() UP;"+buffer.writerIndex());
        //非头部直接过
        if(head != Message.MSG_HEAD){
            log.info("头："+head+"未识别头标识5b 查找5b 原始总长度 " + buffer.readableBytes());
            buffer.markReaderIndex();
            boolean jumpflag = false;
            for (int i=buffer.readerIndex();i<buffer.readableBytes();i++){
                int currentHead = buffer.getByte(i);
                if(currentHead == Message.MSG_HEAD){
                    buffer.skipBytes(i);
                    log.info("找出识别头标识5b 跳过多余字节: i：" + i);
                    jumpflag = true;
                    break;
                }
            }
            if(!jumpflag){
                buffer.resetReaderIndex();
                return  null;
            }

        }
        return this.buildMessage(buffer);
    }


    private Message buildMessage(ChannelBuffer buffer){
        log.info("buffer.readableBytes() ;"+buffer.readableBytes());
        log.info("buffer.capacity() ;"+buffer.capacity());
        log.info("buffer.writerIndex() ;"+buffer.writerIndex());
        log.info("buffer.readerIndex() ;"+buffer.readerIndex());
        //截取字节尾
        int tail = buffer.getByte(buffer.capacity()-1);
        log.info("buffer.writerIndex()-buffer.readerIndex() 新数组长度: "+(buffer.writerIndex()-buffer.readerIndex()));
        //新建所有字节数量的数组
        byte[] array =	new byte[buffer.writerIndex()-buffer.readerIndex()];
        //把所有字节赋给array
        buffer.getBytes(buffer.readerIndex(), array);
        //转换进制
        String receiveData = HexStringUtils.toHexString(array);
        log.info("receive data : " + receiveData);
        //数据长度标识数组
        byte[] msgLengthBytes =new byte[4];
        //如字节流长度小于4字节则直接返回；
        if(buffer.readableBytes()<5){
            return null;
        }
        byte[] arrayfixed = null;
        try {
            arrayfixed =  doEscape4Receive(array,0,array.length);
        }catch (Exception e ){
            log.info("----转换错误----");
        }
        //复制array给msgLengthBytes从原数组的第一位开始，目标数组从0开始长度为4个
        System.arraycopy(arrayfixed,1,msgLengthBytes,0,4);
        //标记长度转换为int
        int msgLength = new BigInteger(msgLengthBytes).intValue();
        log.info("数据长度 : " + msgLength);
        int readLength;//读取长度
        //标记数据长度大于数据实际长度（半包）
        if(msgLength>arrayfixed.length){
            log.info(" tail ======>>>>>>  " + tail);
            log.info("array lastchar :"+arrayfixed[arrayfixed.length-1]);
            log.info("array.length :"+arrayfixed.length);
            if( tail == Message.MSG_TAIL){//如果结尾以5d结尾说明，数据有问题
                readLength = array.length;
                log.info("半包读完");
                buffer.readBytes(readLength);//读取了所有数据长度，过掉当前一条
                return null;
            }else{
                //如果非 5d结尾结束
                log.info("半包未读");
                return null;//过掉，继续下一次读取
            }
        //标记数据长度小于或等于数据实际长度（粘豆包）
        }else{
//            if(developer.equals("xingtu")){//如果数据为星途的则读取到第一个标记符5d
                readLength = getFirstMatchingIndex(array,(byte) Message.MSG_TAIL)+1;
                log.info("Message.MSG_TAIL xingtu: " + readLength);
                if(readLength==0){
                    log.info("结尾非5d，且数据给定长度小于总长度");
                    buffer.readBytes(buffer.readableBytes());
                    return  null;
                }else {
                    log.info("成功读取数据长度==="+readLength);
                    buffer.readBytes(readLength);
                    log.info("buffer.readerIndex() DOWN;"+buffer.readerIndex());
                    log.info("buffer.writerIndex() DOWN;"+buffer.writerIndex());
                }
        }
        //包装转码后的数据体
        Message msg = new Message();
        try {
//      array =  doEscape4Receive(array,0,array.length);
        ChannelBuffer  content = ChannelBuffers.copiedBuffer(arrayfixed);
            //1 byte 头5b
        content.skipBytes(1);
        //4 byte 数据长度
        msg.setMsgLength(content.readUnsignedInt());
        //4 byte  报文序列号
        msg.setMsgSn(content.readInt());
        //2 byte  主业务类型
        msg.setMsgId(content.readUnsignedShort());
        //4 byte 下级平台接入码
        msg.setMsgGesscenterId(content.readUnsignedInt());
        //3 byte 版本号 v x.x.x
        msg.setVersionFlag(content.readBytes(3).array());
        //1 byte 是否加密 0 不加密 1 加密
        msg.setEncryptFlag(content.readUnsignedByte());
        //4 byte 数据加密钥
        msg.setEncryptKey(content.readUnsignedInt());
        //数据体为变长字节 去掉校验码与尾标识的数据体
        ChannelBuffer bodyBytes = content.readBytes(content.readableBytes()-2-1);
        //解密
        if(msg.getEncryptFlag() == 1L){
            byte[] dataByte =  encrypt('A','B','C',msg.getEncryptKey(),bodyBytes.array());
            bodyBytes = ChannelBuffers.copiedBuffer(dataByte);
        }
        msg.setMsgBody(bodyBytes);
        //校验码
        msg.setCrcCode(content.readUnsignedShort());
        //跳过尾标识
        content.skipBytes(1);
        log.info("---------------------------------buildMessage" + msg.toString());
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Decode Error jump out");
            return null;
        }
        return msg;
    }

    /**
     * 接收消息时转义<br>
     *
     * <pre>
     * 0x5A01 <====> 0x5B
     * 0x5A02 <====> 0x5A
     * 0x5E01 <====> 0x5D
     * 0x5E02 <====> 0x5E
     * </pre>
     *
     * @param bs
     *            要转义的字节数组
     * @param start
     *            起始索引
     * @param end
     *            结束索引
     * @return 转义后的字节数组
     * @throws Exception
     */
    public byte[] doEscape4Receive(byte[] bs, int start, int end) throws Exception {
        if (start < 0 || end > bs.length)
            throw new ArrayIndexOutOfBoundsException("doEscape4Receive error : index out of bounds(start=" + start
                    + ",end=" + end + ",bytes length=" + bs.length + ")");
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            for (int i = 0; i < start; i++) {
                baos.write(bs[i]);
            }
            for (int i = start; i < end - 1; i++) {
                if (bs[i] == 0x5A && bs[i + 1] == 0x01) {
                    baos.write(0x5B);
                    i++;
                } else if (bs[i] == 0x5A && bs[i + 1] == 0x02) {
                    baos.write(0x5A);
                    i++;
                } else if(bs[i] == 0x5E && bs[i + 1] == 0x01){
                    baos.write(0x5D);
                    i++;
                }else if(bs[i] == 0x5E && bs[i + 1] == 0x02){
                    baos.write(0x5E);
                    i++;
                }else{
                    baos.write(bs[i]);
                }
            }
            for (int i = end - 1; i < bs.length; i++) {
                baos.write(bs[i]);
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


    /**
     * Returns the index within the given input string of the first occurrence
     * of the specified substring.
     *
     */
    public static int getFirstMatchingIndex(byte[] bytes, byte query) {
        int inputLength = bytes.length;
        int inputIndex = 0;
        int index = -1;
        while (inputIndex < inputLength) {
            if(bytes[inputIndex] == query){
                return inputIndex;
            }else{
                inputIndex++;
            }
        }
      return   index;
    }
    //加密，解密 执行第一次加密 第二次变回原值解密
    public static byte[] encrypt(int M1,int IA1,int IC1,long key,byte [] data) {
        if(data == null) return null;
        byte[] array = data;//使用原对象，返回原对象
        //byte[] array = new byte[data.length]; //数组复制 返回新的对象
        //System.arraycopy(data, 0, array, 0, data.length);
        int idx=0;
        if(key==0){
            key=1;
        }
        int mkey = M1;
        if (0 == mkey ) {
            mkey = 1;
        }
        while(idx<array.length){
            key = IA1 * ( key % mkey ) + IC1;
            array[idx]^=((key>>20)&0xFF);
            idx++;
        }
        return array;
    }
}
