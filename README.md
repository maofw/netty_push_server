netty_pusher
============

netty pusher framework，Currently It cantans server(SQL script) 、Java SDK and Android SDK 。The iOS SDK is developing!

Based netty-4.0.23.Final + spring build the push server platform！Already through simple test，and it is worked good。You can used Java SDK in your owner server,and it get from the jar fold.Also you can used Android SDK for your mobile development,it used very easy and simple！

Before you start ,must choose an oracle database for your server,and import the SQL script. Other database is developing!You can develop it yourself with your own database,only create a Java File in server project，in the package：com.netty.push.dao，and used another spring annotate for @Service("your named")。

==========================NETTY SERVER PUBLISH================================
start netty push server:  java -jar netty_push_server.jar spring-context.xml。
Must be current folder contans libs sub folder where contans the libs's jars


==========================JAVA SERVER SDK================================
jar/netty_server_sdk.jar:which is the Java Server SDK。Used the com.netty.push.sdk.client.NettyClient class create connection to the netty server。


Java Server SDK Example:

import java.util.ArrayList;
import java.util.List;

import com.netty.push.sdk.client.NettyClient;
import com.netty.push.sdk.client.listener.INettyClientHandlerListener;
import com.netty.push.sdk.pojo.Message;
import com.netty.push.sdk.pojo.MessageResult;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		//netty server ip
		String ip = "127.0.0.1";
		//netty server port
		int port = 6319;
		//create NettyClient with ip & port
		NettyClient nettyClient = NettyClient.getInstance(ip, port);
		//new Message Object
		Message message = new Message();
		//set app key
		message.setAppKey("5E4283368424E24641B990631988EE2C");
		//true:all devices can received    false:the message.devices can received
		message.setAllPush(false);
		//message title
		message.setTitle("test title");
		//message content 
		message.setContent("test content");
		//offline device is show or not
		message.setOfflineShow(true);
		//true: after terminal received the message it will send a receipt message to server
		message.setReceipt(true);
		//set message devices
		List<String> list = new ArrayList<String>(); 
		list.add("1800E6B16E5F77B4EE9747B1904F1F90");
		message.setDevices(list);		
		try {
			//send message method 
			//INettyClientHandlerListener is callback listener which tell you the sended message's status
			nettyClient.sendMessage(message, new INettyClientHandlerListener() {

				@Override
				public void receive(MessageResult result) {
					System.out.println("come in");
					if (result != null) {
						System.out.println(result.getMsgId() + "-" + result.isSuccess());
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
