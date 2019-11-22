package com.unicom.netty809.server;
import com.unicom.netty809.common.CommonSession;
import com.unicom.netty809.common.SessionUtil;
import com.unicom.netty809.util.ChannelUtils;
import com.unicom.netty809.vomodel.Login809;
import com.unicom.netty809.vomodel.Message;
import com.unicom.netty809.vomodel.UpExgMsgRealLocationEntity;
import com.unicom.netty809.vomodel.UpWarningMsgEntity;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BusiHandler extends SimpleChannelHandler
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private String developer;

    private SessionUtil instance  = SessionUtil.getInstance();

//    private AmqpTemplate rabbitTemplate = (AmqpTemplate) SpringApplicationUtil.getBean("rabbitTemplate");


    public BusiHandler(String developer)
    {
        this.developer = developer;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info("channelConnected  : ------------------------------------------channelConnected : "  + developer);
        Channel channel = ctx.getChannel();
        //链接后如果有会话则直接提取会话加入会话链，入没有新建之后加入
        if(instance.commonSessionMap.containsKey(developer)){
            CommonSession commonSession = instance.commonSessionMap.get(developer);// key:ip value:channel
            commonSession.addChannel(channel);
        }else{
            CommonSession commonSession = new CommonSession(developer);
            commonSession.addChannel(channel);
            instance.commonSessionMap.put(developer,commonSession);
        }
        Map<String,CommonSession> commonSessionMap = instance.commonSessionMap;
        for(String developerKey : commonSessionMap.keySet()){
            logger.info("developer  : ---------------------------------------------------------  " + developerKey);
            CommonSession commonSession = commonSessionMap.get(developerKey);
            for(String ipKey : commonSession.getDeveloperSessionHashMap().keySet()){
                logger.info("ipKey  : ---------------------------------------------------------  " + ipKey);
                logger.info("channel  : ---------------------------------------------------------  " + commonSession.getDeveloperSessionHashMap().get(ipKey));
            }
        }
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info("channelDisconnected  : ------------------------------------------channelDisconnected");
        Channel channel = ctx.getChannel();
        String ip = ChannelUtils.getIp(channel);
        if(instance.commonSessionMap.containsKey(developer)){
            if(instance.commonSessionMap.get(developer).getDeveloperSessionHashMap().size()>1){
                instance.commonSessionMap.get(developer).getDeveloperSessionHashMap().remove(ip);
            }else{
                instance.removeBySessionId(developer);
            }
        }
        logger.debug("终端断开连接:{}", ip);
        ctx.getChannel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.error("发生异常:{}", e.getCause().getMessage());
        e.getCause().printStackTrace();
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception
    {
        Message msg = (Message)e.getMessage();
        if( msg!=null){
            this.logger.info("-----------------------------------------------------------------------------------msg.getMsgId():"+Integer.valueOf(msg.getMsgId()));
            switch (msg.getMsgId())
            {
                case 0x1001:
                    //暂时用的都是主链路进行信息交换
                    logger.info("主链路登录请求消息。",msg);
                    //接收客户端登录信息存到数据库
                    login(msg, ctx, e);
                    break;
                case 0x1005:
                    logger.info("主链路连接保持请求消息。",msg.toString());
                    //接收客户端位置连接心跳信息并返回空应答信息维持连接
                    heartBeat(msg, ctx, e);
                    break;
                case 0x1200:
                    logger.info("主链路动态信息交换信息",msg.toString());
                    //接收车辆实时定位报文信息存到数据库
                    vehicleRTLS(msg, ctx, e);
                    break;
                case 0x1400:
                    logger.info("主动上报报警信息",msg.toString());
                    //接收视频启动/停止应答报文信息存到数据库
                    upWarningMsg(msg, ctx,e);
                    break;
                default:
                    logger.info("其他消息。",msg.toString());
                    break;
            }
        }
    }

    private void vehicleRTLS(Message msg, ChannelHandlerContext ctx, MessageEvent e)
    {
        String vehicleNo = dealEmpty(new String(msg.getMsgBody().readBytes(21).array(), Charset.forName("GBK")));
        byte vehicleColor = msg.getMsgBody().readByte();
        int dataType = msg.getMsgBody().readUnsignedShort();
        long dataLength = msg.getMsgBody().readUnsignedInt();
        this.logger.info("子业务id dataType:" +  dataType);
        switch (dataType)
        {
            //实时车辆定位
            case 0x1202:
                this.logger.info("实时上传车辆定位信息 :" +  msg.toString());
                ChannelBuffer childBody = msg.getMsgBody().readBytes((int)dataLength);
                //储存车辆实时定位信息
                upExgMsgRealLocation(msg, e, vehicleNo, vehicleColor, dataType, dataLength, childBody);
                break;
            default:
                this.logger.info("其他消息 :" +  msg.toString());
        }
    }

    /**
     * 储存车辆实时定位信息
     * @param msg
     * @param e
     * @param vehicleNo
     * @param vehicleColor
     * @param dataType
     * @param dataLength
     * @param childBody
     */
    private void upExgMsgRealLocation(Message msg, MessageEvent e, String vehicleNo, byte vehicleColor, int dataType, long dataLength, ChannelBuffer childBody)
    {
        byte encrypt = childBody.readByte();
        byte day = childBody.readByte();
        byte month = childBody.readByte();
        int year = childBody.readUnsignedShort();
        byte hour = childBody.readByte();
        byte min = childBody.readByte();
        byte sec = childBody.readByte();
        long lon = childBody.readUnsignedInt();
        String lonStr = String.valueOf(lon).substring(0, 3) + "." + String.valueOf(lon).substring(3, String.valueOf(lon).length());
        long lat = childBody.readUnsignedInt();
        String latStr = String.valueOf(lat).substring(0, 2) + "." + String.valueOf(lat).substring(2, String.valueOf(lat).length());
        int vec1 = childBody.readUnsignedShort();
        int vec2 = childBody.readUnsignedShort();
        long vec3 = childBody.readUnsignedInt();
        int direction = childBody.readUnsignedShort();
        int altttude = childBody.readUnsignedShort();
        long state = childBody.readUnsignedInt();
        long alarm = childBody.readUnsignedInt();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateTime = null;
        try
        {
            dateTime = sdf.parse(year + "-" + month + "-" + day + " " + hour + ":" + min + ":" + sec);
        }
        catch (ParseException e1)
        {
            e1.printStackTrace();
        }
        UpExgMsgRealLocationEntity upExgMsgRealLocation = new UpExgMsgRealLocationEntity();
        upExgMsgRealLocation.setYear(year);
        upExgMsgRealLocation.setMonth(month);
        upExgMsgRealLocation.setDay(day);
        upExgMsgRealLocation.setHour(hour);
        upExgMsgRealLocation.setMin(min);
        upExgMsgRealLocation.setSec(sec);
        upExgMsgRealLocation.setMsgLength(msg.getMsgLength());
        upExgMsgRealLocation.setMsgSn(msg.getMsgSn());
        upExgMsgRealLocation.setMsgId(msg.getMsgId());
        upExgMsgRealLocation.setMsgGnsscenterId(msg.getMsgGesscenterId());
        upExgMsgRealLocation.setVersionFlag(new String(msg.getVersionFlag(), Charset.forName("GBK")));
        upExgMsgRealLocation.setEncryptFlag((int)msg.getEncryptFlag());
        upExgMsgRealLocation.setEncryptKey(msg.getEncryptKey());

        upExgMsgRealLocation.setVehicleNo(vehicleNo);
        upExgMsgRealLocation.setVehicleColor(vehicleColor);
        upExgMsgRealLocation.setDataType(dataType);
        upExgMsgRealLocation.setDataLength(dataLength);

        upExgMsgRealLocation.setEncrypt(encrypt);
        upExgMsgRealLocation.setDateTime(dateTime);
        upExgMsgRealLocation.setLon(lonStr);
        upExgMsgRealLocation.setLat(latStr);
        upExgMsgRealLocation.setVec1(vec1);
        upExgMsgRealLocation.setVec2(vec2);
        upExgMsgRealLocation.setVec3(vec3);
        upExgMsgRealLocation.setDirection(direction);
        upExgMsgRealLocation.setAltttude(altttude);
        upExgMsgRealLocation.setState(state);
        upExgMsgRealLocation.setAlarm(alarm);
        upExgMsgRealLocation.setDeveloper(this.developer);
//        this.rabbitTemplate.convertAndSend("vehicle_RTLS_809", JSON.toJSONString(upExgMsgRealLocation));
        this.logger.info("实时上传车辆定位信息 " + upExgMsgRealLocation.toString());
        //消息应答报文回复接收成功
        Message msgRep = new Message(4102);
        ChannelBuffer buffer = ChannelBuffers.buffer(0);
        msgRep.setMsgBody(buffer);
        ChannelFuture f = e.getChannel().write(msgRep);
    }

    private void login(Message msg, ChannelHandlerContext ctx, MessageEvent e)
    {
        int userId = msg.getMsgBody().readInt();
        String passWord = msg.getMsgBody().readBytes(8).toString(Charset.forName("GBK"));
        String ip = msg.getMsgBody().readBytes(32).toString(Charset.forName("GBK"));
        int port = msg.getMsgBody().readUnsignedShort();
        msg.getMsgBody().clear();
        Login809 login809 = new Login809();
        login809.setDeveloper(this.developer);
        login809.setUserId(String.valueOf(userId));
        login809.setPassWord(passWord);
        login809.setIp(ip);
        login809.setPort(String.valueOf(port));
//        this.rabbitTemplate.convertAndSend("Login_809", JSON.toJSONString(login809));
        //返回时按照规定 按照16进制转换10进制 设置应答信息
        Message msgRep = new Message(4098);
        ChannelBuffer buffer = ChannelBuffers.buffer(5);
        buffer.writeByte(0);

        buffer.writeInt(1111);
        msgRep.setMsgBody(buffer);
        ChannelFuture f = e.getChannel().write(msgRep);
    }

    /**
     * 接收客户端位置连接心跳信息并返回空应答信息维持连接
     * @param msg
     * @param ctx
     * @param e
     */
    private void heartBeat(Message msg, ChannelHandlerContext ctx, MessageEvent e)
    {
        Message msgRep = new Message(4102);
        ChannelBuffer buffer = ChannelBuffers.buffer(0);
        msgRep.setMsgBody(buffer);
        ChannelFuture f = e.getChannel().write(msgRep);
    }


    /**
     * 处理空字符串
     * @param str
     * @return
     */
    public String dealEmpty(String str){
        Pattern pattern = Pattern.compile("([^\u0000]*)");
        Matcher matcher = pattern.matcher(str);
        if(matcher.find(0)){
            try {
                str = new String(matcher.group(1).getBytes("utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return str;
    }
    /**
     * description: 处理报警信息
     * @Param msg:
     * @Param ctx:
     * @Param e:
     * @return void:
     * @author: zhaoxiao
     * @createTime: 2019/8/9 14:09
    */
    private void  upWarningMsg(Message msg, ChannelHandlerContext ctx, MessageEvent e){
        //车牌号
        String vehicleNo = dealEmpty(new String(msg.getMsgBody().readBytes(21).array(), Charset.forName("GBK")));
        byte vehicleColor = msg.getMsgBody().readByte();
        int dataType = msg.getMsgBody().readUnsignedShort();
        long dataLength = msg.getMsgBody().readUnsignedInt();
        switch (dataType)
        {
            case 0x1402:
                this.logger.info("实时上传报警信息 :" +  msg.toString());
                ChannelBuffer childBody = msg.getMsgBody().readBytes((int)dataLength);
                //储存车辆实时定位信息
                transferUpWarningMsg(msg, e, vehicleNo, vehicleColor, dataType, dataLength, childBody);
                break;
            default:
                this.logger.info("其他消息 :" +  msg.toString());
        }
    }
    private void  transferUpWarningMsg(Message msg, MessageEvent e, String vehicleNo, byte vehicleColor, int dataType, long dataLength, ChannelBuffer childBody){
        byte  warnSrc =  childBody.readByte();
        int  warnType =  childBody.readUnsignedShort();
        long  warnTime =  childBody.readLong();
        String infoId = new String(childBody.readBytes(32).array());//报警id
        byte driverLength = childBody.readByte();
        String driver = dealEmpty(new String(childBody.readBytes(driverLength).array(), Charset.forName("GBK")));//驾驶员
        byte  driverNoLength = childBody.readByte();
        String driverNo = new String(childBody.readBytes(driverNoLength).array());//驾驶证id
        byte level = childBody.readByte();
        long lng = childBody.readUnsignedInt();
        String lngStr = String.valueOf(lng).substring(0, 3) + "." + String.valueOf(lng).substring(3);
        long lat = childBody.readUnsignedInt();
        String latStr = String.valueOf(lat).substring(0, 2) + "." + String.valueOf(lat).substring(2);
        int altitude = childBody.readUnsignedShort();
        int vec1  = childBody.readUnsignedShort();
        int vec2  = childBody.readUnsignedShort();
        byte status  = childBody.readByte();
        int direction  = childBody.readUnsignedShort();
        int infoLength  = childBody.readUnsignedShort();
        String infoContent  = dealEmpty(new String(childBody.readBytes(infoLength).array(), Charset.forName("GBK")));//报警内容
        UpWarningMsgEntity upWarningMsgEntity = new UpWarningMsgEntity();
        upWarningMsgEntity.setInfoId(infoId);
        upWarningMsgEntity.setVehicleNo(vehicleNo);
        upWarningMsgEntity.setVehicleColor(vehicleColor);
        upWarningMsgEntity.setDataType(dataType);
        upWarningMsgEntity.setDataLength(dataLength);
        upWarningMsgEntity.setWarnSrc(warnSrc);
        upWarningMsgEntity.setWarnType(warnType);
        upWarningMsgEntity.setWarnTime(warnTime);
        upWarningMsgEntity.setDriverLength(driverLength);
        upWarningMsgEntity.setDriver(driver);
        upWarningMsgEntity.setDriverNoLength(driverNoLength);
        upWarningMsgEntity.setDriverNo(driverNo);
        upWarningMsgEntity.setLevel(level);
        upWarningMsgEntity.setLng(lngStr);
        upWarningMsgEntity.setLat(latStr);
        upWarningMsgEntity.setAltitude(altitude);
        upWarningMsgEntity.setVec1(vec1);
        upWarningMsgEntity.setVec2(vec2);
        upWarningMsgEntity.setStatus(status);
        upWarningMsgEntity.setDirection(direction);
        upWarningMsgEntity.setInfoLength(infoLength);
        upWarningMsgEntity.setInfoContent(infoContent);
//        this.rabbitTemplate.convertAndSend("warning_rtls_809", JSON.toJSONString(upWarningMsgEntity));
        this.logger.info("实时上传报警信息 " + upWarningMsgEntity.toString());
        //消息应答报文回复接收成功
        Message msgRep = new Message(4102);
        ChannelBuffer buffer = ChannelBuffers.buffer(0);
        msgRep.setMsgBody(buffer);
        ChannelFuture f = e.getChannel().write(msgRep);
    }
}
