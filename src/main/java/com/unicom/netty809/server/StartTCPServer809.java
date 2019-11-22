package com.unicom.netty809.server;

import org.springframework.stereotype.Component;

/**
 * 服务启动
 */
@Component
public class StartTCPServer809 {



    static {
        TCPServer809 c1 = new TCPServer809(10905,"company1");
        new Thread(c1).start();
    }
}
