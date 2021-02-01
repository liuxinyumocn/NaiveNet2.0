package cn.naivenet;

public class NaiveNetServer {

	public NaiveNetServer() throws Exception {
		System.out.println("NaiveNetServer Version:"+VERSION+" powered by NaiveNet.CN");
		this.naivenetserver = new NaiveNetServerHandler();
	}
	private static String VERSION = "2.5.0";
	private static boolean debug = false;
	
	/**
	 * 	设置是否开启调试模式
	 * */
	public static void setDebug(boolean status) {
		debug = status;
	}
	
	/**
	 * 	获取调试模式的开启状态
	 * */
	public static boolean getDebug() {
		return debug;
	}
	
	private NaiveNetServerHandler naivenetserver;
	
	/**
	 * 	启动服务
	 * @throws Exception 启动失败时抛出异常
	 * */
	public void launch() throws Exception {
		this.naivenetserver.launch();
	}
	
	/**
	 * 	关闭服务
	 * @throws Exception 关闭失败时抛出异常
	 * */
	public void shutdown() throws Exception {
		this.naivenetserver.shutdown();
	}
	
	public static void main(String[] args) {
		
		try {
			new NaiveNetServer().launch();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
