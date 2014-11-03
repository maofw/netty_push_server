package com.netty.push.dao;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Service;

import com.netty.push.pojo.AppInfo;
import com.netty.push.pojo.DeviceInfo;
import com.netty.push.pojo.MessageDevice;
import com.netty.push.pojo.MessageInfo;
import com.netty.push.pojo.MessageOffline;
import com.netty.push.pojo.MessagePushedInfo;

@Service("pushDao")
@Scope("singleton")
public class PushDaoImpl extends AbstractBaseDao implements IPushDao {
	private static final String MESSAGEPUSHEDINFO_SQL = "merge into T_DEVELOPER_MESSAGE_PUSHED t using (select ? as DEVICE_ID,? as MSG_ID,? as STATE,? as PUSH_TIME,? as RECEIPT_TIME from dual) s on (t.DEVICE_ID = s.DEVICE_ID and t.MSG_ID = s.MSG_ID) when matched then update set t.RECEIPT_TIME = s.RECEIPT_TIME, t.STATE = s.STATE when not matched then insert(t.DEVICE_ID,t.MSG_ID,t.STATE,t.PUSH_TIME,t.RECEIPT_TIME) values(s.DEVICE_ID,s.MSG_ID,s.STATE,s.PUSH_TIME,s.RECEIPT_TIME) ";
	private static final String DEVICEINFO_SQL = "merge into T_DEVELOPER_REGISTRATION t using (select ? as REG_ID,? as USER_ID,? as APP_ID,? as APP_KEY, ? as IS_ONLINE, ? as DEVICE_ID,? as IMEI, ? as CHANNEL, ? as  ONLINE_TIME, ? as OFFLINE_TIME from dual) s on (t.REG_ID = s.REG_ID) when not matched then insert (t.REG_ID , t.USER_ID, t.APP_ID, t.APP_KEY, t.IS_ONLINE, t.DEVICE_ID, t.IMEI ,t.CHANNEL ,t.ONLINE_TIME, t.OFFLINE_TIME) values(s.REG_ID , s.USER_ID, s.APP_ID, s.APP_KEY, s.IS_ONLINE, s.DEVICE_ID, s.IMEI ,s.CHANNEL ,s.ONLINE_TIME, s.OFFLINE_TIME) when matched then update set t.USER_ID = s.USER_ID , t.APP_ID = s.APP_ID, t.APP_KEY = s.APP_KEY , t.IS_ONLINE = s.IS_ONLINE, t.DEVICE_ID = s.DEVICE_ID , t.IMEI = s.IMEI, t.CHANNEL = s.CHANNEL , t.ONLINE_TIME = s.ONLINE_TIME , t.OFFLINE_TIME = s.OFFLINE_TIME ";

	private static final String MESSAGEDEVICE_SQL = "merge into T_DEVELOPER_MESSAGE_DEVICE t using (select ? as MSG_ID,? as DEVICE_ID from dual) s on (t.DEVICE_ID = s.DEVICE_ID and t.MSG_ID = s.MSG_ID) when not matched then insert(t.DEVICE_ID,t.MSG_ID) values(s.DEVICE_ID,s.MSG_ID) ";

