package com.unicom.netty809.util;

import org.jboss.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * channel的工具类
 * @author kingston
 */
public final class ChannelUtils {
	

	public static String getIp(Channel channel) {
		return ((InetSocketAddress)channel.getRemoteAddress()).getAddress().toString().substring(1);
	}

}
