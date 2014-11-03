package com.netty.push;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.netty.push.server.IServer;

public class Application {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String params = (args != null && args.length > 0) ? args[0] : null;
		// 启动服务器
		ApplicationContext context = getApplicationContext(params);
		if (context != null) {
			IServer server = (IServer) context.getBean("nettyServer");
			server.start();
		} else {
			System.out.println("Please check spring-context.xml config correct!");
		}

	}

	private static ApplicationContext getApplicationContext(String params) {
		// 启动服务器
		ApplicationContext context = null;
		if (params == null) {
			context = new ClassPathXmlApplicationContext("classpath*:spring-context.xml");
		} else {
			System.out.println("params:" + params);
			context = new FileSystemXmlApplicationContext(params);
		}
		return context;
	}
}
