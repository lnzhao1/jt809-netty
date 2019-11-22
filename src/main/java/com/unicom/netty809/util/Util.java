package com.unicom.netty809.util;
/**
    转16进制
*/
public class Util {
    public static int crc16(byte[]... bytesArr) {
        int b = 0;
        int crc = 0xffff;

        for (byte[] d : bytesArr) {
            for (int i = 0; i < d.length; i++) {
                for (int j = 0; j < 8; j++) {
                    b = ((d[i] << j) & 0x80) ^ ((crc & 0x8000) >> 8);
                    crc <<= 1;
                    if (b != 0)
                        crc ^= 0x1021;
                }
            }
        }
        crc = ~crc;
        return crc;
    }

}
