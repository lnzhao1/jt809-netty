package com.unicom.netty809.client;

import com.unicom.netty809.util.IpUtils;
import com.unicom.netty809.util.Util;
import com.unicom.netty809.vomodel.Message;
import com.unicom.netty809.vomodel.UpExgMsgRealLocationEntity;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;


public class TcpClientDemo {

    private static Logger LOGGER = LoggerFactory.getLogger(TcpClientDemo.class);

    /**
     * 交委指定本公司接入码
     */
    public static int PLANT_CODE;
    /**
     * 交委指定本公司用户名
     */
    public static int COM_ID;
    /**
     * 交委指定本公司密码
     */
    public static String COM_PWD;
    public static String LONGINSTATUS = "";
    public static String LOGINING = "logining";
    private static int LOGIN_FLAG = 0;
    private static String DOWN_LINK_IP = "127.0.0.1";

    //初始化基类
    private static TcpClient809 tcpClient = TcpClient809.getInstence();
    //初始化
    private static TcpClientDemo tcpClientDemo = new TcpClientDemo();

    //初始化channel,以便心跳机制及时登录
    private Channel channel = tcpClient.getChannel("127.0.0.1", 10098);


    public static TcpClientDemo getInstance() {
        //获取本机IP对应的用户名密码,IpUtils自己实现一个，就是获取本地IP的，因为有的城市的交委会给每个服务器一个账号密码
        String localIp = IpUtils.getLinuxLocalIp();
        if (StringUtils.isNotBlank(localIp)) {
            String properties = "1:0:0:1";
            if (StringUtils.isNotBlank(localIp)) {
                String[] pros = properties.split(":");
                PLANT_CODE = Integer.parseInt(pros[0]);
                COM_ID = Integer.parseInt(pros[1]);
                COM_PWD = pros[2];
            }
        } else {
            LOGGER.error("获取本机IP异常");
        }
        return tcpClientDemo;
    }

    /**
     * 判断是否登录
     * boolean
     *
     * @return 2016年10月12日 by fox_mt
     */
    public boolean isLogined() {
        return Constants.LOGIN_SUCCESS.equals(LONGINSTATUS);
    }

    /**
     * 登录交委接入平台
     * boolean
     *
     * @return 2016年9月28日 by fox_mt
     */
    public boolean login2Gov() {

        boolean success = false;
        if (!Constants.LOGIN_SUCCESS.equals(LONGINSTATUS) && !LOGINING.equals(LONGINSTATUS)) {
            //开始登录
            Message msg = new Message(JT809Constants.UP_CONNECT_REQ);
            ChannelBuffer buffer = ChannelBuffers.buffer(46);
            buffer.writeInt(COM_ID);//4

            byte[] pwd = getBytesWithLengthAfter(8, COM_PWD.getBytes());
            buffer.writeBytes(pwd);//8

            byte[] ip = getBytesWithLengthAfter(32, DOWN_LINK_IP.getBytes());
            buffer.writeBytes(ip);//32

            buffer.writeShort((short) Constants.TCP_RESULT_PORT);//2
            msg.setMsgBody(buffer);
            channel = tcpClient.getChannel(Constants.TCP_ADDRESS_FUJIAN, Constants.TCP_PORT_FUJIAN);
            channel.write(msg);
            LONGINSTATUS = LOGINING;
        }
        return success;
    }

//    public static Message buildMessage(Message msg) {
//        int bodyLength = 0;
//        if (null != msg.getMsgBody()) {
//            bodyLength = msg.getMsgBody().capacity();
//        }
//        msg.setMsgGesscenterId(PLANT_CODE);
//        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(bodyLength);
//        //---数据体
//        if (null != msg.getMsgBody()) {
//            buffer.writeBytes(msg.getMsgBody());
//        }
//        msg.setMsgLength(bodyLength);
//        return msg;
//    }


    /**
     * 发送数据到交委接入平台
     * boolean
     *
     * @param awsVo
     * @return 2016年9月28日 by fox_mt
     */
    public boolean sendMsg2Gov(UpExgMsgRealLocationEntity awsVo) {
        boolean success = false;
        if (isLogined()) {
            //已经登录成功，开始发送数据
            channel = tcpClient.getChannel(Constants.TCP_ADDRESS_FUJIAN, Constants.TCP_RESULT_PORT);
            if (null != channel && channel.isWritable()) {
                Message msg = buildSendVO(awsVo);
                channel.write(msg);
                LOGGER.info("发送--" + awsVo.toString());
            } else {
                LONGINSTATUS = "";
            }
        } else if (LOGIN_FLAG == 0) {
            LOGIN_FLAG++;
            login2Gov();
            LOGGER.error("--------------第一次登录");
        } else {
            LOGGER.error("--------------等待登录");
        }
        return success;
    }


