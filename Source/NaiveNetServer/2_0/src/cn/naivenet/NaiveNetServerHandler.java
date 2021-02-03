package cn.naivenet;

import java.io.File;
import java.io.FileInputStream;

import cn.naivenet.Channel.ChannelManager;
import cn.naivenet.ClientSocket.ClientConnPool;
import cn.naivenet.ClientSocket.ClientHandler;
import cn.naivenet.ClientSocket.ClientSocketEvent;
import cn.naivenet.Config.NaiveNetConfig;
import cn.naivenet.User.NaiveNetBox;
import cn.naivenet.User.UserManager;

public class NaiveNetServerHandler {
	
	public ClientConnPool clientConnPoll;
	public ChannelManager channelManager;
	public UserManager userManager;
	public NaiveNetConfig config;
	
	public NaiveNetServerHandler() throws Exception {
		config = new NaiveNetConfig();
		channelManager = new ChannelManager(this);
		clientConnPoll = new ClientConnPool(this);
		userManager = new UserManager(this);

		userManager.setTimeOutQuit(config.getConf("USER_QUIT_TIMEOUT").getInt());
		this.initEvent();
		this.initAdminBox();
	}


	/**
	 * 	初始化关联事件
	 * */
	private void initEvent() {
		
		//设置新用户访问事件
		clientConnPoll.setOnNewClientEvent(new ClientSocketEvent() {

			@Override
			public void on(ClientHandler handler, byte[] data) {
				userManager.createUser(handler);
			}
			
		});
		
	}

	/**
	 * 	启动服务
	 * @throws Exception 启动失败时抛出异常
	 * */
	public void launch() throws Exception {
		String jks = config.getConf("SSL_JKS_FILEPATH").getStr();
		clientConnPoll.setTimeOutBreak(config.getConf("USER_BREAK_TIMEOUT").getInt());
		if(jks.equals("")) { //普通模式
			clientConnPoll.launch(config.getConf("SERVER_PORT").getInt());
		}else {
			//读取密码文件
			File file = new File(config.getConf("SSL_PASSWORD_FILEPATH").getStr());
			if(!file.exists()) {
				throw new Exception("SSL_PASSWORD_FILEPATH error");
			}
			FileInputStream fin = new FileInputStream(file.getAbsolutePath());
			byte[] password = new byte[fin.available()];
			fin.read(password);
			clientConnPoll.launch(config.getConf("SERVER_PORT").getInt(), jks, new String(password,"utf-8"));
		}
	}

	/**
	 * 	关闭服务
	 * @throws Exception 关闭失败时抛出异常
	 * */
	public void shutdown() throws Exception {
		clientConnPoll.shutdown();
	}

	/**
	 * 	获取NS管理员权限控制器Box
	 * 	@return Box对象
	 * */
	public NaiveNetBox getAdminBox() {
		return this.admin_box;
	}
	
	private NaiveNetBox admin_box;
	/**
	 * 	初始化管理员权限控制器Box
	 * */
	private void initAdminBox() {
		this.admin_box = new NaiveNetBox();
		this.admin_box.addController(new ControllerGetChannelConf(this.config));
		this.admin_box.addController(new ControllerSetChannelConf(this.config));
		
	}
	
	
}
