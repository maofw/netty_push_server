package com.netty.push.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.netty.push.handler.context.ApplicationContext;

/**
 * Application初始化任务
 * 
 * @author maofw
 * 
 */
@Service("applicationInitTask")
@Scope("singleton")
public class ApplicationInitTask extends TimerTask {
	@Resource
	private ApplicationContext applicationContext;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public void run() {
		try{
			if (applicationContext != null) {
				System.out.println(sdf.format(new Date()) + "-APPLICATIONINITTASK EXECUTE!");
				applicationContext.init();
			}
		}catch(Exception e){
			e.printStackTrace();			
		}
	}
}
