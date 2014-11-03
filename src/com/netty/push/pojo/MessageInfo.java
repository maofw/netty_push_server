package com.netty.push.pojo;

import java.util.Date;

public class MessageInfo {
	private Long msgId;
	private Long appId;
	private String title;
	private String content;
	private int type;
	private int isOfflineShow;
	private Date pushTime;
	private Date startTime;
	private Date endTime;
	private long expireTimes;
	private int state;
	public Long getMsgId() {
		return msgId;
	}
	public void setMsgId(Long msgId) {
		this.msgId = msgId;
	}
	public Long getAppId() {
		return appId;
	}
	public void setAppId(Long appId) {
		this.appId = appId;
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
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getIsOfflineShow() {
		return isOfflineShow;
	}
	public void setIsOfflineShow(int isOfflineShow) {
		this.isOfflineShow = isOfflineShow;
	}
	public Date getPushTime() {
		return pushTime;
	}
	public void setPushTime(Date pushTime) {
		this.pushTime = pushTime;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public long getExpireTimes() {
		return expireTimes;
	}
	public void setExpireTimes(long expireTimes) {
		this.expireTimes = expireTimes;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}


}
