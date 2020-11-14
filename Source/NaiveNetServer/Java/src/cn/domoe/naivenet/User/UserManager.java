package cn.domoe.naivenet.User;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import cn.domoe.naivenet.NaiveNetServerHandler;
import cn.domoe.naivenet.Config.ChannelInfo;
import io.netty.channel.Channel;

public class UserManager {

	private NaiveNetServerHandler naiveNetServer;
	
	public UserManager(NaiveNetServerHandler naiveNetServer) {
		this.naiveNetServer = naiveNetServer;
		
		logedUsers = new ConcurrentHashMap<>();
		unlogUsers = new ConcurrentLinkedDeque<>();
		channelAndUserMap = new ConcurrentHashMap<>();
		
		
		this.registerEvent();
	}
	
	//用户池
	private ConcurrentHashMap<String,User> logedUsers;			//SESSIONKEY 与 User的关系表
	private ConcurrentLinkedDeque<User> unlogUsers;				//未登录 或 用于恢复的用户的句柄
	private ConcurrentHashMap<Channel,User> channelAndUserMap;  //网络句柄与用户句柄的映射
	
	/**
	 * 新的用户连接创建
	 * */
	public void createUser(Channel channel) {
		User user = new User(channel,this, this.naiveNetServer.channelManager);
		this.channelAndUserMap.put(channel, user);
		this.unlogUsers.add(user);
	}

	public User FindUser(Channel channel) {
		return this.channelAndUserMap.get(channel);
	}
	
	private void registerEvent() {
		NaiveNetBox box = new NaiveNetBox();
		box.addController(new ControllerGetChannel());
		box.addController(new ControllerEnterChannel());
		box.addController(new ControllerQuitChannel());
		box.addController(new ControllerClose());
		box.addController(new ControllerRecover());
		box.addController(new ControllerHeart());

		User.AddBox(box);
	}
	
	class ControllerHeart extends NaiveNetController {

