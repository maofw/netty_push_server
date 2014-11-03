package com.netty.push.pojo;

import java.util.Date;

/**
 * 设备信息
 * 
 * @author maofw
 * 
 */
public class DeviceInfo {
	private String regId ;
	private Long userId ;
	private Long appId ;
	private String appKey ;
	private int isOnline ;
	private String deviceId ;
	private String imei ;
	private String channel ;
	private Date onlineTime ;
	private Date offlineTime ;
	public String getRegId() {
		return regId;
	}
	public void setRegId(String regId) {
		this.regId = regId;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	public Long getAppId() {
		return appId;
	}
	public void setAppId(Long appId) {
		this.appId = appId;
	}
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
	public int getIsOnline() {
		return isOnline;
	}
	public void setIsOnline(int isOnline) {
		this.isOnline = isOnline;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getImei() {
		return imei;
	}
	public void setImei(String imei) {
		this.imei = imei;
	}
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
	}
	public Date getOnlineTime() {
		return onlineTime;
	}
	public void setOnlineTime(Date onlineTime) {
		this.onlineTime = onlineTime;
	}
	public Date getOfflineTime() {
		return offlineTime;
	}
	public void setOfflineTime(Date offlineTime) {
		this.offlineTime = offlineTime;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("regId:"+regId);
		sb.append("-userId:"+userId);
		sb.append("-appId:"+appId);
		sb.append("-appKey:"+appKey);
		sb.append("-isOnline:"+(isOnline==1?"ONLINE":"OFFLINE"));
		sb.append("-deviceId:"+deviceId);
		sb.append("-imei:"+imei);
		sb.append("-channel:"+channel);
		sb.append("-onlineTime:"+((onlineTime==null)?"":onlineTime.toString()));
		sb.append("-offlineTime:"+((offlineTime==null)?"":offlineTime.toString()));
		return sb.toString();
	}
	
}