	@SuppressWarnings("unchecked")
	@Override
	public List<DeviceInfo> queryMessageDeviceList(Long appId, Long msgId) {
		if (msgId == null) {
			return null;
		}
		List<DeviceInfo> result = null;
		String sql = "select b.* from T_DEVELOPER_MESSAGE t, T_DEVELOPER_MESSAGE_DEVICE a,T_DEVELOPER_REGISTRATION b where t.MSG_ID = a.MSG_ID and a.DEVICE_ID=b.DEVICE_ID and sysdate>=t.START_TIME and sysdate<=t.END_TIME and t.STATE = 0 and a.MSG_ID = ? and b.APP_ID = ? and not exists(select 1 from T_DEVELOPER_MESSAGE_PUSHED b where a.MSG_ID = b.MSG_ID and a.DEVICE_ID = b.DEVICE_ID and ( b.state=1 or ( b.state=0 and t.EXPIRE_TIMES<>0 and ceil(sysdate-b.PUSH_TIME)*24*60*60<t.EXPIRE_TIMES ))) ";
		List<Map<String, Object>> list = this.getJdbcTemplate().queryForList(sql, new Object[] { msgId, appId });
		if (list != null && list.size() > 0) {
			result = new ArrayList<DeviceInfo>();
			for (Map<String, Object> map : list) {
				if (map == null || map.isEmpty()) {
					continue;
				}
				DeviceInfo deviceInfo = parseMapToDeviceInfo(map);
				if (deviceInfo != null) {
					result.add(deviceInfo);
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<MessageInfo> queryMessageList() {
		List<MessageInfo> result = null;
		String sql = "select * from T_DEVELOPER_MESSAGE where sysdate>=START_TIME and sysdate<=END_TIME and STATE = 0 order by MSG_ID";
		List<Map<String, Object>> list = this.getJdbcTemplate().queryForList(sql);
		if (list != null && list.size() > 0) {
			result = new ArrayList<MessageInfo>();
			for (Map<String, Object> map : list) {
				if (map == null || map.isEmpty()) {
					continue;
				}
				MessageInfo messageInfo = parseMapToMessageInfo(map);
				if (messageInfo != null) {
					result.add(messageInfo);
				}
			}
		}
		return result;
	}

	private MessageInfo parseMapToMessageInfo(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}
		MessageInfo messageInfo = new MessageInfo();
		BigDecimal bd = (BigDecimal) map.get("MSG_ID");
		messageInfo.setMsgId(bd.longValue());
		bd = (BigDecimal) map.get("APP_ID");
		messageInfo.setAppId(bd.longValue());
		messageInfo.setTitle((String) map.get("TITLE"));

		messageInfo.setContent((String) map.get("CONTENT"));

		bd = (BigDecimal) map.get("TYPE");
		messageInfo.setType(bd.intValue());

		bd = (BigDecimal) map.get("IS_OFFLINE_SHOW");
		messageInfo.setIsOfflineShow(bd.intValue());

		messageInfo.setStartTime((Date) map.get("START_TIME"));
		messageInfo.setEndTime((Date) map.get("END_TIME"));
		messageInfo.setPushTime((Date) map.get("PUSH_TIME"));

		bd = (BigDecimal) map.get("EXPIRE_TIMES");
		messageInfo.setExpireTimes(bd.longValue());

		bd = (BigDecimal) map.get("STATE");
		messageInfo.setState(bd.intValue());

		return messageInfo;
	}

	@Override
	public int saveMessagePushedInfo(MessagePushedInfo messagePushedInfo) {
		if (messagePushedInfo == null) {
			return -1;
		}
		
		return this.getJdbcTemplate().update(
				MESSAGEPUSHEDINFO_SQL,
				new Object[] { messagePushedInfo.getDeviceId(), messagePushedInfo.getMsgId(), messagePushedInfo.getState(), messagePushedInfo.getPushTime(),
						messagePushedInfo.getReceiptTime() });
	}

	@Override
	public int[] saveMessagePushedList(final List<MessagePushedInfo> messageDeviceList) {
		if (messageDeviceList == null || messageDeviceList.size() <= 0) {
			return null;
		}
		BatchPreparedStatementSetter setter = new BatchPreparedStatementSetter() {
			public int getBatchSize() {
				return messageDeviceList.size();
			}

			public void setValues(PreparedStatement ps, int i) {
				MessagePushedInfo messagePushedInfo = messageDeviceList.get(i);
				if (messagePushedInfo != null) {
					try {
						ps.setString(1, messagePushedInfo.getDeviceId());
						ps.setLong(2, messagePushedInfo.getMsgId());
						ps.setInt(3, messagePushedInfo.getState());
						if (messagePushedInfo.getPushTime() != null) {
							Timestamp ts = new Timestamp(messagePushedInfo.getPushTime().getTime());
							ps.setTimestamp(4, ts);
						} else {
							ps.setTimestamp(4, null);
						}
						if (messagePushedInfo.getReceiptTime() != null) {
							Timestamp ts = new Timestamp(messagePushedInfo.getReceiptTime().getTime());
							ps.setTimestamp(5, ts);
						} else {
							ps.setTimestamp(5, null);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		return getJdbcTemplate().batchUpdate(MESSAGEPUSHEDINFO_SQL, setter);
	}

	@Override
	public int saveMessage(MessageInfo messageInfo) {
		if (messageInfo == null) {
			return -1;
		}

		Long msgId = this.getPrimaryKey("SEQ_T_DEVELOPER_MESSAGE");
		messageInfo.setMsgId(msgId);

		String sql = "insert into T_DEVELOPER_MESSAGE(MSG_ID,APP_ID,TITLE, CONTENT, TYPE, IS_OFFLINE_SHOW, START_TIME, END_TIME, PUSH_TIME, EXPIRE_TIMES, STATE) values(?,?,?,?,?,?,?,?,?,?,?)";
		return this.getJdbcTemplate().update(
				sql,
				new Object[] { messageInfo.getMsgId(), messageInfo.getAppId(), messageInfo.getTitle(), messageInfo.getContent(), messageInfo.getType(),
						messageInfo.getIsOfflineShow(), messageInfo.getStartTime(), messageInfo.getEndTime(), messageInfo.getPushTime(),
						messageInfo.getExpireTimes(), messageInfo.getState() });
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DeviceInfo> queryDeviceListByAppId(Long appId) {
		if (appId == null) {
			return null;
		}
		List<DeviceInfo> result = null;
		String sql = "select t.* from T_DEVELOPER_REGISTRATION t where APP_ID = ?";
		List<Map<String, Object>> list = this.getJdbcTemplate().queryForList(sql, new Object[] { appId });
		if (list != null && list.size() > 0) {
			result = new ArrayList<DeviceInfo>();
			for (Map<String, Object> map : list) {
				if (map == null || map.isEmpty()) {
					continue;
				}
				DeviceInfo deviceInfo = parseMapToDeviceInfo(map);
				if (deviceInfo != null) {
					result.add(deviceInfo);
				}
			}
		}
		return result;
	}

	/**
	 * 查詢設備
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public DeviceInfo queryDeviceByDeviceId(String deviceId) {
		if (deviceId == null) {
			return null;
		}
		DeviceInfo deviceInfo = null;
		String sql = "select t.* from T_DEVELOPER_REGISTRATION t where DEVICE_ID = ?";
		List<Map<String, Object>> list = this.getJdbcTemplate().queryForList(sql, new Object[] { deviceId });
		if (list != null && list.size() > 0) {
			for (Map<String, Object> map : list) {
				if (map == null || map.isEmpty()) {
					continue;
				}
				deviceInfo = parseMapToDeviceInfo(map);
			}
		}
		return deviceInfo;
	}

	private DeviceInfo parseMapToDeviceInfo(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}
		DeviceInfo deviceInfo = new DeviceInfo();
		deviceInfo.setRegId((String) map.get("REG_ID"));
		BigDecimal bd = (BigDecimal) map.get("USER_ID");
		deviceInfo.setUserId(bd.longValue());
		bd = (BigDecimal) map.get("APP_ID");
		deviceInfo.setAppId(bd.longValue());
		deviceInfo.setAppKey((String) map.get("APP_KEY"));
		bd = (BigDecimal) map.get("IS_ONLINE");
		deviceInfo.setIsOnline(bd.intValue());
		deviceInfo.setDeviceId((String) map.get("DEVICE_ID"));
		deviceInfo.setImei((String) map.get("IMEI"));
		deviceInfo.setChannel((String) map.get("CHANNEL"));
		deviceInfo.setOnlineTime((Date) map.get("ONLINE_TIME"));
		deviceInfo.setOfflineTime((Date) map.get("OFFLINE_TIME"));
		return deviceInfo;
	}

	@Override
	public int saveOrUpdateDeviceInfo(DeviceInfo deviceInfo) {
		if (deviceInfo == null) {
			return -1;
		}
		return this.getJdbcTemplate().update(
				DEVICEINFO_SQL,
				new Object[] { deviceInfo.getRegId(), deviceInfo.getUserId(), deviceInfo.getAppId(), deviceInfo.getAppKey(), deviceInfo.getIsOnline(),
						deviceInfo.getDeviceId(), deviceInfo.getImei(), deviceInfo.getChannel(), deviceInfo.getOnlineTime(), deviceInfo.getOfflineTime() });

	}

	@Override
	public int[] updateDeviceInfoList(final List<DeviceInfo> deviceInfos) {
		if (deviceInfos == null || deviceInfos.size() <= 0) {
			return null;
		}
		BatchPreparedStatementSetter setter = new BatchPreparedStatementSetter() {
			public int getBatchSize() {
				return deviceInfos.size();
			}

			public void setValues(PreparedStatement ps, int i) {
				DeviceInfo deviceInfo = deviceInfos.get(i);
				if (deviceInfo != null) {
					try {
						ps.setString(1, deviceInfo.getRegId());
						ps.setLong(2, deviceInfo.getUserId());
						ps.setLong(3, deviceInfo.getAppId());
						ps.setString(4, deviceInfo.getAppKey());
						ps.setInt(5, deviceInfo.getIsOnline());
						ps.setString(6, deviceInfo.getDeviceId());
						ps.setString(7, deviceInfo.getImei());
						ps.setString(8, deviceInfo.getChannel());
						if (deviceInfo.getOnlineTime() != null) {
							Timestamp ts = new Timestamp(deviceInfo.getOnlineTime().getTime());
							ps.setTimestamp(9, ts);
						} else {
							ps.setTimestamp(9, null);
						}
						if (deviceInfo.getOfflineTime() != null) {
							Timestamp ts = new Timestamp(deviceInfo.getOfflineTime().getTime());
							ps.setTimestamp(10, ts);
						} else {
							ps.setTimestamp(10, null);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		return getJdbcTemplate().batchUpdate(DEVICEINFO_SQL, setter);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AppInfo> queryAppList() {
		List<AppInfo> result = null;
		String sql = "select * from T_DEVELOPER_APP";
		List<Map<String, Object>> list = this.getJdbcTemplate().queryForList(sql);
		if (list != null && list.size() > 0) {
			result = new ArrayList<AppInfo>();
			for (Map<String, Object> map : list) {
				if (map == null || map.isEmpty()) {
					continue;
				}
				AppInfo appInfo = new AppInfo();
				BigDecimal bd = (BigDecimal) map.get("APP_ID");
				appInfo.setAppId(bd == null ? null : bd.longValue());
				appInfo.setAppPackage((String) map.get("APP_PACKAGE"));
				appInfo.setAppKey((String) map.get("APP_KEY"));
				bd = (BigDecimal) map.get("USER_ID");
				appInfo.setUserId(bd.longValue());

				result.add(appInfo);
			}
		}
		return result;

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<MessageOffline> queryMessageOfflineList() {
		List<MessageOffline> result = null;
		String sql = "select * from T_DEVELOPER_MESSAGE_OFFLINE";
		List<Map<String, Object>> list = this.getJdbcTemplate().queryForList(sql);
		if (list != null && list.size() > 0) {
			result = new ArrayList<MessageOffline>();
			for (Map<String, Object> map : list) {
				if (map == null || map.isEmpty()) {
					continue;
				}
				MessageOffline messageOffline = new MessageOffline();
				BigDecimal bd = (BigDecimal) map.get("MSG_ID");
				messageOffline.setMsgId(bd == null ? null : bd.longValue());
				messageOffline.setDeviceId((String) map.get("DEVICE_ID"));
				result.add(messageOffline);
			}
		}
		return result;
	}

	@Override
	public int saveOrUpdateMessageOffline(MessageOffline messageOffline) {
		if (messageOffline == null) {
			return -1;
		}
		String sql = "merge into T_DEVELOPER_MESSAGE_OFFLINE t using (select ? as MSG_ID,? as DEVICE_ID from dual) s on (t.MSG_ID = s.MSG_ID and t.DEVICE_ID = s.DEVICE_ID) when not matched then insert(t.MSG_ID,t.DEVICE_ID) values(s.MSG_ID,s.DEVICE_ID) ";
		return this.getJdbcTemplate().update(sql, new Object[] { messageOffline.getMsgId(), messageOffline.getDeviceId() });
	}

	@Override
	public int deleteMessageOffline(MessageOffline messageOffline) {
		if (messageOffline == null) {
			return -1;
		}
		String sql = "delete from T_DEVELOPER_MESSAGE_OFFLINE t where t.MSG_ID = ? and t.DEVICE_ID = ?";
		return this.getJdbcTemplate().update(sql, new Object[] { messageOffline.getMsgId(), messageOffline.getDeviceId() });

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<MessageInfo> queryMessageOfflineList(String deviceId) {
		if (deviceId == null) {
			return null;
		}
		String sql = "select a.* from T_DEVELOPER_MESSAGE a ,T_DEVELOPER_MESSAGE_OFFLINE b where a.MSG_ID = b.MSG_ID and sysdate>=a.START_TIME and sysdate<=a.END_TIME and a.STATE = 0 and b.DEVICE_ID = ? and not exists(select 1 from T_DEVELOPER_MESSAGE_PUSHED x where a.MSG_ID = x.MSG_ID and b.DEVICE_ID = x.DEVICE_ID and ( x.state=1 or ( x.state=0 and a.EXPIRE_TIMES<>0 and ceil(sysdate-x.PUSH_TIME)*24*60*60<a.EXPIRE_TIMES )))";
		List<MessageInfo> result = null;
		List<Map<String, Object>> list = this.getJdbcTemplate().queryForList(sql, new Object[] { deviceId });
		if (list != null && list.size() > 0) {
			result = new ArrayList<MessageInfo>();
			for (Map<String, Object> map : list) {
				if (map == null || map.isEmpty()) {
					continue;
				}
				MessageInfo messageInfo = parseMapToMessageInfo(map);
				if (messageInfo != null) {
					result.add(messageInfo);
				}
			}
		}
		return result;
	}

	/**
	 * 获取已经推送消息列表
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, List<MessagePushedInfo>> queryMessagePushedInfo() {
		Map<String, List<MessagePushedInfo>> resMap = null;
		String sql = "select a.* from T_DEVELOPER_MESSAGE_PUSHED a ";
		List<Map<String, Object>> list = this.getJdbcTemplate().queryForList(sql);
		if (list != null && list.size() > 0) {
			resMap = new HashMap<String, List<MessagePushedInfo>>();
			for (Map<String, Object> map : list) {
				if (map == null || map.isEmpty()) {
					continue;
				}

				String deviceId = (String) map.get("DEVICE_ID");

				List<MessagePushedInfo> mapList = resMap.get(deviceId);
				if (mapList == null) {
					mapList = new ArrayList<MessagePushedInfo>();
					resMap.put(deviceId, mapList);
				}

				BigDecimal bd = (BigDecimal) map.get("MSG_ID");
				MessagePushedInfo messagePushedInfo = new MessagePushedInfo();
				messagePushedInfo.setDeviceId(deviceId);
				messagePushedInfo.setMsgId(bd.longValue());
				messagePushedInfo.setPushTime((Date) map.get("PUSH_TIME"));
				messagePushedInfo.setReceiptTime((Date) map.get("RECEIPT_TIME"));
				bd = (BigDecimal) map.get("STATE");
				messagePushedInfo.setState(bd.intValue());
				mapList.add(messagePushedInfo);
			}
		}
		return resMap;
	}

	/**
	 * 保存消息-设备列表
	 * 
	 * @param messageDevices
	 * @return
	 */
	public int[] saveOrUpdateMessageDeviceList(final List<MessageDevice> messageDevices) {

		if (messageDevices == null || messageDevices.size() <= 0) {
			return null;
		}
		BatchPreparedStatementSetter setter = new BatchPreparedStatementSetter() {
			public int getBatchSize() {
				return messageDevices.size();
			}

			public void setValues(PreparedStatement ps, int i) {
				MessageDevice messageDevice = messageDevices.get(i);
				if (messageDevice != null) {
					try {
						ps.setLong(1, messageDevice.getMsgId());
						ps.setString(2, messageDevice.getDeviceId());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		return getJdbcTemplate().batchUpdate(MESSAGEDEVICE_SQL, setter);
	}

	/**
	 * 根据app key和注册ids获取设备信息列表
	 * 
	 * @param appKey
	 * @param regIds
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DeviceInfo> queryDeviceInfoFromRegIds(String appKey, List<String> regIds) {
		if (appKey == null || regIds == null || regIds.size() <= 0) {
			return null;
		}
		List<DeviceInfo> result = null;
		String sql = "select * from T_DEVELOPER_REGISTRATION where APP_KEY = :appKey and REG_ID in (:regIds)  ";
		Map<String,Object> paramMap = new HashMap<String,Object>();
		paramMap.put("regIds", regIds);
		paramMap.put("appKey", appKey);
		List<Map<String, Object>> list = this.nettyNamedParameterJdbcTemplate.queryForList(sql, paramMap);
		if (list != null && list.size() > 0) {
			result = new ArrayList<DeviceInfo>();
			for (Map<String, Object> map : list) {
				if (map == null || map.isEmpty()) {
					continue;
				}
				DeviceInfo deviceInfo = parseMapToDeviceInfo(map);
				if (deviceInfo != null) {
					result.add(deviceInfo);
				}
			}
		}
		return result;
	}
}
