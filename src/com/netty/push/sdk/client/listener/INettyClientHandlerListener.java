package com.netty.push.sdk.client.listener;

import com.netty.push.sdk.pojo.MessageResult;

public interface INettyClientHandlerListener {
	public void receive(MessageResult messageResult);
}
