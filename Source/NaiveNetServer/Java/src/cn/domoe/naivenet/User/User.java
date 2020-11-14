package cn.domoe.naivenet.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cn.domoe.naivenet.Channel.NaiveNetChannelManager;
import cn.domoe.naivenet.Channel.NaiveNetChannelPool;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class User {

	Channel channel;
	UserManager userManager;
	
	public long start_timestamp; //客户创建时间
	public long end_timestamp; //结束连接时间
	
	private String sessionid;
	private boolean loged;
	public byte level = 0 ;
	public long break_timestamp = 0;
	public long lastmsg_timestamp = 0;
	
	private long inCounter = 0 ;
	private long outCounter = 0 ;
	
	public NaiveNetChannelPool channelPool;
	
	public User(Channel channel, UserManager userManager , NaiveNetChannelManager channelManager) {
		this.channel = channel;
		this.userManager = userManager;
		this.start_timestamp = System.currentTimeMillis();
		this.lastmsg_timestamp = this.start_timestamp;
		this.level = 1;
		this.loged = false;
		this.sessionStore = new ConcurrentHashMap<>();
		this.channelPool = new NaiveNetChannelPool(this,channelManager);
		
	}

	/**
	 * 	获取用户sessionid
	 * */
	public String getSessionID() {
		return this.sessionid;
	}
	
	/**
	 * 接收到来自客户端的数据消息
	 * */
	public void onMessage(byte[] data) {
		this.lastmsg_timestamp = System.currentTimeMillis();
		NaiveNetUserMessage msg = new NaiveNetUserMessage(data,this);
		if(msg.control == 1) { //NS NC
			if(msg.channelid == 0) { //NS
				this.dealCToNS(msg);
			}else { //NC
				this.dealCToNC(msg);
			}
		}else if(msg.control == 0 || msg.control == 3) { //回复
			if(msg.channelid != 0) { 
				this.channelPool.responseClientToNC(msg);
			}else { //对NS回复 目前版本客户端没有对NS的回复消息
				
			}
		}
		
	}
	
	/**
	 * 连接发生断开的事件
	 * */
	public void onBreak() {
		this.break_timestamp = System.currentTimeMillis();
		//如果用户处于非授权状态 则执行Close操作
		if(!this.isLog()) {
			this.onQuit();
			return;
		}
		
		//否则执行正常的断开操作
		
		try {
			//断开用户的Channel句柄
			this.channel.close().sync();
		} catch (InterruptedException e1) {
			
		}
		//移除UserManager的Channel与User的映射表
		this.userManager.removeChannelAndUser(this.channel);
		
		//变更连接状态
		this.level = 0;
		
		//通知已经连接的Channel User发生Break
		this.channelPool.onUserBreak();
		
	}


	/**
	 * 连接发生异常引发关闭
	 * */
	public void onQuit() {
		try {
			//断开用户的Channel句柄
			this.channel.close().sync();
		} catch (InterruptedException e1) {
			
		}
		//移除Channel与User的映射
		this.userManager.removeChannelAndUser(this.channel);
		
		//如果已经登录 则移除
		if(this.isLog()) {
			this.userManager.removeUser(this.sessionid);
		}else {
			this.userManager.removeUnlogUser(this);
		}

		//变更连接状态
		this.level = 0;
		
		//通知已经连接的Channel User发生Break
		this.channelPool.onUserQuit();
	}

	/**
	 * 入流量计数器
	 * */
	public void InCounter(int readableBytes) {
		this.inCounter += readableBytes;
		
	}

	public void OutCounter(int readableBytes) {
		this.outCounter += readableBytes;
		
	}
	
	//建立Module 与 Controller机制
	private List<NaiveNetBox> boxs = new ArrayList<>();
	public void addBox(NaiveNetBox mod) {
		boxs.add(mod);
	}
	public void removeBox(NaiveNetBox mod) {
		boxs.remove(mod);
	}
	private static List<NaiveNetBox> BOXs = new ArrayList<>();
	public static void AddBox(NaiveNetBox mod) {
		BOXs.add(mod);
	}
	public static void RemoveBox(NaiveNetBox mod) {
		BOXs.remove(mod);
	}
	
	/**
	 * 处理客户端发往NS的请求操作
	 * */
	private void dealCToNS(NaiveNetMessage msg) {
		NaiveNetResponseData res = null;
		for(int i = 0;i<this.boxs.size();i++) {
			res = this.boxs.get(i).deal((NaiveNetMessage)msg);
			if(res != null) {
				this.responseClient(res);
				return;
			}
		}
		for(int i=0;i<User.BOXs.size();i++) {
			res = User.BOXs.get(i).deal((NaiveNetMessage)msg);
			if(res != null) {
				this.responseClient(res);
				return;
			}
		}
		//未发现控制器
		NaiveNetResponseData nrd = new NaiveNetResponseData(msg,CodeMap.NOT_FOUNTD_CONTROLLER,false);
		this.responseClient(nrd);
	}
	
	/**
	 * 处理客户端发往NC的请求
	 * */
	public void dealCToNC(NaiveNetMessage msg) {
		//this.channelManager.dealCToNC(msg);
		this.channelPool.dealCToNC(msg);
		
	}
	
	/**
	 * 回应客户端
	 * */
	public void responseClient(NaiveNetResponseData data) {
		if(data.getCancel())
			return;
		byte[] _data = data.genData();
		this.send(_data);
	}
	
	/**
	 * 	NC发往Client的数据 原样回传给Client
	 * */
	public void dealNCToC(NaiveNetUserMessage nnm) {
		this.send(nnm.data);
	}
	
	/**
	 * 回应客户端 原样数据发送
	 * */
//	public void responseClient(NaiveNetUserMessage data) {
//		byte[] _data = data.data;
//		BinaryWebSocketFrame f = new BinaryWebSocketFrame();
//		f.content().writeBytes(_data);
//		this.channel.writeAndFlush(f);
//	}
	private void send(byte[] data) {
		BinaryWebSocketFrame f = new BinaryWebSocketFrame();
		f.content().writeBytes(data);
		this.channel.writeAndFlush(f);
	}
	

	/**
	 * 判断当前用户是否已经登录
	 * */
	public boolean isLog() {
		return this.loged;
	}

	/**
	 * 请求进入特定的客户端
	 * msg 回复的用户句柄
	 * channel 期望进入的频道ID
	 * */
	public void enterChannel(NaiveNetMessage msg) {
		this.channelPool.enterChannel(msg);
		
	}

	/**
	 * 为用户设置为授权装填
	 * */
	public void auth() {
		if(this.isLog())
			return;
		this.sessionid = this.userManager.authUser(this);
		this.loged = true;
		//通知用户你已经授权
		this.requestNSToClient("auth", this.sessionid.getBytes());
	}

	/*
	 * 用户句柄的会话机制
	 * */
	private ConcurrentHashMap<String,byte[]> sessionStore = null;
	public void setSession(String key,byte[] value) {
		System.out.println(key);
		//Log.print(value);
		sessionStore.put(key, value);
	}
	public byte[] getSession(String key) {
		return sessionStore.get(key);
	}
	public void clearSession() {
		sessionStore.clear();
	}
	
	/**
	 * 	NS向用户发起请求
	 * */
	private void requestNSToClient(String controller,byte[] param) {
		NaiveNetRequestData req = new NaiveNetRequestData(
				1,0,0,controller,param
				);
		byte[] _data = req.genData();
		this.send(_data);
	}

	/**
	 * 	当用户进行恢复时，将会话句柄进行替换，并关闭旧会话通道
	 * */
	public void replaceNetwork(User newUser) {
		this.channel = newUser.channel;
		this.level = 1;
		//通知所有已经建立连接的NC 该用户发生了网络恢复
		this.channelPool.onUserRecover();
	}

	private String ping = "0";
	/**
	 * 	设置用户的ping值
	 * */
	public void setPing(String ping) {
		this.ping = ping;
	}
	/**
	 * 	获取用户的ping值
	 * */
	public String getPing() {
		return ping;
	}


}
