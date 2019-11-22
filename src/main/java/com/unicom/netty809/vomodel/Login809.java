package com.unicom.netty809.vomodel;

import java.io.Serializable;

/**
 *
 */
public class Login809 implements Serializable {

    private String userId;

    private String passWord;

    private String ip;

    private String port;

    private String developer;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    @Override
    public String toString() {
        return "Login809{" +
                "userId='" + userId + '\'' +
                ", passWord='" + passWord + '\'' +
                ", ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                ", developer='" + developer + '\'' +
                '}';
    }
}
