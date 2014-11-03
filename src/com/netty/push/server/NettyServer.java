package com.netty.push.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.net.InetSocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.protobuf.ExtensionRegistry;
import com.netty.push.handler.DispatcherHandler;
import com.netty.push.handler.context.ApplicationContext;
import com.netty.push.sdk.server.MessageHandler;
import com.netty.push.utils.SystemPrintUtil;
import com.xwtec.protoc.CommandProtoc;

/**
 * server处理类main方法启动server
 * 
 * @author maofw
 * 
 */
@Service("nettyServer")
@Scope("singleton")
public class NettyServer implements IServer {

	// 服务端口
	private int port;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	@Resource
	private DispatcherHandler dispatcherHandler;

	@Resource
	private MessageHandler messageHandler;

	private List<String> serverIpConfig;

	@Resource
	private ApplicationContext applicationContext;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public List<String> getServerIpConfig() {
		return serverIpConfig;
	}

	public void setServerIpConfig(List<String> serverIpConfig) {
		this.serverIpConfig = serverIpConfig;
	}

	/**
	 * 启动服务器方法入口
	 */
	@Override
	public void start() throws Exception {
		bossGroup = new NioEventLoopGroup(); // (1)
		workerGroup = new NioEventLoopGroup();
		try {
			// 引导辅助程序
			ServerBootstrap sb = new ServerBootstrap(); // (2)
			// 通过nio方式来接收连接和处理连接
			sb.group(bossGroup, workerGroup);
			// 设置nio类型的channel
			sb.channel(NioServerSocketChannel.class); // (3)
			// 设置监听端口
			sb.localAddress(new InetSocketAddress(port));
			// 有连接到达时会创建一个channel
			final ExtensionRegistry registry = ExtensionRegistry.newInstance();
			CommandProtoc.registerAllExtensions(registry);

			sb.childHandler(new ChannelInitializer<SocketChannel>() { // (4)
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					// 判断是否是server接入渠道
					InetSocketAddress address = ch.remoteAddress();
					if (address != null) {
						if (address.getAddress() != null) {
							String ip = address.getAddress().getHostAddress();
							System.out.println("address.getAddress().getHostAddress():" + address.getAddress().getHostAddress());
							System.out.println("serverIpConfig:" + serverIpConfig);
							if (serverIpConfig != null && ip != null && serverIpConfig.contains(ip)) {
								System.out.println("contains!!!");
								ChannelPipeline pipeline = ch.pipeline();
								pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
								pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
								pipeline.addLast("encode", new ObjectEncoder());
								pipeline.addLast("decode", new ObjectDecoder(ClassResolvers.weakCachingConcurrentResolver(null)));
								pipeline.addLast("handler", messageHandler);
								return;
							}
						}
					}
					// pipeline管理channel中的Handler，在channel队列中添加一个handler来处理业务
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
					pipeline.addLast("protobufDecoder", new ProtobufDecoder(CommandProtoc.PushMessage.getDefaultInstance(), registry));
					pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
					pipeline.addLast("protobufEncoder", new ProtobufEncoder());
					pipeline.addLast("hander", dispatcherHandler);
				}
			});
			sb.option(ChannelOption.SO_BACKLOG, 128); // (5)
			sb.childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
			// Bind and start to accept incoming connections.
			// 配置完成，开始绑定server，通过调用sync同步方法阻塞直到绑定成功
			ChannelFuture cf = sb.bind().sync(); // (7)
			applicationContext.addChannel(cf.channel());
			System.out.println("server applicationContext addChannel:======================\n" + cf.channel().localAddress() + "-"
					+ cf.channel().remoteAddress());
			SystemPrintUtil.printServerInfo(this.getClass().getName() + " started and listen on " + cf.channel().localAddress());
			// Wait until the server socket is closed.
			cf.channel().closeFuture().sync();
		} finally {
			System.out.println("finally!!!");
			stopServer();
		}
	}

	/**
	 * 重启netty服务器
	 */
	@Override
	public void restart() throws Exception {
		SystemPrintUtil.printServerInfo(this.getClass().getName() + " restarting  netty server on port:" + port);
		ChannelGroupFuture future = applicationContext.closeAllChannels();
		if (future != null) {
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.channel().close();
					stopServer();
					start();
				}
			});
		}
	}

	/**
	 * 停止服务器
	 */
	@Override
	public void stop() throws Exception {
		ChannelGroupFuture future = applicationContext.closeAllChannels();
		if (future != null) {
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.channel().close();
					stopServer();
				}
			});
		}

	}

	private void stopServer() throws Exception {
		if (workerGroup != null) {
			workerGroup.shutdownGracefully();
			workerGroup = null;
		}

		if (bossGroup != null) {
			bossGroup.shutdownGracefully();
			bossGroup = null;
		}
		applicationContext.destory();
		SystemPrintUtil.printServerInfo(this.getClass().getName() + " stop netty server on " + this.port + " success!");
	}

	public static void main(String[] args) {
		String dateString = new Date().toString();

		System.out.println(dateString);
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US);// MMM
		// dd
		// hh:mm:ss
		// Z
		// yyyy
		// DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL,
		// Locale.getDefault());
		try {
			System.out.println(sdf.parse(dateString));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
