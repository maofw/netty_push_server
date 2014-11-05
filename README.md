Netty Push Server
============

The Netty Push Server Framework contains Netty Server (SQL script) 、Java SDK and Android SDK 。The iOS SDK is developing!

Based Netty-4.0.23.Final + Spring build the Netty Push Server Framework！Already through simple test，and it work's good。You can use Java SDK in your owner server,it will get from the jar folder . Also you can use Android SDK for your mobile development,it used very easy and simple！You can get it from the anetty_client project!

Before you start ,must choose an oracle database for your server,and import the SQL script. Other database is developing!You can develop it yourself with your own database,only create a Java File in server project，in the package：com.netty.push.dao，and used another spring annotate for @Service("your named")。



COMPILE & BUILD

You can used ant to compile & build the Netty Server,the build.xml is created in the root folder. If your computer is Windows Operating System you can run start.bat with the Ant Environment is configed.Last will create two jars：netty_push_server.jar & netty_server_sdk.jar in the jar folder。

netty_push_server.jar——The Push Server Library .That You can put in your self Web Project with support spring,and must load sping-context.xml in the web.xml。

netty_server_sdk.jar ——Your Server SDK Library .You can import the jar in your self project to send push message!



HOW TO START?

There are two kinds of methods：

1.Use nettyServer.start()  to run the push server in a suitable place of your project. For example：In the Application ServletContextListener. The nettyServer defined in spring-context.xml.

2.Use command line： java -jar netty_push_server.jar spring-context.xml to run the push server independent startup.
Above all , you must ensure that contains the  libs's jars in the classpath！



HOW USED JAVA SDK?

Java Server SDK：jar/netty_server_sdk.jar

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

