package com.netty.push.pojo;

import io.netty.channel.Channel;

import java.util.List;

/**
 * 渠道设备信息
 * 
 * @类名称：ChannelDeviceInfo
 * @类描述：
 * @创建人：maofw
 * @创建时间：2014-10-13 下午3:13:51
 * 
 */
public class ChannelDeviceInfo {
	private Channel channel;
	private DeviceInfo deviceInfo;
	private List<MessagePushedInfo> messagePushedInfos;

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public DeviceInfo getDeviceInfo() {
		return deviceInfo;
	}

	public void setDeviceInfo(DeviceInfo deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	public List<MessagePushedInfo> getMessagePushedInfos() {
		return messagePushedInfos;
	}

	public void setMessagePushedInfos(List<MessagePushedInfo> messagePushedInfos) {
		if (this.messagePushedInfos != null && this.messagePushedInfos.size() > 0) {
			this.messagePushedInfos.clear();
			this.messagePushedInfos = null;
		}
		this.messagePushedInfos = messagePushedInfos;
	}

}
