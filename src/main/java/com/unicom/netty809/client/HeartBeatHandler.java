package com.unicom.netty809.client;

import com.unicom.netty809.vomodel.Message;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhaoxiao 2019/11/22
 */
public class HeartBeatHandler extends IdleStateAwareChannelHandler {

    private static Logger LOG = LoggerFactory.getLogger(HeartBeatHandler.class);
    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception{

        if(StringUtils.isNotBlank(TcpClientDemo.LONGINSTATUS) || TcpClientDemo.LOGINING.equals(TcpClientDemo.LONGINSTATUS)){
            TcpClientDemo.getInstance().login2Gov();
            LOG.error("利用空闲心跳去登录------ 开始登录");
        }

        if(e.getState() == IdleState.WRITER_IDLE){
            LOG.error("链路空闲，发送心跳!");
            Message msg = new Message(JT809Constants.UP_LINKETEST_REQ);
            e.getChannel().write(TcpClientDemo.buildMessage(msg));
            super.channelIdle(ctx, e);
        }
    }

}
