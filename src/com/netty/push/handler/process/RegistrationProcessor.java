package com.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;

import com.netty.push.pojo.DeviceInfo;
import com.xwtec.protoc.CommandProtoc;
import com.xwtec.protoc.CommandProtoc.PushMessage;

/**
 * 设备注册消息请求
 * 
 * @author maofw
 * 
 */
public class RegistrationProcessor extends AbstractHandleProcessor<CommandProtoc.Registration> {

	@Override
	public PushMessage process(ChannelHandlerContext ctx) {
		// 服务器解析注册数据请求
		// 判断注册是否成功
		CommandProtoc.Registration registration = this.getProcessObject();
		int result = applicationContext.registDevice(ctx.channel(), registration);
		String regId = null;
		if (result > 0) {
			// 成功了 返回RegistrationResult
			DeviceInfo deviceInfo = applicationContext.getDeviceInfo(registration.getDeviceId());
			regId = deviceInfo.getRegId();
		}

		return applicationContext.createCommandRegistrationResult(registration.getAppKey(), registration.getAppPackage(), regId);

	}
}
