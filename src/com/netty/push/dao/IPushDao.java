package com.netty.push.dao;

import java.util.List;
import java.util.Map;

import com.netty.push.pojo.AppInfo;
import com.netty.push.pojo.DeviceInfo;
import com.netty.push.pojo.MessageDevice;
import com.netty.push.pojo.MessageInfo;
import com.netty.push.pojo.MessageOffline;
import com.netty.push.pojo.MessagePushedInfo;

public interface IPushDao {

	/**
	 * 保存消息到数据库
	 * 
	 * @return
	 */
	public int saveMessage(MessageInfo messageInfo);

	/**
	 * 获取push消息列表
	 * 
	 * @return
	 */
	public List<MessageInfo> queryMessageList();

	/**
	 * 获取device离线push消息列表
	 * 
	 * @return
	 */
	public List<MessageInfo> queryMessageOfflineList(String deviceId);

	/**
	 * 获取消息设备列表信息
	 * 
	 * @param msgId
	 * @return
	 */
	public List<DeviceInfo> queryMessageDeviceList(Long appId, Long msgId);

	/**
	 * 保存设备推送消息
	 * 
	 * @param messagePushedInfo
	 * @return
	 */
	public int saveMessagePushedInfo(MessagePushedInfo messagePushedInfo);

	/**
	 * 保存设备推送消息
	 * 
	 * @param messagePushedInfo
	 * @return
	 */
	public int[] saveMessagePushedList(List<MessagePushedInfo> messageDeviceList);

	/**
	 * 查詢所有設備列表
	 * 
	 * @return
	 */
	public List<DeviceInfo> queryDeviceListByAppId(Long appId);

	/**
	 * 查詢設備
	 * 
	 * @return
	 */
	public DeviceInfo queryDeviceByDeviceId(String deviceId);

	/**
	 * 更新设备信息
	 * 
	 * @param deviceInfo
	 * @return
	 */
	public int saveOrUpdateDeviceInfo(DeviceInfo deviceInfo);

	/**
	 * 批量更新设备信息列表
	 * 
	 * @param deviceInfos
	 * @return
	 */
	public int[] updateDeviceInfoList(List<DeviceInfo> deviceInfos);

	/**
	 * 查询App列表信息
	 * 
	 * @return
	 */
	public List<AppInfo> queryAppList();

	/**
	 * 获取离线消息-设备列表
	 * 
	 * @return
	 */
	public List<MessageOffline> queryMessageOfflineList();

	/**
	 * 保存离线消息到数据库
	 * 
	 * @param messageOffline
	 * @return
	 */
	public int saveOrUpdateMessageOffline(MessageOffline messageOffline);

	/**
	 * 刪除离线消息
	 * 
	 * @param msgId
	 * @param deviceId
	 * @return
	 */
	public int deleteMessageOffline(MessageOffline messageOffline);

	/**
	 * 获取已经推送消息列表
	 * 
	 * @return
	 */
	public Map<String, List<MessagePushedInfo>> queryMessagePushedInfo();

	/**
	 * 保存消息-设备列表
	 * 
	 * @param messageDevices
	 * @return
	 */
	public int[] saveOrUpdateMessageDeviceList(List<MessageDevice> messageDevices);

	/**
	 * 根据app key和注册ids获取设备信息列表
	 * 
	 * @param appKey
	 * @param regIds
	 * @return
	 */
	public List<DeviceInfo> queryDeviceInfoFromRegIds(String appKey, List<String> regIds);
}
