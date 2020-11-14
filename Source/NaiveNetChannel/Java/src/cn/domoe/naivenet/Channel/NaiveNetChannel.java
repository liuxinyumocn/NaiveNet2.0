package cn.domoe.naivenet.Channel;

import java.io.IOException;

/**
 * NaiveNetChannel 启动类
 * */
public class NaiveNetChannel {

	private int port = 0;
	private String token = "";
	private UserManager userManager;
	private NaiveNetChannelServer naiveNetChannelServer;
	
	/**
	 *	 创建NaiveNetChannel实例 设定对外开放的Port与授权字符串
	 * */
	public NaiveNetChannel(int port,String token) {
		
		System.out.println("NaiveNetChannel Version 2.0.0 SingleIO Powered by naivenet.domoe.cn");
		
		this.port = port;
		this.token = token;
		this.userManager = new UserManager(this.token);
		this.naiveNetChannelServer = new NaiveNetChannelServer(this.port,this.userManager);
	}

	/**
	 * 	以默认配置下启动NaiveNetChannel
	 * */
	public void launch() throws IOException {
		this.launch(-1);
	}

	/**
	 * 	启动NaiveNetChannel
	 * 	并规定并发处理线程数量
	 * */
	public void launch(int MAX_THREAD) throws IOException {
		this.naiveNetChannelServer.launch(MAX_THREAD);
	}
	
	/**
	 * 	关停NaiveNetChannel 并释放所有资源
	 * */
	public void shutdown() throws Exception {
		this.naiveNetChannelServer.shutdown();
	}
	
	
	/**
	 * 	配置新用户进入Channel的监听器，当有新用户访问时触发监听器
	 * */
	public void setOnNewUserListener(NaiveNetEvent e) {
		this.userManager.setOnNewUserListener(e);
	}
	
}
