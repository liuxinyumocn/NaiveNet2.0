package cn.domoe.naivenet;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;

import cn.domoe.naivenet.Channel.NaiveNetChannelManager;
import cn.domoe.naivenet.Config.NaiveNetConfig;
import cn.domoe.naivenet.Server.NaiveServer;
import cn.domoe.naivenet.User.CodeMap;
import cn.domoe.naivenet.User.NaiveNetBox;
import cn.domoe.naivenet.User.NaiveNetController;
import cn.domoe.naivenet.User.NaiveNetMessage;
import cn.domoe.naivenet.User.NaiveNetResponseData;
import cn.domoe.naivenet.User.UserManager;

public class NaiveNetServerHandler {

	private final String version = "2.1.0";
	
	public UserManager userManager;
	public NaiveServer naiveServer;
	public NaiveNetConfig config;
	public NaiveNetChannelManager channelManager;
	
	private NaiveNetBox admin_box;
	private Thread checkThread;
	private int timeout = 5000;
	
	public NaiveNetServerHandler() throws Exception {
		System.out.println("NaiveNet Version "+version+" Powered by naivenet.domoe.cn");
		
		config = new NaiveNetConfig();
		userManager = new UserManager(this);
		naiveServer = new NaiveServer(this);
		channelManager = new NaiveNetChannelManager(this);
		
		//创建巡查线程触发器
		checkThread = new Thread(new CheckThread());
		checkThread.start();
		
		this.initAdminBox();
	}
	
	public NaiveNetBox getAdminBox() {
		return this.admin_box;
	}
	
	private void initAdminBox() {
		admin_box = new NaiveNetBox();
		//获取当前的Channel配置
		admin_box.addController(new NaiveNetController("getChannelConf") {

			@Override
			public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
				String json = config.getChannelConf();
				return new NaiveNetResponseData(msg,json.getBytes(),true);
			}
			
		});
		
		//设置当前的Channel配置
		admin_box.addController(new NaiveNetController("setChannelConf") {

			@Override
			public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
				try {
					String json = new String(msg.param,"utf-8");
					config.setChannelInfo(json);
				} catch (UnsupportedEncodingException e) {
					return new NaiveNetResponseData(msg,CodeMap.DATA_FORMAT_ERROR,false);
				} catch (Exception e) {
					return new NaiveNetResponseData(msg,CodeMap.DATA_FORMAT_ERROR,false);
				}
				return new NaiveNetResponseData(msg,CodeMap.OK,true);
			}
			
		});
		
	}
	
	private boolean checkstatus = true;
	class CheckThread implements Runnable{

		@Override
		public void run() {
			
			while(checkstatus) {
				
				userManager.checkData();
	
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
			
				}
			}
			
		}
		
	}
	
	public void launch() throws Exception {
		//读取配置文件
		String jks = config.getConf("SSL_JKS_FILEPATH").getStr();
		if(jks.equals("")) { //普通模式
			naiveServer.launch(config.getConf("SERVER_PORT").getInt());
		}else {
			//读取密码文件
			File file = new File(config.getConf("SSL_PASSWORD_FILEPATH").getStr());
			if(!file.exists()) {
				throw new Exception("SSL_PASSWORD_FILEPATH error");
			}
			FileInputStream fin = new FileInputStream(file.getAbsolutePath());
			byte[] password = new byte[fin.available()];
			fin.read(password);
			naiveServer.launch(config.getConf("SERVER_PORT").getInt(), jks, new String(password,"utf-8"));
		}
	}
	
	public void shutdown() {
		
	}

}
