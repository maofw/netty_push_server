package com.netty.push.sdk.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.netty.push.handler.context.ApplicationContext;
import com.netty.push.pojo.AppInfo;
import com.netty.push.pojo.DeviceInfo;
import com.netty.push.pojo.MessageDevice;
import com.netty.push.pojo.MessageInfo;
import com.netty.push.sdk.pojo.Message;
import com.netty.push.sdk.pojo.MessageResult;
import com.xwtec.protoc.CommandProtoc;

/**
 * 外部接口Handler 接收消息后处理
 * 
 * @类名称：MessageHandler
 * @类描述：
 * @创建人：maofw
 * @创建时间：2014-10-30 上午10:16:57
 * 
 */
@Sharable
@Service("messageHandler")
@Scope("singleton")
public class MessageHandler extends ChannelInboundHandlerAdapter {
	@Resource
	private ApplicationContext applicationContext;

	private long endTimesInMillis;

	private long expireTimes;

	public long getEndTimesInMillis() {
		return endTimesInMillis;
	}

	public void setEndTimesInMillis(long endTimesInMillis) {
		this.endTimesInMillis = endTimesInMillis;
	}

	public long getExpireTimes() {
		return expireTimes;
	}

	public void setExpireTimes(long expireTimes) {
		this.expireTimes = expireTimes;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// 将新的连接加入到ChannelGroup，当连接断开ChannelGroup会自动移除对应的Channel
		applicationContext.addChannel(ctx.channel());
		System.out.println("applicationContext addChannel:======================\n" + ctx.channel().localAddress() + "-" + ctx.channel().remoteAddress());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Long msgId = null;
		boolean success = false;
		if (msg != null && msg instanceof Message) {
			Message message = (Message) msg;
			// 将消息入库'
			AppInfo app = applicationContext.getAppInfo(message.getAppKey());
			if (app == null) {
				// APP KEY非法
				System.out.println("MESSAGEHANDLER INVALID APP KEY:" + message.getAppKey() + "\n");
				return;
			}

			Calendar calendar = Calendar.getInstance();
			Date today = calendar.getTime();
			calendar.setTimeInMillis(today.getTime() + endTimesInMillis);
			Date endTime = calendar.getTime();
			System.out.println("TODAY:" + today + "-ENDTIME:" + endTime);

			MessageInfo messageInfo = new MessageInfo();
			messageInfo.setAppId(app.getAppId());
			messageInfo.setContent(message.getContent());
			messageInfo.setTitle(message.getTitle());
			messageInfo.setEndTime(endTime);
			messageInfo.setStartTime(today);
			messageInfo.setPushTime(today);
			messageInfo.setExpireTimes(message.isReceipt() ? expireTimes : 0);
			messageInfo.setIsOfflineShow(message.isOfflineShow() ? ApplicationContext.MESSAGE_OFFLINE_SHOW_YES : ApplicationContext.MESSAGE_OFFLINE_SHOW_NO);
			messageInfo.setType(message.isAllPush() ? ApplicationContext.MESSAGE_TYPE_SEND_TO_ALL : ApplicationContext.MESSAGE_TYPE_SEND_TO_POINT);
			messageInfo.setState(ApplicationContext.MESSAGE_STATE_YES);
			int x = applicationContext.saveMessageInfo(messageInfo);
			if (x > 0) {
				msgId = messageInfo.getMsgId();
				success = true;
				// 如果是针对设备推送则获取设备信息
				if (!message.isAllPush()) {
					// 进行PUSH消息推送
					List<DeviceInfo> devices = applicationContext.queryDeviceInfoListByRegIds(message.getAppKey(), message.getDevices());
					applicationContext.sendMessageToDevices(messageInfo, CommandProtoc.Message.UserStatus.ONLINE, devices);
					// 保存消息-设备对应关系到数据库
					if (devices != null && devices.size() > 0) {
						// 保存消息-设备信息
						List<MessageDevice> messageDevices = new ArrayList<MessageDevice>();
						for (DeviceInfo deviceInfo : devices) {
							MessageDevice md = new MessageDevice();
							md.setMsgId(messageInfo.getMsgId());
							md.setDeviceId(deviceInfo.getDeviceId());
							messageDevices.add(md);
						}
						applicationContext.saveMessageDevices(messageDevices);
					}
				} else {
					// 保存成功 了 进行PUSH消息推送
					applicationContext.sendMessageToAll(messageInfo, CommandProtoc.Message.UserStatus.ONLINE);
				}

			}
			messageInfo = null;
		}

		// 写回结果
		MessageResult mr = new MessageResult();
		mr.setMsgId(msgId);
		mr.setSuccess(success);
		ctx.writeAndFlush(mr);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		// 读完关闭
		ctx.close().addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("server exceptionCaught====" + ctx.channel().remoteAddress() + "-" + cause.getMessage());
		cause.printStackTrace();// 捕捉异常信息
		// System.out.println("server exceptionCaught====" + ctx.channel().remoteAddress());
		ctx.close();// 出现异常时关闭channel
	}

}
