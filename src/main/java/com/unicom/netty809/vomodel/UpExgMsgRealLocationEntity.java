package com.unicom.netty809.vomodel;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;

/**
 * Created by liujiawei on 2019-03-11.
 * 车辆实时定位信息类
 */
@Data
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class UpExgMsgRealLocationEntity implements Serializable{

    /**
     *        主键
    */
    private Long id;

    /**
     *  年月日，时分秒
     */
    private int year;
    private int month;
    private int day;
    private int hour;
    private int min;
    private int sec;

    /**
     *        数据长度
     */
    private long msgLength;

    /**
     *        报文序列号
     */
    private long msgSn;

    /**
     *        业务数据类型
     */
    private int msgId;

    /**
     *        下级平台接入码，上级平台给下级平台分配的唯一标识
     */
    private long msgGnsscenterId;

    /**
     *        协议版本号标识，上下级平台之间采用的标准协议版本编号；
     *        长度为三个字节来表示:0x01 0x02 0x0F 表示的版本号是V1.2.15，以此类推。
     */
    private String versionFlag;

    /**
     *        报文加密标志位：0 表示报文不加密，1 表示报文加密
     */
    private int encryptFlag;

    /**
     *        数据加密的密钥，长度为四个字节
     */
    private long encryptKey;


    /**
     *        车牌号
     */
    private String vehicleNo;

    /**
     *        车牌颜色，按照JT/T 415-2006中5.4.12的规定
     */
    private byte vehicleColor;

    /**
     *        子业务类型标识
     */
    private int dataType;

    /**
     * 后续数据长度
     */
    private long dataLength;


    /**
     *        该字段标识传输的定位信息是否使用国家测绘局批准的地图保密插件进行加密
     *        机密标识：1 已加密 0 未加密
     */
    private int encrypt;

    /**
     *        上报时间
     */
    private Date dateTime;

    /**
     *        经度，单位为1*10^(-6)
     */
    private String lon;

    /**
     *        纬度，单位为1*10^(-6)
     */
    private String lat;

    /**
     *        速度，指卫星定位车载终端设备上传的行车速度信息，为必填项，单位为千米每小时（km/h）
     */
    private int vec1;

    /**
     *        行驶记录速度,指车辆行驶记录设备上传的行车速度信息，单位为千米每小时（km/h）
     */
    private int vec2;

    /**
     *        车辆当前总里程数，指车辆上传的行车里程数，单位为千米（km）
     */
    private long vec3;

    /**
     *        方向，0~359,单位为度（°），正北为0，顺时针
     */
    private int direction;

    /**
     *        海拔高度，单位为米（m）
     */
    private int altttude;

    /**
     *        车辆状态，二进制表示:B31B30... ...B2B1B0。
     *        具体定义按照JT/T 808-2011 中表17的规定
     */
    private long state;

    /**
     *        报警状态，二进制表示，0表示正常，1表示报警:B31B30B29... ...B2B1B0。
     *        具体定义按照JT/T 808-2011中表18的规定。
     */
    private long alarm;

    /**
     *        接口开发商
     */
    private String developer;

    /**
     *        原始报文
     */
    private Blob receiveData;

}
