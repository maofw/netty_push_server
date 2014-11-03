package com.netty.push.handler.context;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.protobuf.ByteString;
import com.netty.push.dao.IPushDao;
import com.netty.push.pojo.AppInfo;
import com.netty.push.pojo.ChannelDeviceInfo;
import com.netty.push.pojo.ChannelInfo;
import com.netty.push.pojo.DeviceInfo;
import com.netty.push.pojo.MessageDevice;
import com.netty.push.pojo.MessageInfo;
import com.netty.push.pojo.MessageOffline;
import com.netty.push.pojo.MessagePushedInfo;
import com.netty.push.utils.DateUtils;
import com.netty.push.utils.Md5Util;
import com.xwtec.protoc.CommandProtoc;

/**
 * 应用上下文环境
 * 
 * @author maofw
 * 
 */
@Service("applicationContext")
@Scope("singleton")
public class ApplicationContext {

	// ChannelGroup用于保存所有连接的客户端，注意要用static来保证只有一个ChannelGroup实例，否则每new一个TcpServerHandler都会创建一个ChannelGroup
	private static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

	// 存储所有设备列表
	// private static final Map<String, ChannelDevice> devices = new HashMap<String, ChannelDevice>();

	private static final Map<Channel, ChannelInfo> channelInfos = new HashMap<Channel, ChannelInfo>();

	// private static final Map<String, String> regDevices = new HashMap<String, String>();
	// 离线消息
	private static final List<MessageOffline> messageOfflines = new ArrayList<MessageOffline>();
	// app应用信息
	private static final Map<Long, AppInfo> apps = new HashMap<Long, AppInfo>();

	private static ApplicationContext applicationContext;
	/**
	 * 消息类型 0-系统消息 1-用户消息
	 */
	public static final int MESSAGE_TYPE_SYSTEM = 0;
	public static final int MESSAGE_TYPE_USER = 1;
	/**
	 * 消息状态状态 0-有效 1-无效
	 */
	public static final int MESSAGE_STATE_YES = 0;
	public static final int MESSAGE_STATE_NO = 1;
	/**
	 * 设备是否在线（1-在线 0-离线）
	 */
	public static final int DEVICE_ONLINE_YES = 1;
	public static final int DEVICE_ONLINE_NO = 0;
	/**
	 * 消息类型 0-群消息 1-点对点消息
	 */
	public static final int MESSAGE_TYPE_SEND_TO_ALL = 0;
	public static final int MESSAGE_TYPE_SEND_TO_POINT = 1;

	/**
	 * 离线是否可见 0-离线设备发送离线消息 1-离线设备不发送
	 */
	public static final int MESSAGE_OFFLINE_SHOW_YES = 0;
	public static final int MESSAGE_OFFLINE_SHOW_NO = 1;

	/**
	 * 设备-消息状态 0-初始状态（消息已发送尚未收到回执，下次轮询不能再发） 1-收到回执（不在发送相同消息） 2-push超时仍未收到回执
	 */
	public static final int MESSAGE_PUSHED_STATE_INIT = 0;
	public static final int MESSAGE_PUSHED_STATE_RECEIPT = 1;
	public static final int MESSAGE_PUSHED_STATE_FAILED = 2;

	/**
	 * 注册设备错误码
	 */
	public static final int REGIST_SUCCESS = 1;
	public static final int REGIST_FAILED = 0;
	public static final int REGIST_APPINFO_NULL = -100;
	public static final int REGIST_APPKEY_PACKAGE_NOT_MATCHED = -200;

	@Resource
	private IPushDao pushDao;

	public static ApplicationContext getInstance() {
		return applicationContext;
	}

	/**
	 * 初始化执行方法
	 */
	@PostConstruct
	public void init() {
		System.out.println(this.getClass().getName() + " INIT ...");
		// List<DeviceInfo> deviceList = pushDao.queryDeviceList();
		if (channelInfos != null && !channelInfos.isEmpty()) {
			Map<String, List<MessagePushedInfo>> messagePushedMap = pushDao.queryMessagePushedInfo();
			Iterator<Map.Entry<Channel, ChannelInfo>> iterator = channelInfos.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Channel, ChannelInfo> entry = iterator.next();
				ChannelInfo channelInfo = entry.getValue();
				if (channelInfo != null) {
					List<ChannelDeviceInfo> list = channelInfo.getChannelDeviceInfos();
					if (list != null && list.size() > 0) {
						for (ChannelDeviceInfo cdi : list) {
							if (cdi != null && cdi.getDeviceInfo() != null && cdi.getDeviceInfo().getDeviceId() != null) {
								cdi.setMessagePushedInfos(messagePushedMap.get(cdi.getDeviceInfo().getDeviceId()));
							}
						}
					}
				}
			}
		}

