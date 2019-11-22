package com.unicom.netty809.vomodel;

import lombok.Data;

/**
 * 报警报文消息体
 */
@Data
public class UpWarningMsgEntity {
    /**
     *车牌号
     */
    private String vehicleNo;
    /**
     *车牌颜色
     * 0x01:蓝色
     * 0x02:黄色
     * 0x03:黑色
     * 0x04:白色
     * 0x05:绿色
     * 0x06:黄绿色
     * 0x09:其它
     */
    private byte vehicleColor ;
    /**
     * 子业务类型标识
     */
    private int dataType ;
    /**
     * 后续数据长度
     */
    private long dataLength ;
    /**
     * 报警信息来源定义如下：
     * 0x01：车载终端
     * 0x02：企业监控平台
     * 0x03：政府监控平台
     * 0x09：其他
     */
    private byte warnSrc ;
    /**
     * 报警类型
     */
    private int warnType ;
    /**
     * 报警时间，UTC 时间格式
     */
    private long warnTime ;
    /**
     * 报警信息 ID
     */
    private String infoId ;
    /**
     * 驾驶员姓名长度
     */
    private byte driverLength ;
    /**
     * 驾驶员姓名
     */
    private String driver;
    /**
     * 驾驶员驾照号码长度
     */
    private byte driverNoLength;
    /**
     * 驾驶员驾照号码
     */
    private String driverNo;
    /**
     * 报警级别
     */
    private byte level;
    /**
     * 经度,单位为 1*10^-6 度
     */
    private String lng;
    /**
     * 纬度,单位为 1*10^-6 度
     */
    private String lat;
    /**
     * 海拔高度,单位为米(m)
     */
    private int altitude;
    /**
     * 行车速度，单位为千米每小时(km/h)
     */
    private int vec1 ;
    /**
     *行驶记录速度,单位为千米每小时(km/h)
     */
    private int vec2 ;
    /**
     * 报警状态,1:报警开始;2:报警结束
     */
    private byte status ;
    /**
     * 方向,0-359,正北为 0,顺时针
     */
    private int direction ;
    /**
     *报警数据长度,最长 2048 字节
     */
    private int infoLength ;
    /**
     * 上报报警信息内容
     */
    private String infoContent ;

}
