package com.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;

import com.xwtec.protoc.CommandProtoc;

public interface IHandleProcessor {
	public CommandProtoc.PushMessage process(ChannelHandlerContext ctx);

	public void updateObject(Object t);

}