		// 加载离线设备列表
		List<MessageOffline> msgOfflines = pushDao.queryMessageOfflineList();
		if (msgOfflines != null && msgOfflines.size() > 0) {
			messageOfflines.clear();
			messageOfflines.addAll(msgOfflines);
		}
		// 加载app列表
		List<AppInfo> appList = pushDao.queryAppList();
		if (appList != null && appList.size() > 0) {
			apps.clear();
			for (AppInfo appInfo : appList) {
				apps.put(appInfo.getAppId(), appInfo);
			}
		}
		applicationContext = this;
		System.out.println(this.getClass().getName() + " INIT SUCCESS!!!\n");
	}

	/**
	 * 刷新心跳
	 */
	public void refreshHeart(Channel channel) {
		if (channel == null) {
			return;
		}

		if (channelInfos != null && !channelInfos.isEmpty() && channelInfos.containsKey(channel)) {
			ChannelInfo channelInfo = channelInfos.get(channel);
			if (channelInfo != null) {
				channelInfo.setHeartTime(System.currentTimeMillis());
			}
		}
	}

	/**
	 * 设备 心跳监控 超时的需要下线处理
	 */
	public void deviceMonitors(Long timeout) {
		if (channelInfos != null && !channelInfos.isEmpty()) {
			List<DeviceInfo> deviceListNew = new ArrayList<DeviceInfo>();
			Iterator<Map.Entry<Channel, ChannelInfo>> iterator = channelInfos.entrySet().iterator();

			StringBuffer sb = new StringBuffer();
			while (iterator.hasNext()) {
				Map.Entry<Channel, ChannelInfo> entry = iterator.next();
				ChannelInfo channelInfo = entry.getValue();
				if (channelInfo != null) {
					Long heartTime = channelInfo.getHeartTime() == null ? 0 : channelInfo.getHeartTime();
					
					Long cha = System.currentTimeMillis() - heartTime;
					
					sb.append("CHANNELINFO DEVICE SIZE:"+(channelInfo.getChannelDeviceInfos()==null?0:channelInfo.getChannelDeviceInfos().size())+" - HEARTTIME:"+heartTime +" - TIMEOUT:"+timeout+" - CHA:"+cha+"\n");
					
					if (cha >= timeout) {
						// 超时了已经
						List<ChannelDeviceInfo> list = channelInfo.getChannelDeviceInfos();
						if (list != null && list.size() > 0) {
							for (ChannelDeviceInfo cdi : list) {
								if (cdi != null && cdi.getDeviceInfo() != null) {
									DeviceInfo deviceInfo = cdi.getDeviceInfo();
									deviceInfo.setIsOnline(DEVICE_ONLINE_NO);
									deviceInfo.setOfflineTime(new Date());
									deviceListNew.add(deviceInfo);

									sb.append("DEVICE STATUS:" + (deviceInfo.getDeviceId() + "-" + deviceInfo.getImei()) + "-"
											+ (deviceInfo.getIsOnline() == DEVICE_ONLINE_YES ? "ONLINE" : "OFFLINE"));

									sb.append("-TIMEOUT OFFLINE!\n");
								}
							}
							list.clear();
						}
						// 关闭渠道信息
						Channel channel = entry.getKey();
						if (channel != null) {
							channel.close().addListener(ChannelFutureListener.CLOSE);
						}
						// 移除渠道信息
						iterator.remove();
					}

				}
			}
			pushDao.updateDeviceInfoList(deviceListNew);
			if (sb.length() > 0) {
				System.out.println(sb.toString());
				sb.delete(0, sb.length() - 1);
			}
			sb = null;
			deviceListNew.clear();
			deviceListNew = null;
		}
	}

	/**
	 * 获取所有离线设备信息
	 * 
	 * @return
	 */
	public List<MessageOffline> getMessageOfflines() {
		return messageOfflines;
	}

	/**
	 * 获得离线消息
	 * 
	 * @param msgId
	 * @param deviceId
	 * @return
	 */
	private MessageOffline getMessageOffline(Long msgId, String deviceId) {
		MessageOffline messageOffline = null;
		if (messageOfflines != null) {
			for (MessageOffline mf : messageOfflines) {
				if (mf == null) {
					continue;
				}
				if (mf.getMsgId() != null && mf.getMsgId().equals(msgId) && mf.getDeviceId() != null && mf.getDeviceId().equals(deviceId)) {
					messageOffline = mf;
					break;
				}
			}
		}
		return messageOffline;
	}

	/**
	 * 创建PUSH消息
	 * 
	 * @param appKey
	 * @param title
	 * @param content
	 * @param type
	 * @param isOfflinShow
	 * @param expireTimes
	 * @return
	 */
	public MessageInfo createMessageInfo(String appKey, String title, String content, int type, int isOfflineShow, long expireTimes, Date startTime,
			Date endTime) {

		AppInfo appInfo = this.getAppInfo(appKey);
		if (appInfo != null) {
			MessageInfo messageInfo = new MessageInfo();
			messageInfo.setAppId(appInfo.getAppId());
			messageInfo.setTitle(title);
			messageInfo.setContent(content);
			messageInfo.setType(type);
			messageInfo.setIsOfflineShow(isOfflineShow);
			messageInfo.setExpireTimes(expireTimes);
			messageInfo.setPushTime(new Date());
			messageInfo.setStartTime(startTime);
			messageInfo.setEndTime(endTime);
			messageInfo.setState(MESSAGE_STATE_YES);

			int x = this.pushDao.saveMessage(messageInfo);
			if (x > 0) {
				return messageInfo;
			}
		}
		return null;
	}

	/**
	 * 设备登陆后 发送默认消息
	 * 
	 * @param appPackage
	 * @param title
	 * @param content
	 * @param isReceipt
	 * @return
	 */
	public CommandProtoc.PushMessage getDefaultPushMessageForLogin(String deviceId, String title, String content, boolean isReceipt) {
		try {
			DeviceInfo deviceInfo = this.getDeviceInfo(deviceId);

			AppInfo appInfo = deviceInfo == null ? null : this.getAppInfo(deviceInfo.getAppKey());
			if (appInfo != null) {
				String appPackage = appInfo.getAppPackage();
				CommandProtoc.Message message = this.createCommandMessage(String.valueOf(Integer.MAX_VALUE), appPackage,
						CommandProtoc.Message.UserStatus.ONLINE, DateUtils.getDateString(new Date()), title, content.getBytes("UTF-8"), isReceipt,
						MESSAGE_TYPE_SYSTEM);
				CommandProtoc.PushMessage.Builder builder = this.createCommandPushMessage(CommandProtoc.PushMessage.Type.MESSAGE);
				builder.setMessage(message);
				return builder.build();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 保存消息
	 * 
	 * @param messageInfo
	 * @return
	 */
	public int[] saveMessageDevices(List<MessageDevice> messageDevices) {
		if (messageDevices == null) {
			return null;
		}
		return this.pushDao.saveOrUpdateMessageDeviceList(messageDevices);
	}

	/**
	 * 保存消息
	 * 
	 * @param messageInfo
	 * @return
	 */
	public int saveMessageInfo(MessageInfo messageInfo) {
		if (messageInfo == null) {
			return -1;
		}
		return this.pushDao.saveMessage(messageInfo);
	}

	/**
	 * 保存离线消息
	 * 
	 * @param messageOffline
	 */
	public synchronized MessageOffline saveMessageOffline(Long msgId, String deviceId) {
		MessageOffline messageOffline = this.getMessageOffline(msgId, deviceId);
		if (messageOffline == null) {
			messageOffline = new MessageOffline();
			messageOffline.setMsgId(msgId);
			messageOffline.setDeviceId(deviceId);
			messageOfflines.add(messageOffline);
			// 保存到数据库中
			pushDao.saveOrUpdateMessageOffline(messageOffline);
		}
		return messageOffline;
	}

	/**
	 * 删除离线设备消息
	 * 
	 * @param msgId
	 * @param deviceId
	 * @return
	 */
	public int deleteMessageOffline(Long msgId, String deviceId) {
		MessageOffline messageOffline = this.getMessageOffline(msgId, deviceId);
		if (messageOffline != null) {
			// 删除数据库中的信息
			int x = this.pushDao.deleteMessageOffline(messageOffline);
			// 刪除緩存中的信息
			if (x > 0) {
				messageOfflines.remove(messageOffline);
			}
			return x;
		}
		return 0;
	}

	/**
	 * 获取设备离线消息列表
	 * 
	 * @param deviceId
	 * @return
	 */
	public List<MessageInfo> getMessageOfflineOfDevice(String deviceId) {
		return this.pushDao.queryMessageOfflineList(deviceId);
	}

	/**
	 * 获取 设备信息
	 * 
	 * @param deviceId
	 * @return
	 */
	public DeviceInfo getDeviceInfo(String deviceId) {
		DeviceInfo deviceInfo = getDeviceInfoFromCache(deviceId);
		if (deviceInfo == null) {
			deviceInfo = this.pushDao.queryDeviceByDeviceId(deviceId);
		}
		return deviceInfo;
	}

	/**
	 * 获得DeviceInfo信息
	 * 
	 * @param deviceId
	 * @param list
	 * @return
	 */
	private DeviceInfo getDeviceInfoFromCache(String deviceId) {
		ChannelDeviceInfo channelDeviceInfo = getChannelDeviceInfoFromCache(deviceId);
		if (channelDeviceInfo != null) {
			return channelDeviceInfo.getDeviceInfo();
		}
		return null;
	}

	/**
	 * 从缓存中获取ChannelDeviceInfo
	 * 
	 * @param deviceId
	 * @return
	 */
	private ChannelDeviceInfo getChannelDeviceInfoFromCache(String deviceId) {
		ChannelDeviceInfo channelDeviceInfo = null;
		if (deviceId != null && channelInfos != null && !channelInfos.isEmpty()) {
			Iterator<Map.Entry<Channel, ChannelInfo>> iterator = channelInfos.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Channel, ChannelInfo> entry = iterator.next();
				ChannelInfo channelInfo = entry.getValue();
				if (channelInfo != null) {
					List<ChannelDeviceInfo> list = channelInfo.getChannelDeviceInfos();
					if (deviceId != null && list != null && list.size() > 0) {
						for (ChannelDeviceInfo cdi : list) {
							if (cdi != null && cdi.getDeviceInfo() != null && deviceId.equals(cdi.getDeviceInfo().getDeviceId())) {
								channelDeviceInfo = cdi;
								break;
							}
						}
					}
				}
			}
		}
		return channelDeviceInfo;
	}

	/**
	 * 获取AppInfo信息
	 * 
	 * @param appKey
	 * @return
	 */
	public AppInfo getAppInfo(String appKey) {
		if (appKey == null) {
			return null;
		}
		AppInfo tmpAppInfo = null;
		Iterator<Map.Entry<Long, AppInfo>> iterator = apps.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Long, AppInfo> entry = iterator.next();
			AppInfo appInfo = entry.getValue();
			if (appInfo != null && appInfo.getAppKey() != null && appInfo.getAppKey().equals(appKey)) {
				tmpAppInfo = appInfo;
				break;
			}
		}
		return tmpAppInfo;
	}

	/**
	 * 注册设备信息
	 * 
	 * @param deviceInfo
	 */
	public int registDevice(Channel channel, CommandProtoc.Registration registration) {
		if (registration != null) {
			AppInfo tmpAppInfo = getAppInfo(registration.getAppKey());
			if (tmpAppInfo == null) {
				return REGIST_APPINFO_NULL;
			}

			if (tmpAppInfo.getAppPackage() == null || !tmpAppInfo.getAppPackage().equals(registration.getAppPackage())) {
				// package与app key不对应
				return REGIST_APPKEY_PACKAGE_NOT_MATCHED;
			}
			DeviceInfo deviceInfo = null;
			ChannelInfo channelInfo = null;
			if (channelInfos.containsKey(channel)) {
				channelInfo = channelInfos.get(channelInfo);
			}
			if (channelInfo == null) {
				channelInfo = new ChannelInfo();
				channelInfos.put(channel, channelInfo);
			}
			// 获取渠道设备信息列表
			List<ChannelDeviceInfo> channelDeviceInfoList = channelInfo.getChannelDeviceInfos();
			if (channelDeviceInfoList == null) {
				channelDeviceInfoList = new ArrayList<ChannelDeviceInfo>();
				channelInfo.setChannelDeviceInfos(channelDeviceInfoList);
			}
			// 获得渠道设备信息
			ChannelDeviceInfo channelDeviceInfo = this.getChannelDeviceInfoFromCache(registration.getDeviceId());
			if (channelDeviceInfo == null) {
				channelDeviceInfo = new ChannelDeviceInfo();
				// 设置渠道
				channelDeviceInfo.setChannel(channel);
				// 设置已发送消息列表
				Map<String, List<MessagePushedInfo>> messagePushedMap = pushDao.queryMessagePushedInfo();
				if (messagePushedMap != null && !messagePushedMap.isEmpty()) {
					channelDeviceInfo.setMessagePushedInfos(messagePushedMap.get(registration.getDeviceId()));
				}
				channelDeviceInfoList.add(channelDeviceInfo);
			}

			if (deviceInfo == null) {
				deviceInfo = this.getDeviceInfo(registration.getDeviceId());
			}
			if (deviceInfo == null) {
				deviceInfo = new DeviceInfo();
			}
			// 设置设备信息内容
			channelDeviceInfo.setDeviceInfo(deviceInfo);
			if (deviceInfo.getRegId() == null) {
				// 重新创建一个ID
				String regId = Md5Util.toMD5(UUID.randomUUID().toString());
				deviceInfo.setRegId(regId);
				deviceInfo.setAppId(tmpAppInfo.getAppId());
				deviceInfo.setUserId(tmpAppInfo.getUserId());
				deviceInfo.setAppKey(tmpAppInfo.getAppKey());
				deviceInfo.setChannel(registration.getChannel());
				deviceInfo.setImei(registration.getImei());
				deviceInfo.setDeviceId(registration.getDeviceId());
				// 更新到数据库
				int r = pushDao.saveOrUpdateDeviceInfo(deviceInfo);
				if (r > 0) {
					return REGIST_SUCCESS;
				}
			} else {
				// 保存设备到缓存中
				// channelDevice.setChannel(channel);
				return REGIST_SUCCESS;
			}
		}
		return REGIST_FAILED;
	}

	/**
	 * 添加渠道到渠道组里面 用于群发消息
	 * 
	 * @param channel
	 * @return
	 */
	public boolean addChannel(Channel channel) {
		if (channel != null) {
			return channels.add(channel);
		}
		return false;
	}

	/**
	 * 关闭所有channel
	 * 
	 * @return
	 */
	public ChannelGroupFuture closeAllChannels() {
		if (channels != null && channels.size() > 0) {
			return channels.close();
		}
		return null;
	}

	/**
	 * 设备上线
	 * 
	 * @param deviceId
	 * @return
	 */
	public boolean online(String deviceId) {
		if (deviceId != null) {
			ChannelDeviceInfo channelDeviceInfo = this.getChannelDeviceInfoFromCache(deviceId);
			if (channelDeviceInfo != null) {
				ChannelInfo channelInfo = channelInfos.get(channelDeviceInfo.getChannel());
				// 更新心跳时间
				if (channelInfo != null) {
					channelInfo.setHeartTime(System.currentTimeMillis());
				}
				// 更新设备在线状态
				DeviceInfo deviceInfo = channelDeviceInfo.getDeviceInfo();
				if (deviceInfo != null) {
					deviceInfo.setIsOnline(DEVICE_ONLINE_YES);
					deviceInfo.setOnlineTime(new Date());
					int x = this.pushDao.saveOrUpdateDeviceInfo(deviceInfo);
					if (x > 0) {
						System.out.println("DEVICE ONLINE:" + deviceId + "-" + deviceInfo.getImei()+"\n");
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 设备下线
	 * 
	 * @param deviceId
	 * @return
	 */
	public boolean offline(String deviceId) {
		if (deviceId != null) {
			ChannelDeviceInfo channelDeviceInfo = this.getChannelDeviceInfoFromCache(deviceId);
			if (channelDeviceInfo != null) {
				// 更新设备在线状态
				DeviceInfo deviceInfo = channelDeviceInfo.getDeviceInfo();
				if (deviceInfo != null) {
					deviceInfo.setIsOnline(DEVICE_ONLINE_NO);
					deviceInfo.setOfflineTime(new Date());
					this.pushDao.saveOrUpdateDeviceInfo(deviceInfo);
				}
				Channel channel = channelDeviceInfo.getChannel();
				if (channel != null) {
					channel.close().addListener(ChannelFutureListener.CLOSE);
					ChannelInfo channelInfo = channelInfos.get(channel);
					if (channelInfo != null && channelInfo.getChannelDeviceInfos() != null) {
						channelDeviceInfo.setDeviceInfo(null);
						channelDeviceInfo.setMessagePushedInfos(null);
						channelInfo.getChannelDeviceInfos().remove(channelDeviceInfo);
					}
				}
				System.out.println("DEVICE OFFLINE:" + deviceId + (deviceInfo == null ? "" : ("-" + deviceInfo.getImei())));
				return true;
			}
		}
		return false;
	}

	/**
	 * 渠道下线
	 * 
	 * @param deviceId
	 * @return
	 */
	public boolean offline(Channel channel) {
		if (channel != null) {
			ChannelInfo channelInfo = channelInfos.get(channel);
			if (channelInfo != null && channelInfo.getChannelDeviceInfos() != null && channelInfo.getChannelDeviceInfos().size() > 0) {
				List<DeviceInfo> deviceInfos = new ArrayList<DeviceInfo>();
				List<ChannelDeviceInfo> channelDeviceInfos = channelInfo.getChannelDeviceInfos();
				for (ChannelDeviceInfo channelDeviceInfo : channelDeviceInfos) {
					if (channelDeviceInfo != null) {
						// 更新设备在线状态
						DeviceInfo deviceInfo = channelDeviceInfo.getDeviceInfo();
						if (deviceInfo != null) {
							deviceInfo.setIsOnline(DEVICE_ONLINE_NO);
							deviceInfo.setOfflineTime(new Date());
							deviceInfos.add(deviceInfo);
							System.out.println("DEVICE OFFLINE:" + deviceInfo.getDeviceId() + (deviceInfo == null ? "" : ("-" + deviceInfo.getImei())));
						}
					}
				}
				if (deviceInfos != null && deviceInfos.size() > 0) {
					this.pushDao.updateDeviceInfoList(deviceInfos);
				}
				channelInfo.getChannelDeviceInfos().removeAll(channelDeviceInfos);
			}
			channelInfos.remove(channel);
			channel.close().addListener(ChannelFutureListener.CLOSE);
			return true;
		}
		return false;
	}

	/**
	 * 发送所有消息
	 */
	public void sendAllMessages() {
		// 数据库获取消息列表内容
		List<MessageInfo> messages = pushDao.queryMessageList();
		if (messages != null && messages.size() > 0) {
			for (MessageInfo mi : messages) {
				if (mi == null) {
					continue;
				}
				// 发送消息 类型在线消息
				sendMessage(mi, CommandProtoc.Message.UserStatus.ONLINE);
			}
		}
	}

	/**
	 * 发送消息给终端设备
	 * 
	 * @param messageInfo
	 */
	public void sendMessage(MessageInfo messageInfo, CommandProtoc.Message.UserStatus status) {
		if (messageInfo != null) {
			switch (messageInfo.getType()) {
			case MESSAGE_TYPE_SEND_TO_ALL:
				// 群消息
				sendMessageToAll(messageInfo, status);
				break;
			case MESSAGE_TYPE_SEND_TO_POINT:
				// 点对点消息
				sendMessageToPoint(messageInfo, status);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * 群消息
	 * 
	 * @param messageInfo
	 */
	public void sendMessageToAll(MessageInfo messageInfo, CommandProtoc.Message.UserStatus status) {

		// 设备消息数据
		List<DeviceInfo> deviceList = this.pushDao.queryDeviceListByAppId(messageInfo.getAppId());
		if (deviceList == null || deviceList.size() <= 0) {
			return;
		}
		sendMessageToDevices(messageInfo, status, deviceList);
	}

	/**
	 * 点对点消息
	 * 
	 * @param messageInfo
	 */
	private void sendMessageToPoint(MessageInfo messageInfo, CommandProtoc.Message.UserStatus status) {
		// 获取该消息中是否存在 设备关联列表
		List<DeviceInfo> deviceList = this.pushDao.queryMessageDeviceList(messageInfo.getAppId(), messageInfo.getMsgId());
		if (deviceList == null || deviceList.size() <= 0) {
			return;
		}
		sendMessageToDevices(messageInfo, status, deviceList);
	}

	/**
	 * 发送PUSH消息
	 * 
	 * @param messageInfo
	 * @param status
	 * @param deviceList
	 */
	public void sendMessageToDevices(MessageInfo messageInfo, CommandProtoc.Message.UserStatus status, List<DeviceInfo> deviceList) {
		// System.out.println("sendMessageToDevices come in!");
		if (deviceList != null && deviceList.size() > 0) {
			// 创建消息对象
			CommandProtoc.PushMessage.Builder builder = this.createCommandPushMessage(CommandProtoc.PushMessage.Type.MESSAGE);
			CommandProtoc.PushMessage pushMessage = this.getRealPushMessage(builder, messageInfo, status);
			// 已经推送过的消息列表
			List<MessagePushedInfo> messageDeviceList = new ArrayList<MessagePushedInfo>();
			ChannelGroup mchannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

			for (DeviceInfo deviceInfo : deviceList) {
				if (deviceInfo == null) {
					continue;
				}
				// 获取设备消息发送对象
				MessagePushedInfo messagePushedInfo = this.makeMessageInfoToDevice(mchannels, messageInfo, deviceInfo);

				if (messagePushedInfo != null) {
					// 更新数据库数据
					if (messageDeviceList != null && messageDeviceList.size() >= 100) {
						// 提交数据库
						this.pushDao.saveMessagePushedList(messageDeviceList);
						messageDeviceList.clear();
					}
					messageDeviceList.add(messagePushedInfo);
				}
			}

			// 更新数据库数据
			if (messageDeviceList != null && messageDeviceList.size() > 0) {
				// 提交数据库
				this.pushDao.saveMessagePushedList(messageDeviceList);
				messageDeviceList.clear();
				messageDeviceList = null;
			}
			if (!mchannels.isEmpty()) {
				System.out.println("writeAndFlush pushMessage:=======================\n" + pushMessage);
				// 发送消息
				mchannels.writeAndFlush(pushMessage);
				mchannels.clear();
			}
			mchannels = null;
		}
	}

	/**
	 * 实际发送消息方法
	 * 
	 * @param pushMessage
	 * @param status
	 * @param messageInfo
	 * @param deviceId
	 * @return
	 */
	private MessagePushedInfo makeMessageInfoToDevice(ChannelGroup mchannels, MessageInfo messageInfo, DeviceInfo deviceInfo) {
		// System.out.println("makeMessageInfoToDevice come in!");
		// 获取设备消息发送对象
		MessagePushedInfo messagePushedInfo = getMessagePushedInfo(messageInfo, deviceInfo);
		if (messagePushedInfo != null) {
			// 发送消息
			if (deviceInfo != null && deviceInfo.getIsOnline() == DEVICE_ONLINE_YES) {
				// 如果设备在线 则添加发送通道
				ChannelDeviceInfo channelDeviceInfo = this.getChannelDeviceInfoFromCache(deviceInfo.getDeviceId());
				// System.out.println("makeMessageInfoToDevice channelDeviceInfo=" + channelDeviceInfo);
				Channel channel = channelDeviceInfo == null ? null : channelDeviceInfo.getChannel();
				if (channel != null && channel.isWritable()) {
					mchannels.add(channel);
				} else {
					return null;
				}
			}
		}
		return messagePushedInfo;
	}

	/**
	 * 保存回执信息
	 * 
	 * @param appKey
	 * @param msgId
	 * @param regId
	 * @return
	 */
	public int saveMessagePushedInfo(Channel channel, String appKey, String msgId, String regId) {

		AppInfo tmpAppInfo = getAppInfo(appKey);
		if (tmpAppInfo == null) {
			return -1;
		}
		if (msgId == null || regId == null) {
			return -1;
		}
		// 获取deviceId
		ChannelInfo channelInfo = channelInfos.get(channel);
		if (channelInfo == null) {
			return -1;
		}
		String deviceId = null;
		DeviceInfo deviceInfo = null;
		List<ChannelDeviceInfo> list = channelInfo.getChannelDeviceInfos();
		if (list != null && list.size() > 0) {
			for (ChannelDeviceInfo cdi : list) {
				if (cdi != null && cdi.getDeviceInfo() != null && regId.equals(cdi.getDeviceInfo().getRegId())) {
					deviceInfo = cdi.getDeviceInfo();
					break;
				}
			}
		}
		deviceId = (deviceInfo == null) ? null : deviceInfo.getDeviceId();
		if (deviceId == null) {
			return -1;
		}
		Long msgIdLong = Long.parseLong(msgId);
		// 获取设备信息
		MessagePushedInfo tmpMessagePushedInfo = getMessagePushedInfoFromCache(msgIdLong, deviceId);
		if (tmpMessagePushedInfo == null) {
			return -1;
		}
		// 更新内容后保存数据库
		tmpMessagePushedInfo.setReceiptTime(new Date());
		tmpMessagePushedInfo.setState(MESSAGE_PUSHED_STATE_RECEIPT);
		int rx = this.pushDao.saveMessagePushedInfo(tmpMessagePushedInfo);
		System.out.println("SAVEMESSAGEPUSHEDINFO MSGID:"+msgIdLong+" - DEVICEID:"+deviceId+" - RESULT:" + rx);
		if (rx > 0) {
			// 消息确认成功了 删除该设备的离线消息
			int m = this.deleteMessageOffline(msgIdLong, deviceId);
			System.out.println("DELETEMESSAGEOFFLINE MSGID:"+msgIdLong+" - DEVICEID:"+deviceId+" - RESULT:" + m);
		}
		return rx;
	}

	/**
	 * 获得设备列表
	 * 
	 * @param regIds
	 * @param appKey
	 * @return
	 */
	public List<DeviceInfo> queryDeviceInfoListByRegIds(String appKey, List<String> regIds) {
		return this.pushDao.queryDeviceInfoFromRegIds(appKey, regIds);
	}

	/**
	 * 从缓存中查找MessagePushedInfo对象
	 * 
	 * @param msgId
	 * @param deviceId
	 * @return
	 */
	private MessagePushedInfo getMessagePushedInfoFromCache(Long msgId, String deviceId) {
		// 获取设备信息
		ChannelDeviceInfo channelDeviceInfo = this.getChannelDeviceInfoFromCache(deviceId);
		if (channelDeviceInfo == null) {
			return null;
		}
		// 獲取目前的列表
		List<MessagePushedInfo> messagePushedList = channelDeviceInfo.getMessagePushedInfos();
		if (messagePushedList == null || messagePushedList.size() <= 0) {
			return null;
		}

		MessagePushedInfo tmpMessagePushedInfo = null;
		for (MessagePushedInfo messagePushedInfo : messagePushedList) {
			if (messagePushedInfo == null) {
				continue;
			}
			if (deviceId.equals(messagePushedInfo.getDeviceId()) && msgId.equals(messagePushedInfo.getMsgId())) {
				// 找到了之前发送的消息实体 则更新内容后保存数据库
				tmpMessagePushedInfo = messagePushedInfo;
				break;
			}
		}
		return tmpMessagePushedInfo;
	}

	/**
	 * 加入緩存中
	 * 
	 * @param messagePushedInfo
	 * @return
	 */
	private int addMessagePushedInfoToCache(String deviceId, MessagePushedInfo messagePushedInfo) {
		// 获取设备信息
		ChannelDeviceInfo channelDeviceInfo = this.getChannelDeviceInfoFromCache(deviceId);
		if (channelDeviceInfo == null) {
			return -1;
		}
		// 獲取目前的列表
		List<MessagePushedInfo> messagePushedList = channelDeviceInfo.getMessagePushedInfos();
		if (messagePushedList == null) {
			messagePushedList = new ArrayList<MessagePushedInfo>();
			messagePushedList.add(messagePushedInfo);
			channelDeviceInfo.setMessagePushedInfos(messagePushedList);
		} else {
			messagePushedList.add(messagePushedInfo);
		}
		return 1;
	}

	/**
	 * 更新发送设备信息到数据库
	 * 
	 * @param deviceId
	 * @return
	 */
	private MessagePushedInfo getMessagePushedInfo(MessageInfo messageInfo, DeviceInfo deviceInfo) {
		if (messageInfo == null || deviceInfo == null) {
			return null;
		}
		// 獲取appinfo信息
		AppInfo deviceAppInfo = apps.get(deviceInfo.getAppId());

		AppInfo messageAppInfo = apps.get(messageInfo.getAppId());
		if (deviceAppInfo != messageAppInfo) {
			return null;
		}
		// 判断设备是否离线
		if (isDeviceOffline(deviceInfo)) {
			// 如果设备已经离线了 则关闭相应的channel
			ChannelDeviceInfo channelDeviceInfo = this.getChannelDeviceInfoFromCache(deviceInfo.getDeviceId());
			if (channelDeviceInfo != null) {
				Channel channel = channelDeviceInfo.getChannel();
				if (channel != null && channel.isOpen()) {
					channel.close().addListener(ChannelFutureListener.CLOSE);
				}
			}
			// 离线的消息就不需要发送了 直接保存到离线消息中
			if (messageInfo.getIsOfflineShow() == MESSAGE_OFFLINE_SHOW_YES) {
				// 保存离线消息
				this.saveMessageOffline(messageInfo.getMsgId(), deviceInfo.getDeviceId());
				return null;
			}
		} else {
			MessagePushedInfo messagePushedInfo = getMessagePushedInfoFromCache(messageInfo.getMsgId(), deviceInfo.getDeviceId());
			if (messagePushedInfo == null) {
				messagePushedInfo = new MessagePushedInfo();
				messagePushedInfo.setDeviceId(deviceInfo.getDeviceId());
				messagePushedInfo.setMsgId(messageInfo.getMsgId());
				messagePushedInfo.setPushTime(new Date());
				// 超时时间 0-无超时（认为发送即成功，无需客户端确认回执）
				if (messageInfo.getExpireTimes() == 0) {
					// 默认设置设备已经确认了
					messagePushedInfo.setReceiptTime(new Date());
					messagePushedInfo.setState(MESSAGE_PUSHED_STATE_RECEIPT);
				} else {
					messagePushedInfo.setState(MESSAGE_PUSHED_STATE_INIT);
				}

				this.addMessagePushedInfoToCache(deviceInfo.getDeviceId(), messagePushedInfo);
			} else if (messagePushedInfo.getState() == MESSAGE_PUSHED_STATE_RECEIPT) {
				// 已經回執過了 不需要再發送
				return null;
			}

			return messagePushedInfo;
		}
		return null;
	}

	/**
	 * 判断设备是否离线
	 * 
	 * @param channelDevice
	 * @return
	 */
	private boolean isDeviceOffline(DeviceInfo deviceInfo) {
		if (deviceInfo != null) {
			if (deviceInfo == null || deviceInfo.getIsOnline() == DEVICE_ONLINE_NO) {
				return true;
			}
			return false;

		}
		return true;
	}

	/**
	 * 群消息
	 * 
	 * @param messageInfo
	 */
	private CommandProtoc.PushMessage getRealPushMessage(CommandProtoc.PushMessage.Builder builder, MessageInfo messageInfo,
			CommandProtoc.Message.UserStatus status) {
		AppInfo appinfo = apps.get(messageInfo.getAppId());
		if (appinfo != null) {
			CommandProtoc.Message message = null;
			try {
				message = this.createCommandMessage(String.valueOf(messageInfo.getMsgId()), appinfo.getAppPackage(), status,
						DateUtils.getDateString(messageInfo.getPushTime()), messageInfo.getTitle(), messageInfo.getContent().getBytes("UTF-8"),
						(messageInfo.getExpireTimes() == 0 ? false : true), MESSAGE_TYPE_USER);
				builder.setMessage(message);
				// 生成PushMessage消息对象
				CommandProtoc.PushMessage pushMessage = builder.build();
				return pushMessage;
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 创建发送消息对象
	 * 
	 * @param type
	 * @return
	 */
	private CommandProtoc.PushMessage.Builder createCommandPushMessage(CommandProtoc.PushMessage.Type type) {
		CommandProtoc.PushMessage.Builder builder = CommandProtoc.PushMessage.newBuilder();
		builder.setType(type);
		return builder;
	}

	/**
	 * 创建RegistrationResult对象
	 * 
	 * @param type
	 * @return
	 */
	public CommandProtoc.PushMessage createCommandRegistrationResult(String appKey, String appPackage, String registrationId) {
		CommandProtoc.RegistrationResult.Builder builder = CommandProtoc.RegistrationResult.newBuilder();
		builder.setAppKey(appKey);
		builder.setAppPackage(appPackage);
		if (registrationId != null) {
			builder.setRegistrationId(registrationId);
		}
		CommandProtoc.RegistrationResult commandProtoc = builder.build();
		// 创建消息对象
		CommandProtoc.PushMessage.Builder messageBuilder = this.createCommandPushMessage(CommandProtoc.PushMessage.Type.REGISTRATION_RESULT);
		messageBuilder.setRegistrationResult(commandProtoc);
		return messageBuilder.build();
	}

	/**
	 * 创建DeviceOnoffResult对象
	 * 
	 * @param type
	 * @return
	 */
	public CommandProtoc.PushMessage createCommandDeviceOnoffResult(String appPackage, CommandProtoc.DeviceOnoffResult.ResultCode resultCode,
			CommandProtoc.Message.UserStatus userStatus) {
		CommandProtoc.DeviceOnoffResult.Builder builder = CommandProtoc.DeviceOnoffResult.newBuilder();
		if (appPackage != null) {
			builder.setAppPackage(appPackage);
		}
		builder.setResCode(resultCode);
		builder.setUserStatus(userStatus);
		CommandProtoc.DeviceOnoffResult commandProtoc = builder.build();
		// 创建消息对象
		CommandProtoc.PushMessage.Builder messageBuilder = this.createCommandPushMessage(CommandProtoc.PushMessage.Type.DEVICE_ONOFFLINE_RESULT);
		messageBuilder.setDeviceOnoffResult(commandProtoc);
		return messageBuilder.build();
	}

	/**
	 * 创建Message对象
	 * 
	 * @param type
	 * @return
	 */
	private CommandProtoc.Message createCommandMessage(String msgId, String appPackage, CommandProtoc.Message.UserStatus userStatus, String pushTime,
			String title, byte[] content, boolean isReceipt, int type) {
		CommandProtoc.Message.Builder builder = CommandProtoc.Message.newBuilder();
		builder.setAppPackage(appPackage);
		builder.setStatus(userStatus);
		builder.setPushTime(pushTime);
		builder.setTitle(title);
		builder.setContent(ByteString.copyFrom(content));
		builder.setIsReceipt(isReceipt);
		builder.setMsgId(msgId);
		builder.setType(type);
		CommandProtoc.Message commandProtoc = builder.build();
		return commandProtoc;
	}

	/**
	 * 销毁
	 */
	public void destory() {
		channelInfos.clear();
		messageOfflines.clear();
		apps.clear();
		channels.clear();

	}

}
