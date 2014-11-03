package com.netty.push.pojo;

/**
 * 离线消息
 * 
 * @author maofw
 * 
 */
public class MessageOffline {
	private Long msgId;
	private String deviceId;
	public Long getMsgId() {
		return msgId;
	}
	public void setMsgId(Long msgId) {
		this.msgId = msgId;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	

}
