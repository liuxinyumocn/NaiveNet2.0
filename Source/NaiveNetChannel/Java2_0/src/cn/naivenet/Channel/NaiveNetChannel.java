package cn.naivenet.Channel;

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
		
		System.out.println("NaiveNetChannel Version 2.5.0 Powered by naivenet.cn");
		
		this.port = port;
		this.token = token;
		this.userManager = new UserManager();
		this.naiveNetChannelServer = new NaiveNetChannelServer(this.port,this.token);
		this.initEvent();
	}

	/**
	 * 	启动NaiveNetChannel
	 * 	并规定并发处理线程数量
	 * */
	public void launch() throws Exception {
		this.naiveNetChannelServer.launch();
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
	
	/**
	 * 	初始化事件
	 * */
	private void initEvent() {
		this.naiveNetChannelServer.setOnNewClientEvent(new ClientSocketEvent() {

			@Override
			public void on(ClientHandler handler, byte[] data) {
				userManager.createUser(handler);
			}
			
		});
	}
	
}