		public ControllerHeart() {
			super("heart");
		}
		
		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			msg.user.setPing(new String(msg.param));
			return null;
		}
		
	}
	
	
	class ControllerGetChannel extends NaiveNetController {

		public ControllerGetChannel() {
			super("getChannel");

		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			String channel = new String(msg.param);
			ChannelInfo n = UserManager.this.naiveNetServer.config.getChannelInfo(channel);
			if(n == null) {
				NaiveNetResponseData nrd = new NaiveNetResponseData(msg,CodeMap.NOT_FOUND_CHANNEL,false);
				return nrd;
			}
			
			String id = Integer.toString(n.getID());
			
			NaiveNetResponseData res = new NaiveNetResponseData(msg,id.getBytes(),true);
			return res;
		}
		
	}
	
	//请求与对应Channel建立连接
	class ControllerEnterChannel extends NaiveNetController {

		public ControllerEnterChannel() {
			super("enterChannel");
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			User user = msg.user;
			user.enterChannel(msg);
			NaiveNetResponseData res = new NaiveNetResponseData();
			return res;
		}
		
	}
	
	/**
	 * 	客户端请求退出某个频道
	 * 	param 是频道ID
	 * */
	class ControllerQuitChannel extends NaiveNetController {

		public ControllerQuitChannel() {
			super("quitChannel");

		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			try {
				Integer channelID = Integer.parseInt(new String(msg.param));
				//退出特定的频道
				msg.user.channelPool.quitChannel(channelID);
				return null;
			}catch(Exception e) {
				NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.DATA_FORMAT_ERROR,false);
				return res;
			}
		}
		
	}
	
	/**
	 * 	客户端申请关闭连接（不可恢复 资源全部释放）
	 * */
	class ControllerClose extends NaiveNetController {

		public ControllerClose() {
			super("close");

		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			msg.user.onQuit();
			return null;
		}
		
	}
	
	/**
	 * 	客户端此前发生断线，请求进行连接恢复
	 * */
	class ControllerRecover extends NaiveNetController {

		public ControllerRecover() {
			super("recover");
			// TODO Auto-generated constructor stub
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			//使用网络恢复功能当前用户不能是已经Auth的状态
			if(msg.user.isLog()) {
				NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.RECOVER_FAILED,false);
				return res;
			}
			
			String token = new String(msg.param);
			User user = UserManager.this.logedUsers.get(token);
			if(user == null) { //说明没有用户信息或者用户信息错误 无法进行网络恢复
				NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.RECOVER_FAILED,false);
				return res;
			}
			//存在则恢复
			Channel old = user.channel;
			//先替换当前的句柄映射
			user.replaceNetwork(msg.user); 					//将新的Channel替换到旧的User的channel
			channelAndUserMap.put(msg.user.channel, user);	//将新的Channel与旧的User进行关联
			channelAndUserMap.remove(old);			//移除旧的Channel与旧的User的关联
			unlogUsers.remove(msg.user);					//未登录列表中移除新的User
			//关闭掉旧的UserChannel
			try {
				old.close().sync();
			} catch (InterruptedException e) {
				
			}
			return null;
		}
		
	}

	/**
	 * 	用户连接发生断开时移除映射
	 * */
	public void removeChannelAndUser(Channel channel) {
		try {
			this.channelAndUserMap.remove(channel);
		}catch(Exception e) {
			
		}
	}
	/**
	 * 	移除已经登录的用户信息
	 * */
	public void removeUser(String sessionKey) {
		try {
			this.logedUsers.remove(sessionKey);
		}catch(Exception e) {
			
		}
	}

	/**
	 * 	移除未登录的用户信息
	 * */
	public void removeUnlogUser(User user) {
		try {
			this.unlogUsers.remove(user);
		}catch(Exception e) {
			
		}
	}

	/**
	 * 	周期性进行数据检测
	 * */
	public void checkData() {
		long now = System.currentTimeMillis();
		
		long outtime = now - this.naiveNetServer.config.getConf("USER_AUTH_TIMEOUT").getInt();
		
		//清理没有及时Auth的用户
		Iterator<User> it = this.unlogUsers.iterator();
		ArrayList<User> list = new ArrayList<>();
		while(it.hasNext()) {
			User user = it.next();
			if(outtime > user.start_timestamp) {
				list.add(user);
			}
		}
		
		for(int i = 0;i<list.size();i++) {
			list.get(i).onQuit();
		}
		
		list.clear();
		
		//检测心跳超时的用户以及已经断线且断线时长超过最大数据保留时长的用户
		outtime = now - this.naiveNetServer.config.getConf("USER_BREAK_TIMEOUT").getInt();
		long outtime_quit =  now - this.naiveNetServer.config.getConf("USER_QUIT_TIMEOUT").getInt();
		
		Iterator it2 = this.logedUsers.entrySet().iterator();
		//list 心跳超时用户
		//list2 退出用户
		ArrayList<User> list2 = new ArrayList<>();
		while(it2.hasNext()) {
			Map.Entry entry = (Map.Entry)it2.next();
			String key = entry.getKey().toString();
			User user = (User)entry.getValue();
			if(user.level == 1 && user.lastmsg_timestamp < outtime) { //发生心跳超时的断线
				list.add(user);
			}else if(user.level == 0 && user.break_timestamp < outtime_quit) { //对用户进行回收
				list2.add(user);
			}
		}

		for(int i = 0;i<list.size();i++) {
			list.get(i).onBreak();
		}
		
		for(int i = 0;i<list2.size();i++) {
			list2.get(i).onQuit();
		}
	}

	/**
	 * 	设置用户为授权状态
	 * */
	public String authUser(User user) {
		this.unlogUsers.remove(user);
		long timestamp = System.currentTimeMillis()/1000;
		String sessionid = UserManager.GenSessionID(32)+timestamp;
		this.logedUsers.put(sessionid,user);
		return sessionid;
	}
	
	public static String GenSessionID(int length) {
		final String a = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890a";
		String res = "";
		for(int i =0;i<length;i++) {
			int index = (int)((a.length()-1) * Math.random());
			res += a.substring(index, index+1);
		}
		return res;
	}
	
	/**
	 * 	注销已经登录的用户数据
	 * */
	public void unlog(User user) {
		this.logedUsers.remove(user.getSessionID());
	}
}
