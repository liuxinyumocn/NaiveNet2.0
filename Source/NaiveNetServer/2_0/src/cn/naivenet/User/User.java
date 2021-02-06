package cn.naivenet.User;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.json.JSONException;
import org.json.JSONObject;

import cn.domoe.naive.log.Log;
import cn.naivenet.Channel.ChannelManager;
import cn.naivenet.Channel.ChannelPool;
import cn.naivenet.ClientSocket.ClientHandler;
import cn.naivenet.ClientSocket.ClientSocketEvent;
import cn.naivenet.TimerEvent.Timer;

public class User {

	private ClientHandler clientHandler;
	private UserManager userManager;
	private ChannelManager channelManager;
	private ChannelPool channelPool;
	
	private byte level = 0;		// 用户的状态等级 0 未认证 1 认证
	private String sessionid = "";
	private long start_timestamp = 0;
	
	/**
	 * 	初始化用户句柄
	 * */
	public User(ClientHandler clientHandler, UserManager userManager, ChannelManager channelManager) {
		this.clientHandler = clientHandler;
		this.userManager = userManager;
		this.channelManager = channelManager;
		this.channelPool = this.channelManager.createChannelPool(this);
		this.start_timestamp = System.currentTimeMillis();
		this._initAuthCheck();
		this._initEvent();
		
	}
	
	private void removeEvent() {
		this.clientHandler.setOnCloseListener(null);
		this.clientHandler.setOnExceptionCaughtListener(null);
		this.clientHandler.setOnReadListener(null);
	}
	
	/**
	 * 	初始化相关回调函数
	 * */
	private void _initEvent() {
		
		this.clientHandler.setOnCloseListener(new ClientSocketEvent() {

			@Override
			public void on(ClientHandler handler, byte[] data) { //当发生网络句柄关闭事件
				removeEvent();
				clientHandler = null;
				//回收掉相关的事件
				Timer.CancelTimeout(timertask_auth);
				//如果用户已经完成授权 再最后保留一段时间 若仍然没有完成网络恢复 则彻底退出。否则直接退出
				if(isAuth()) {
					_initQuitCheck();
				}else {
					//直接退出
					quit();
				}
				
			}
			
		});
		
		this.clientHandler.setOnReadListener(new ClientSocketEvent() {

			@Override
			public void on(ClientHandler handler, byte[] data) {
				//接收来自客户端的消息
				NaiveNetUserMessage msg = new NaiveNetUserMessage(data,User.this);
				if(msg.control == 1) { //NS NC
					if(msg.channelid == 0) { //NS
						User.this.dealCToNS(msg);
					}else { //NC
						User.this.channelPool.dealClientToNC(msg);
					}
				}else if(msg.control == 0 || msg.control == 3) { //回复
					if(msg.channelid != 0) {
						User.this.channelPool.dealClientToNC(msg);
					}else { //对NS回复，目前版本客户端没有对NS的回复消息
						
					}
				}
			}
			
		});
		
	}
	
	private ScheduledFuture timertask_quit;
	
	/**
	 * 	初始化退出检测器
	 * */
	private void _initQuitCheck() {
		//通知所有的Channel发生了断线
		this.channelPool.onUserBreak();
		timertask_quit = Timer.SetTimeout(new Runnable() {

			@Override
			public void run() {
				//超时彻底断线
				quit();
			}
			
		}, this.userManager.getTimeOutQuit());
	}
	
	private ScheduledFuture timertask_auth;
	
	/**
	 * 	初始化认证检测器
	 * */
	private void _initAuthCheck() {
		
		if(this.level == 0) { //未认证状态下5000秒后将强制退出连接
			timertask_auth = Timer.SetTimeout(new Runnable() {

				@Override
				public void run() {
					if (level == 0) {
						//超时自动断线
						
						clientHandler.close();
					}
				}
				
			}, this.userManager.getNaiveNetServerHandler().config.getConf("USER_AUTH_TIMEOUT").getInt());
		}
		
	}
	
	
	//建立Module 与 Controller机制
	private List<NaiveNetBox> boxs = new ArrayList<>();
	/**
	 * 	添加存放Controller的box
	 * 	该box内的Controller仅对当前user实例有效（事件优先级比AddBox高）
	 * 	@param box 存放Controller集合容器
	 * */
	public void addBox(NaiveNetBox box) {
		boxs.add(box);
	}
	/**
	 * 	移除存放Controller的box
	 * 	该box内的Controller仅对当前user实例有效
	 * 	@param box 存放Controller集合容器句柄
	 * */
	public void removeBox(NaiveNetBox box) {
		boxs.remove(box);
	}
	private static List<NaiveNetBox> BOXs = new ArrayList<>();
	/**
	 * 	添加存放Controller的box
	 * 	该box内的Controller对所有User实例有效（事件优先级比addBox低）
	 * 	@param box 存放Controller集合容器
	 * */
	public static void AddBox(NaiveNetBox box) {
		BOXs.add(box);
	}
	/**
	 * 	移除存放Controller的box
	 * 	该box内的Controller对所有User实例有效
	 * 	@param box 存放Controller集合容器
	 * */
	public static void RemoveBox(NaiveNetBox box) {
		BOXs.remove(box);
	}
	
