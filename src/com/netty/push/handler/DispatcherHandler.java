package com.netty.push.handler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.netty.push.handler.context.ApplicationContext;
import com.netty.push.handler.process.IHandleProcessor;
import com.netty.push.handler.process.factory.IHandleProcessorFactory;
import com.xwtec.protoc.CommandProtoc;

/**
 * 分发器Handler
 * 
 * @author maofw
 * 
 */
@Sharable
@Service("dispatcherHandler")
@Scope("singleton")
public class DispatcherHandler extends ChannelInboundHandlerAdapter {
	@Resource
	private IHandleProcessorFactory handleProcessorFactory;
	@Resource
	private ApplicationContext applicationContext;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		// 将新的连接加入到ChannelGroup，当连接断开ChannelGroup会自动移除对应的Channel
		applicationContext.addChannel(ctx.channel());
		System.out.println("applicationContext addChannel:======================\n" + ctx.channel().localAddress() + "-" + ctx.channel().remoteAddress());
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		System.out.println(sdf.format(new Date()) + "-\n" + msg);
		IHandleProcessor handleProcessor = handleProcessorFactory.findHandleProcessor(ctx, msg);
		if (handleProcessor != null) {
			CommandProtoc.PushMessage pushMessage = handleProcessor.process(ctx);
			if (pushMessage != null) {
				System.out.println("writeAndFlush pushMessage:=======================\n" + pushMessage);
				ctx.writeAndFlush(pushMessage);
			}
		}
	}

	public void channelReadComplete(ChannelHandlerContext ctx) {
		// flush掉所有写回的数据
		// ctx.close().addListener(ChannelFutureListener.CLOSE); //
		// 当flush完成后关闭channel
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		System.out.println("server exceptionCaught====" + ctx.channel().remoteAddress() + "-" + cause.getMessage());
		cause.printStackTrace();// 捕捉异常信息
		//System.out.println("server exceptionCaught====" + ctx.channel().remoteAddress());
		// ctx.close();// 出现异常时关闭channel
		//applicationContext.offline(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("server channelInactive====" + ctx.channel().remoteAddress());
		applicationContext.offline(ctx.channel());
	}

}
