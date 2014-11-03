package com.netty.push.server;

/**
 * server接口
 * 
 * @author maofw
 * 
 */
public interface IServer {
	public void start() throws Exception;

	public void stop() throws Exception;

	public void restart() throws Exception;
}
