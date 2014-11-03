package com.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;

import com.xwtec.protoc.CommandProtoc.PushMessage;

/**
 * 客户端心跳处理
 * 
 * @author maofw
 * 
 */
public class HeartbeatProcessor extends AbstractHandleProcessor<Object> {

	@Override
	public PushMessage process(ChannelHandlerContext ctx) {
		if (ctx != null) {
			applicationContext.refreshHeart(ctx.channel());
		}
		return null;
	}

}
