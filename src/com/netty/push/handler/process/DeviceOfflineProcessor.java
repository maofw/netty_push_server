package com.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;

import com.netty.push.pojo.AppInfo;
import com.netty.push.pojo.DeviceInfo;
import com.xwtec.protoc.CommandProtoc;
import com.xwtec.protoc.CommandProtoc.PushMessage;

/**
 * 设备下线消息处理
 * 
 * @author maofw
 * 
 */
public class DeviceOfflineProcessor extends AbstractHandleProcessor<CommandProtoc.DeviceOffline> {

	@Override
	public PushMessage process(ChannelHandlerContext ctx) {
		CommandProtoc.DeviceOnoffResult.ResultCode resultCode = null;
		CommandProtoc.Message.UserStatus userStatus = CommandProtoc.Message.UserStatus.OFFLINE;
		// 获取设备id
		String deviceId = this.getProcessObject().getDeviceId();

		DeviceInfo deviceInfo = this.applicationContext.getDeviceInfo(deviceId);
		AppInfo appInfo = deviceInfo == null ? null : this.applicationContext.getAppInfo(deviceInfo.getAppKey());
		String appPackage = appInfo == null ? null : appInfo.getAppPackage();
		// 设备上线
		boolean b = this.applicationContext.offline(deviceId);
		resultCode = b ? CommandProtoc.DeviceOnoffResult.ResultCode.SUCCESS : CommandProtoc.DeviceOnoffResult.ResultCode.FAILED;
		if (b) {
			// DeviceInfo deviceInfo = this.applicationContext.getDeviceInfo(deviceId);
			System.out.println("Byebye :" + deviceId);
		}
		return this.applicationContext.createCommandDeviceOnoffResult(appPackage, resultCode, userStatus);
	}
}
