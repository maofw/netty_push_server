package com.netty.push.pojo;

import java.util.List;

/**
 * 渠道信息
* 
* @类名称：ChannelInfo   
* @类描述：   
* @创建人：maofw   
* @创建时间：2014-10-13 下午3:16:13
*
 */
public class ChannelInfo {
	// 心跳时间
	private Long heartTime;

	private List<ChannelDeviceInfo> channelDeviceInfos;

	public Long getHeartTime() {
		return heartTime;
	}

	public void setHeartTime(Long heartTime) {
		this.heartTime = heartTime;
	}

	public List<ChannelDeviceInfo> getChannelDeviceInfos() {
		return channelDeviceInfos;
	}

	public void setChannelDeviceInfos(List<ChannelDeviceInfo> channelDeviceInfos) {
		this.channelDeviceInfos = channelDeviceInfos;
	}
	
}
