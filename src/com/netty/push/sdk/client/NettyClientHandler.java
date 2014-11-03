package com.netty.push.sdk.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import com.netty.push.sdk.client.listener.INettyClientHandlerListener;
import com.netty.push.sdk.pojo.Message;
import com.netty.push.sdk.pojo.MessageResult;

/**
 * 客户端消息处理Handler
 * 
 * @author maofw
 * 
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<Object> {

	private NettyClient nettyClient;
	private Message message;
	private INettyClientHandlerListener listener;

	public NettyClientHandler(NettyClient nettyClient, Message message, INettyClientHandlerListener listener) {
		this.nettyClient = nettyClient;
		this.message = message;
		this.listener = listener;
	}

	/**
	 * 此方法会在连接到服务器后被调用
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (nettyClient != null) {
			nettyClient.setCtx(ctx);
		}
		if (message != null) {
			// 成功后发送消息
			ctx.writeAndFlush(message);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
		System.out.println("channelRead");
		if (obj != null && obj instanceof MessageResult) {
			if (listener != null) {
				listener.receive((MessageResult) obj);
			}
		}
	}

	/**
	 * 捕捉到异常
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext arg0, Object arg1) throws Exception {
		System.out.println("channelRead0");

	}
}
