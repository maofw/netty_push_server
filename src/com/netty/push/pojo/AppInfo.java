package com.netty.push.pojo;

/**
 * 应用信息
 * 
 * @author maofw
 * 
 */
public class AppInfo {
	private Long appId;
	private String appPackage;
	private String appKey;
	private Long userId;
	public Long getAppId() {
		return appId;
	}
	public void setAppId(Long appId) {
		this.appId = appId;
	}
	public String getAppPackage() {
		return appPackage;
	}
	public void setAppPackage(String appPackage) {
		this.appPackage = appPackage;
	}
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}



}
