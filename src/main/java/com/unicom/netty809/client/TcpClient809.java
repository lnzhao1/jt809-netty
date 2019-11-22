package com.unicom.netty809.client;

import com.unicom.netty809.common.Decoder;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * @author
 */
public class TcpClient809{
    private  Logger LOG = LoggerFactory.getLogger(getClass());
    private static final int DEFAULT_PORT = 9000;

    private long connectTimeoutMillis = 3000;

    private int port = DEFAULT_PORT;

    private boolean tcpNoDelay = false;

    private boolean reuseAddress = true;

    private boolean keepAlive = true;

    private int workerCount = 4;

    private ClientBootstrap bootstrap = null;

    private  static Channel channel = null;

    private Executor bossExecutor = Executors.newCachedThreadPool();

    private Executor workerExecutor = Executors.newCachedThreadPool();


    private static TcpClient809 instance = new TcpClient809();

    private TcpClient809() {
        init();
    }

    public static TcpClient809 getInstence(){
        return instance;
    }


    public void init() {

        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
                bossExecutor, workerExecutor, workerCount));
        bootstrap.setOption("tcpNoDelay", tcpNoDelay);
        bootstrap.setOption("connectTimeoutMillis", connectTimeoutMillis);
        bootstrap.setOption("reuseAddress", reuseAddress);
        bootstrap.setOption("keepAlive", keepAlive);
    }


    public Channel getChannel(String address, int port) {

        if (null == channel || !channel.isOpen()) {
            bootstrap.setOption("writeBufferHighWaterMark", 64 * 1024);
            bootstrap.setOption("writeBufferLowWaterMark", 32 * 1024);
            bootstrap.setPipelineFactory(new ChannelPipelineFactory(){
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    ChannelPipeline pipeline = Channels.pipeline();
                    pipeline.addLast("loging", new LoggingHandler(InternalLogLevel.ERROR));
                    pipeline.addLast("timeout", new IdleStateHandler(new HashedWheelTimer(), 10, 60, 0));//设置空闲心跳机制
                    pipeline.addLast("heartbeat", new HeartBeatHandler());//心跳发送包处理handler
                    pipeline.addLast("decode", new Decoder(""));//解码
                    pipeline.addLast("cliBusiHandler", new CliBusiHandler());//反馈数据处理
                    return pipeline;
                }
            });
            ChannelFuture future = bootstrap.connect(new InetSocketAddress(
                    address, port));
            future.awaitUninterruptibly();
            if (future.isSuccess()) {
                channel = future.getChannel();
            } else {
//                throw new FrameworkRuntimeException(future.getCause());
            }
        }

        return channel;
    }



    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

}
