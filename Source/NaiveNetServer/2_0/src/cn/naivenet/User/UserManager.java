package cn.naivenet.User;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import cn.naivenet.NaiveNetServerHandler;
import cn.naivenet.ClientSocket.ClientHandler;

public class UserManager {

	private NaiveNetServerHandler naiveNetServer;
	
	//用户池
	private ConcurrentHashMap<String,User> logedUsers;			//SESSIONKEY 与 User的关系表 （已授权用户）
	private ConcurrentLinkedDeque<User> userpool;				//用户句柄池（包括授权用户与未授权用户）
	//private ConcurrentHashMap<ClientHandler,User> channelAndUserMap;  //网络句柄与用户句柄的映射
	
	private int timeout_quit = 60000*10;
	
	public UserManager(NaiveNetServerHandler s) {
		this.naiveNetServer = s;
		this.userpool = new ConcurrentLinkedDeque<>();
		this.logedUsers = new ConcurrentHashMap<>();
		
		this.registerEvent();
	}
	
	/**
	 * 	新的连接被创建
	 * */
	public void createUser(ClientHandler clientHandler) {
		
		User user = new User(clientHandler,this,this.naiveNetServer.channelManager);
		this.userpool.add(user);
		
	}
	
	/**
	 * 	为用户句柄注册相关的事件
	 * 	该事件对所有用户有效
	 * */
	private void registerEvent() {
		NaiveNetBox box = new NaiveNetBox();
		box.addController(new ControllerGetChannel(this.naiveNetServer));
		box.addController(new ControllerEnterChannel());
		box.addController(new ControllerQuitChannel());
		box.addController(new ControllerClose());
		box.addController(new ControllerRecover(this));
		box.addController(new ControllerHeart());
		box.addController(new ControllerAdmin(this.naiveNetServer));
		User.AddBox(box);	
	}
	
	/**
	 * 	获取naiveNerServerHandler
	 * */
	public NaiveNetServerHandler getNaiveNetServerHandler() {
		return this.naiveNetServer;
	}

	/**
	 * 	授权用户
	 * 	@param User 用户句柄
	 * */
	public String authUser(User user) {
		String sid = GenSessionID(32);
		this.logedUsers.put(sid, user);
		return sid;
	}
	
	/**
	 * 	取消授权用户
	 * 	@param User 用户句柄
	 * */
	public boolean cancelAuthUser(User user) {
		String id = user.getSessionID();
		if(id.equals("") && this.logedUsers.contains(id)) {
			this.logedUsers.remove(id);
			return true;
		}
		return false;
		
	}
	

	public static String GenSessionID(int length) {
		final String a = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		long timestamp = System.currentTimeMillis()/1000;
		String res = "";
		for(int i =0;i<length;i++) {
			int index = (int)((a.length()-1) * Math.random());
			res += a.substring(index, index+1);
		}
		return res+timestamp;
	}

	/**
	 * 	移除用户句柄
	 * 	@param User user 需要被移除的用户句柄
	 * 	@return boolean 如果正确移除返回true，否则返回false
	 * */
	public boolean removeUser(User user) {
		this.cancelAuthUser(user);
		return this.userpool.remove(user);
	}

	/**
	 * 	SESSIONID
	 * 	@param token
	 * */
	public User getUserBySESSION(String token) {
		User user = this.logedUsers.get(token);
		return user;
	}
	
	/**
	 * 	获取退出超时时间
	 * */
	public int getTimeOutQuit() {
		return this.timeout_quit;
	}

	/**
	 * 	设置退出超时时间
	 * */
	public void setTimeOutQuit(int value) {
		this.timeout_quit = value;
	}
	
	
	
}
