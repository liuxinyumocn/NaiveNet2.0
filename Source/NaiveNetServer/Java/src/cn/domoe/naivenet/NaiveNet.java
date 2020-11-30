package cn.domoe.naivenet;

import io.netty.util.ResourceLeakDetector;

public class NaiveNet {
	
	private NaiveNetServerHandler server;
	public NaiveNet() throws Exception {
//		ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
		server = new NaiveNetServerHandler();
	}
	
	/**
	 * 启动服务
	 * @throws Exception 
	 * 
	 * */
	public void launch() throws Exception {
		server.launch();
	}
	
	/**
	 * 关闭服务
	 * 
	 * */
	public void shutdown() {
		server.shutdown();
	}
	
}
