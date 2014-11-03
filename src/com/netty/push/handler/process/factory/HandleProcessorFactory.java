package com.netty.push.handler.process.factory;

import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.netty.push.handler.process.DeviceOfflineProcessor;
import com.netty.push.handler.process.DeviceOnlineProcessor;
import com.netty.push.handler.process.HeartbeatProcessor;
import com.netty.push.handler.process.IHandleProcessor;
import com.netty.push.handler.process.MessageReceiptProcessor;
import com.netty.push.handler.process.RegistrationProcessor;
import com.xwtec.protoc.CommandProtoc;

/**
 * 请求处理工厂接口实现类
 * 
 * @author maofw
 * @param <T>
 * 
 */
@Service("handleProcessorFactory")
@Scope("singleton")
public class HandleProcessorFactory implements IHandleProcessorFactory {
	// 处理器集合

	private static Map<CommandProtoc.PushMessage.Type, IHandleProcessor> processors = new HashMap<CommandProtoc.PushMessage.Type, IHandleProcessor>();

	@Override
	public IHandleProcessor findHandleProcessor(ChannelHandlerContext ctx, Object msg) {
		if (msg == null) {
			return null;
		}

		if (msg instanceof CommandProtoc.PushMessage) {

			CommandProtoc.PushMessage pushMessage = (CommandProtoc.PushMessage) msg;
			CommandProtoc.PushMessage.Type type = pushMessage.getType();
			if (type == null) {
				return null;
			}

			IHandleProcessor handleProcessor = null;
			synchronized (processors) {
				handleProcessor = processors.get(type);
				Object obj = null;
				boolean isNew = handleProcessor == null ? true : false;
				switch (type) {
					case HEART_BEAT:
						// 心跳处理
						if (handleProcessor == null) {
							handleProcessor = new HeartbeatProcessor();
						}
						break;
					case DEVICE_ONLINE:
						// 设备上线
						obj = pushMessage.getDeviceOnline();
						if (handleProcessor == null) {
							handleProcessor = new DeviceOnlineProcessor();
						}
						break;
					case DEVICE_OFFLINE:
						// 设备下线
						obj = pushMessage.getDeviceOffline();
						if (handleProcessor == null) {
							handleProcessor = new DeviceOfflineProcessor();
						}
						break;
					case REGISTRATION:
						// 设备注册
						obj = pushMessage.getRegistration();
						if (handleProcessor == null) {
							handleProcessor = new RegistrationProcessor();
						}
						break;
					case MESSAGE_RECEIPT:
						// 消息回执
						obj = pushMessage.getMessageReceipt();
						if (handleProcessor == null) {
							handleProcessor = new MessageReceiptProcessor();
						}
						break;
					default:
						break;
				}
				if (isNew) {
					// 设置处理器集合
					processors.put(type, handleProcessor);
				}
				handleProcessor.updateObject(obj);
			}
			return handleProcessor;
		}
		return null;
	}
}