    /**
     * 转换VO
     * void
     *
     * @param awsVo 2016年9月28日 by fox_mt
     */
    private Message buildSendVO(UpExgMsgRealLocationEntity awsVo) {
        Message msg = new Message(JT809Constants.UP_EXG_MSG);
        ChannelBuffer buffer = ChannelBuffers.buffer( 64);//数据体总长
        buffer.writeBytes(getBytesWithLengthAfter(21, awsVo.getVehicleNo().getBytes(Charset.forName("GBK"))));//21
        buffer.writeByte(1);//1
        buffer.writeShort(JT809Constants.UP_EXG_MSG_REAL_LOCATION);//2
        buffer.writeInt(36);//4
        //是否加密
        buffer.writeByte((byte) 0);//0未加密 // 1
        //日月年dmyy
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        buffer.writeByte((byte) cal.get(Calendar.DATE));
        buffer.writeByte((byte) (cal.get(Calendar.MONTH) + 1));
        String hexYear = "0" + Integer.toHexString(cal.get(Calendar.YEAR));
        buffer.writeBytes(hexStringToByte(hexYear));//4

        //时分秒
        buffer.writeByte((byte) cal.get(Calendar.HOUR_OF_DAY));
        buffer.writeByte((byte) cal.get(Calendar.MINUTE));
        buffer.writeByte((byte) cal.get(Calendar.SECOND));//3
        //经度，纬度
        buffer.writeInt(formatLonLat(Double.valueOf(awsVo.getLon())));//4
        buffer.writeInt(formatLonLat(Double.valueOf(awsVo.getLat())));//4
        //速度
        buffer.writeShort(awsVo.getVec1());//2
        //行驶记录速度
        buffer.writeShort(awsVo.getVec2());//2
        //车辆当前总里程数
        buffer.writeInt((int)awsVo.getVec3());//4
        //方向
        buffer.writeShort(awsVo.getDirection());//2
        //海拔
        buffer.writeShort((short) 0);//2
        //车辆状态
        int accStatus = 0;
        int gpsStatus = 0;
        if (accStatus == 0 && gpsStatus == 0) {
            buffer.writeInt(0);//4
        } else if (accStatus == 1 && gpsStatus == 0) {
            buffer.writeInt(1);//4
        } else if (accStatus == 0 && gpsStatus == 1) {
            buffer.writeInt(2);//4
        } else {
            buffer.writeInt(3);//4
        }
        //报警状态
        buffer.writeInt(1);//0表示正常；1表示报警//4
        msg.setMsgBody(buffer);
        return msg;
    }


    /**
     * 16进制字符串转换成byte数组
     * byte[]
     *
     * @param hex
     * @return 2016年10月12日 by fox_mt
     */
    public static byte[] hexStringToByte(String hex) {
        hex = hex.toUpperCase();
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    /**
     * 格式化经纬度,保留六位小数
     * int
     *
     * @param needFormat
     * @return 2016年10月12日 by fox_mt
     */
    private int formatLonLat(Double needFormat) {
        NumberFormat numFormat = NumberFormat.getInstance();
        numFormat.setMaximumFractionDigits(6);
        numFormat.setGroupingUsed(false);
        String fristFromat = numFormat.format(needFormat);
        Double formatedDouble = Double.parseDouble(fristFromat);
        numFormat.setMaximumFractionDigits(0);
        String formatedValue = numFormat.format(formatedDouble * 1000000);
        return Integer.parseInt(formatedValue);
    }

    /**
     * 补全位数不够的定长参数
     * byte[]
     *
     * @param length
     * @param pwdByte
     * @return 2016年10月12日 by fox_mt
     */
    private byte[] getBytesWithLengthAfter(int length, byte[] pwdByte) {
        byte[] lengthByte = new byte[length];
        for (int i = 0; i < pwdByte.length; i++) {
            lengthByte[i] = pwdByte[i];
        }
        for (int i = 0; i < (length - pwdByte.length); i++) {
            lengthByte[pwdByte.length + i] = 0x00;
        }
        return lengthByte;
    }


    public static void main(String[] args) {
        TcpClientDemo s = TcpClientDemo.getInstance();
        UpExgMsgRealLocationEntity awsVo = new UpExgMsgRealLocationEntity();
        awsVo.setDirection(120);
        awsVo.setLon("117.2900911");
        awsVo.setLat("39.56362");
        awsVo.setVec1(45);
        awsVo.setAlarm(10001L);
        awsVo.setVehicleNo("XXXXX");
        s.sendMsg2Gov(awsVo);
        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        s.sendMsg2Gov(awsVo);
    }

}