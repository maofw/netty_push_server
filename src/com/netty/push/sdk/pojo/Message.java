package com.netty.push.sdk.pojo;

import java.io.Serializable;
import java.util.List;

/**
 * 消息实体
 * 
 * @类名称：Message
 * @类描述：
 * @创建人：maofw
 * @创建时间：2014-10-30 上午10:56:34
 * 
 */
public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8882696265942622650L;
	private String appKey;
	private String title;
	private String content;
	private boolean isOfflineShow;
	// 是否需要回执
	private boolean isReceipt;

	private boolean isAllPush;
	private List<String> devices;

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isOfflineShow() {
		return isOfflineShow;
	}

	public void setOfflineShow(boolean isOfflineShow) {
		this.isOfflineShow = isOfflineShow;
	}

	public boolean isReceipt() {
		return isReceipt;
	}

	public void setReceipt(boolean isReceipt) {
		this.isReceipt = isReceipt;
	}

	public List<String> getDevices() {
		return devices;
	}

	public void setDevices(List<String> devices) {
		this.devices = devices;
	}

	public boolean isAllPush() {
		return isAllPush;
	}

	public void setAllPush(boolean isAllPush) {
		this.isAllPush = isAllPush;
	}

}
