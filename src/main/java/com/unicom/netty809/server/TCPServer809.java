package com.unicom.netty809.server;

import com.unicom.netty809.common.Decoder;
import com.unicom.netty809.common.Encoder;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 *  * 根据809协议的服务
 */
public class TCPServer809 implements Runnable{

	private Logger log = LoggerFactory.getLogger(getClass());
	private volatile boolean isRunning = false;

	private int port;

	private String developer;
	//初始化服务
	public TCPServer809() {
		this.port = 10906;
	}
	//初始化服务
	public TCPServer809(int port, String developer) {
		this();
		this.port = port;
		this.developer = developer;
	}

	private void bind() throws Exception {

		ChannelFactory factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());

		ServerBootstrap bootstrap = new ServerBootstrap(factory);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() {
				ChannelPipeline pipeline = Channels.pipeline();
				//添加日志过滤器
				pipeline.addLast("loging", new LoggingHandler(InternalLogLevel.INFO));
				//添加解码过滤器
				pipeline.addLast("decoder", new Decoder(developer));//解码
				//添加编码过滤器
				pipeline.addLast("encoder", new Encoder());//编码
				//添加接收报文业务处理过滤器
				pipeline.addLast("busiHandler", new BusiHandler(developer));
				return pipeline;
			}
		});
		//非延迟
		bootstrap.setOption("child.tcpNoDelay", true);
		//长连接
		bootstrap.setOption("child.keepAlive", true);

		bootstrap.bind(new InetSocketAddress(port));

		this.log.info("TCP服务启动完毕,port={}", this.port);
	}
	//服务器启动
	public synchronized void startServer() {
		if (this.isRunning) {
			throw new IllegalStateException("TCP809 Server is already started .");
		}
		this.isRunning = true;
		try {
			this.bind();
		} catch (Exception e) {
			this.log.info("TCP809服务启动出错:{}", e.getMessage());
			e.printStackTrace();
		}
	}
	//多线程启动服务
	@Override
	public void run() {
		startServer();
	}
}