	/**
	 * 	处理客户端发往NS的请求操作
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
	 * 	回应客户端
	 * 	@param data NaiveNetResponseData 回应实例
	 * */
	public void responseClient(NaiveNetResponseData data) {
		if(data.getCancel())
			return;
		byte[] _data = data.genData();
		if(this.clientHandler != null)
			this.clientHandler.send(_data);
	}
	
	private String ping = "0";
	/**
	 * 	设置用户的ping值
	 * 	@param ping ping值
	 * */
	public void setPing(String ping) {
		this.ping = ping;
	}
	/**
	 * 	获取用户的ping值
	 * 	@return 返回ping值
	 * */
	public String getPing() {
		return ping;
	}

	/**
	 * 	为用户进行授权
	 * 	授权后的用户将不会被强制断开连接
	 * */
	public void auth() {
		if(this.isAuth())
			return;
		this.sessionid = this.userManager.authUser(this);
		this.level = 1;
		this.requestNSToClient("auth",this.sessionid.getBytes());
	}
	
	/**
	 * 	NS向Client发起请求
	 * */
	private void requestNSToClient(String controller, byte[] bytes) {
		NaiveNetRequestData req = new NaiveNetRequestData(
				1,0,0,controller,bytes
				);
		byte[] _data = req.genData();
		if(this.clientHandler != null)
			this.clientHandler.send(_data);
	}

	/**
	 * 	返回授权状态
	 * 	@return boolean true 授权成功 false 未授权
	 * */
	public boolean isAuth() {
		return this.level == 1;
	}

	/**
	 * 	获取用户的SESSIONID
	 * @return 
	 * */
	public String getSessionID() {
		return this.sessionid;
	}

	/**
	 * 	用户释放资源退出
	 * */
	public void quit() {
		if(this.clientHandler != null)
			this.clientHandler.close();
		this.userManager.removeUser(this);
		this.channelPool.quitAll();
	}

	/**
	 * 	请求进入特定的Channel
	 * 	@param msg 回复的用户句柄
	 * */
	public void enterChannel(NaiveNetMessage msg) {
		this.channelPool.enterChannel(msg);
	}

	/**
	 * 	请求退出已经进入的Channel
	 * 	@param channelID 频道ID
	 * */
	public void quitChannel(Integer channelID) {
		this.channelPool.quitChannel(channelID);
	}

	/**
	 * 	恢复网络通信句柄
	 * */
	public void recoverChannelHandler(NaiveNetMessage msg) {
		//this是旧的User，msg.user 是新的user 
		if(this.timertask_quit != null){
			Timer.CancelTimeout(this.timertask_quit);
			this.timertask_quit = null;
		}
		this.closeClientHandler();
		
		ClientHandler newHandler = msg.user.clientHandler;
		this.clientHandler = newHandler;
		this._initEvent();
		//新的User将被回收
		msg.user.release();
		
		this.channelPool.onUserRecover();
		
		//回复客户端
		NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.OK,true);
		this.responseClient(res);
	}

	/**
	 * 	获取用户的通信句柄
	 * */
	public ClientHandler getClientHandler() {
		return this.clientHandler;
	}
	
	/**
	 * 	关闭现有的通信句柄
	 * */
	private void closeClientHandler() {
		if(this.clientHandler != null) {
			this.removeEvent();
			this.clientHandler.close();
		}
	}
	
	/**
	 * 	回收当前User 通常在恢复状态时，新的User中的ClientHandler被移至旧的User，新User需要释放
	 * */
	public void release() {
		if(this.timertask_auth != null) {
			Timer.CancelTimeout(this.timertask_auth);
		}
		if(this.timertask_quit != null) {
			Timer.CancelTimeout(this.timertask_quit);
		}
		
		this.clientHandler = null;
		this.quit();
	}

	/**
	 * 	处理来自NC的数据
	 * */
	public void dealNCToC(NaiveNetUserMessage msg) {
		if(this.clientHandler != null)
			this.clientHandler.send(msg.data);
	}
	
	/*
	 * 用户句柄的会话机制
	 * */
	private ConcurrentHashMap<String,byte[]> sessionStore = new ConcurrentHashMap<>();

	/**
	 * 	设置SESSION数据
	 * */
	public void setSession(String key,byte[] value) {
		sessionStore.put(key, value);
	}

	/**
	 * 	获取SESSION数据
	 * */
	public byte[] getSession(String key) {
		return sessionStore.get(key);
	}
	
	/**
	 * 	清除SESSION数据
	 * */
	public void clearSession() {
		sessionStore.clear();
	}

	public String getLinkInfo() {
		JSONObject json = new JSONObject();
		try {
			InetSocketAddress ipSocket = (InetSocketAddress)this.clientHandler.getChannel().remoteAddress();
			json.put("ip", ipSocket.getAddress().getHostAddress());
			json.put("starttimestamp", this.start_timestamp);
			return json.toString();
		} catch (JSONException e) {
			
		}
		return "";
	}

}
