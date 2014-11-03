package com.netty.push.sdk.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.netty.push.sdk.client.listener.INettyClientHandlerListener;
import com.netty.push.sdk.pojo.Message;

/**
 * netty连接客户端
 * 
 * @类名称：NettyClient
 * @类描述：
 * @创建人：maofw
 * @创建时间：2014-10-30 下午2:51:29
 * 
 */
public class NettyClient {
	private static NettyClient nettyClient = null;
	private NioEventLoopGroup group;
	private String host;
	private int port;
	/**
	 * 连接状态常量
	 */
	// 初始化
	public static final int CONNECT_INIT = 0;
	// 正在处理中
	public static final int CONNECT_PROCESSORING = 1;
	// 連接成功
	public static final int CONNECT_SUCCESS = 2;
	// 連接失敗
	public static final int CONNECT_FAILED = -1;
	// 連接关闭
	public static final int CONNECT_CLOSED = -2;
	// 連接超時
	public static final int CONNECT_TIMEOUT = -3;
	// 連接异常
	public static final int CONNECT_EXCEPTION = -4;
	// 连接状态
	private int connectState = CONNECT_INIT;

	private ChannelHandlerContext ctx;

	private NettyClient() {
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到时才会装载，从而实现了延迟加载。
	 */
	private static class SingletonHolder {
		/**
		 * 静态初始化器，由JVM来保证线程安全
		 */
		private static NettyClient instance = new NettyClient();
	}

	public static NettyClient getInstance(final String host, final int port) {
		if (nettyClient == null) {
			nettyClient = SingletonHolder.instance;
			nettyClient.host = host;
			nettyClient.port = port;
		}
		return nettyClient;
	}

	/**
	 * 连接方法
	 * 
	 * @param host
	 * @param port
	 * @throws Exception
	 */
	public void sendMessage(final Message message, final INettyClientHandlerListener listener) throws Exception {
		if (message == null) {
			return;
		}
		if (isConnected()) {
			// 如果已经连接了则直接发送消息 不必再创建socket连接
			if (ctx != null) {
				ctx.writeAndFlush(message);
			}
			return;
		}

		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv6Addresses", "false");
		ChannelFuture channelFuture = null;
		group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group);
			b.channel(NioSocketChannel.class);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.remoteAddress(new InetSocketAddress(host, port));
			// 有连接到达时会创建一个channel
			b.handler(new ChannelInitializer<SocketChannel>() {
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
					pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
					pipeline.addLast("encode", new ObjectEncoder());
					pipeline.addLast("decode", new ObjectDecoder(ClassResolvers.weakCachingConcurrentResolver(null)));
					pipeline.addLast("handler", new NettyClientHandler(nettyClient, message, listener));

				}
			});
			channelFuture = b.connect().sync();
			channelFuture.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						SocketAddress sa = future.channel().remoteAddress();
						if (sa != null) {
							System.out.println("netty server connected success! host:" + sa);
							// 連接成功
							connectState = CONNECT_SUCCESS;
						} else {
							System.out.println("netty server connected failed! host:" + sa);
							// 連接失敗
							connectState = CONNECT_FAILED;
							// 連接 失敗 啟動重連
							future.cause().printStackTrace();
							future.channel().close();
						}
					} else {
						System.out.println("netty server attemp failed! host:" + future.channel().remoteAddress());
						// 連接失敗
						connectState = CONNECT_FAILED;
						// 連接 失敗 啟動重連
						future.cause().printStackTrace();
						future.channel().close();
					}
				}
			});
		} finally {
			disconnect(channelFuture);
		}
	}

	/**
	 * 断开连接
	 */
	private void disconnect(ChannelFuture channelFuture) throws Exception {
		if (channelFuture != null) {
			channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture channelFuture) throws Exception {
					if (channelFuture != null) {
						connectState = CONNECT_CLOSED;
						if (!group.isShutdown() && !group.isShuttingDown()) {
							group.shutdownGracefully();// .sync();
						}
						System.out.println("netty server closed success!" + channelFuture.channel().remoteAddress());
					}
				}
			});
		}
	}

	/**
	 * 是否連接
	 * 
	 * @return
	 */
	private boolean isConnected() {
		return connectState == CONNECT_SUCCESS ? true : false;
	}

}
