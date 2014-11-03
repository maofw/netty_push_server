package com.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;

import com.xwtec.protoc.CommandProtoc;
import com.xwtec.protoc.CommandProtoc.PushMessage;

/**
 * 消息回执处理
 * 
 * @author maofw
 * 
 */
public class MessageReceiptProcessor extends AbstractHandleProcessor<CommandProtoc.MessageReceipt> {

	@Override
	public PushMessage process(ChannelHandlerContext ctx) {
		CommandProtoc.MessageReceipt messageReceipt = this.getProcessObject();
		if (messageReceipt != null) {
			applicationContext.saveMessagePushedInfo(ctx.channel(),messageReceipt.getAppKey(), messageReceipt.getMsgId(), messageReceipt.getRegistrationId());
		}
		return null;
	}
}
