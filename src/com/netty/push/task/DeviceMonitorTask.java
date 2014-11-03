package com.netty.push.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.netty.push.handler.context.ApplicationContext;

/**
 * 设备监控任务
 * 
 * @author maofw
 * 
 */
@Service("deviceMonitorTask")
@Scope("singleton")
public class DeviceMonitorTask extends TimerTask {
	// 超时时间
	private Long timeout;

	@Resource
	private ApplicationContext applicationContext;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public void run() {
		try{
			if (applicationContext != null) {
				System.out.println(sdf.format(new Date()) + "-DEVICEMONITORTASK EXECUTE!");
				applicationContext.deviceMonitors(timeout);
			}
		}catch(Exception e){
			e.printStackTrace();			
		}
	}

	public Long getTimeout() {
		return timeout;
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}
}
