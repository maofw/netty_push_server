package com.netty.push.handler.process.factory;

import io.netty.channel.ChannelHandlerContext;

import com.netty.push.handler.process.IHandleProcessor;

/**
 * 请求处理工厂接口 生产IHandleProcessor
 * 
 * @author maofw
 * 
 */
public interface IHandleProcessorFactory {
	public IHandleProcessor findHandleProcessor(ChannelHandlerContext ctx, Object msg);
}
