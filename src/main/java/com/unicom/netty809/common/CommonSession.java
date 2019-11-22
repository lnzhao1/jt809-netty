package com.unicom.netty809.common;

import com.unicom.netty809.util.ChannelUtils;
import lombok.*;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 链接的会话
 * 会话channel map 根据ip储存netty服务的channel 会话
 */

@ToString
public class CommonSession {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Getter
	@Setter
	private String developer;

	@Getter
	@Setter
	private Map<String, Channel> developerSessionHashMap = new ConcurrentHashMap<>();

	public CommonSession(String developer) {
		this.developer = developer;
	}

	public void addChannel(Channel channel){
		String ip = ChannelUtils.getIp(channel);
		developerSessionHashMap.put(ip,channel);
	}

	public boolean isClose(Channel channel) {
		if (channel == null) {
			return true;
		}
		return !channel.isConnected() ||
			   !channel.isOpen();
	}


}
