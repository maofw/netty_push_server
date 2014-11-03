package com.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import com.netty.push.pojo.AppInfo;
import com.netty.push.pojo.DeviceInfo;
import com.netty.push.pojo.MessageInfo;
import com.xwtec.protoc.CommandProtoc;
import com.xwtec.protoc.CommandProtoc.PushMessage;

/**
 * 设备上线消息处理
 * 
 * @author maofw
 * 
 */
public class DeviceOnlineProcessor extends AbstractHandleProcessor<CommandProtoc.DeviceOnline> {

	@Override
	public PushMessage process(ChannelHandlerContext ctx) {
		// 获取设备id
		String deviceId = this.getProcessObject().getDeviceId();
		// 设备上线
		boolean b = this.applicationContext.online(deviceId);

		DeviceInfo deviceInfo = this.applicationContext.getDeviceInfo(deviceId);
		AppInfo appInfo = deviceInfo == null ? null : this.applicationContext.getAppInfo(deviceInfo.getAppKey());
		String appPackage = appInfo == null ? null : appInfo.getAppPackage();

		CommandProtoc.DeviceOnoffResult.ResultCode resultCode = null;
		CommandProtoc.Message.UserStatus userStatus = CommandProtoc.Message.UserStatus.ONLINE;
		resultCode = b ? CommandProtoc.DeviceOnoffResult.ResultCode.SUCCESS : CommandProtoc.DeviceOnoffResult.ResultCode.FAILED;
		CommandProtoc.PushMessage pushMessage = this.applicationContext.createCommandDeviceOnoffResult(appPackage, resultCode, userStatus);
		System.out.println("writeAndFlush DeviceOnoffResult:=======================\n" + pushMessage);
		ctx.writeAndFlush(pushMessage);
		if (b) {
			pushMessage = this.applicationContext.getDefaultPushMessageForLogin(deviceId, "Welcome", "Welcome to monitor push platform!", false);
			System.out.println("writeAndFlush getDefaultPushMessageForLogin:=======================\n" + pushMessage);
			// 发送默认消息
			ctx.writeAndFlush(pushMessage);
			// 如果设备上线成功 则查询设备是否存在离线消息 有则返回离线消息列表
			List<MessageInfo> messageList = this.applicationContext.getMessageOfflineOfDevice(deviceId);
			if (messageList != null && messageList.size() > 0) {
				for (MessageInfo messageInfo : messageList) {
					System.out.println("writeAndFlush offline message:=======================" + messageInfo.getMsgId()+"-"+messageInfo.getTitle());
					this.applicationContext.sendMessage(messageInfo, CommandProtoc.Message.UserStatus.OFFLINE);
				}
			}
		}
		return null;
	}
}
