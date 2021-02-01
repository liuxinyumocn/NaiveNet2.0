package cn.naivenet.User;

import java.util.ArrayList;
import java.util.List;

import cn.naivenet.Channel.ChannelManager;
import cn.naivenet.Channel.ChannelPool;
import cn.naivenet.ClientSocket.ClientHandler;
import cn.naivenet.ClientSocket.ClientSocketEvent;
import cn.naivenet.TimerEvent.Task;
import cn.naivenet.TimerEvent.Timer;
import cn.naivenet.TimerEvent.TimerTask;

public class User {

	private ClientHandler clientHandler;
	private UserManager userManager;
	private ChannelManager channelManager;
	private ChannelPool channelPool;
	
	private byte level = 0;		// 用户的状态等级 0 未认证 1 认证
	private String sessionid = "";
	
	/**
	 * 	初始化用户句柄
	 * */
	public User(ClientHandler clientHandler, UserManager userManager, ChannelManager channelManager) {
		this.clientHandler = clientHandler;
		this.userManager = userManager;
		this.channelManager = channelManager;
		this.channelPool = this.channelManager.createChannelPool(this);
		
		this._initAuthCheck();
		this._initBreakAndQuitCheck();
		this._initEvent();
	}


	/**
	 * 	初始化相关回调函数
	 * */
	private void _initEvent() {
		
		this.clientHandler.setOnCloseListener(new ClientSocketEvent() {

			@Override
			public void on(ClientHandler handler, byte[] data) { //当发生网络句柄关闭事件
				//回收掉相关的事件
				Timer.CancelTask(timertask_auth);
				
				System.out.println("句柄关闭");
				
			}
			
		});
		
	}
	
	
	private Task timertask_auth;
	
	/**
	 * 	初始化认证检测器
	 * */
	private void _initAuthCheck() {
		
		if(this.level == 0) { //未认证状态下5000秒后将强制退出连接
			timertask_auth = Timer.SetTimeOut(new TimerTask() {

				@Override
				public void Event() {
					if (level == 0) {
						//超时自动断线
						
						System.out.println("超时了");
						clientHandler.close();
					}
				}
				
			}, this.userManager.getNaiveNetServerHandler().config.getConf("USER_AUTH_TIMEOUT").getInt());
		}
		
	}
	
	/**
	 * 	初始化掉线与退出检测器
	 * */
	private void _initBreakAndQuitCheck() {
		
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
		this.clientHandler.close();
		this.userManager.removeUser(this);
	}

	/**
	 * 	请求进入特定的Channel
	 * 	@param msg 回复的用户句柄
	 * */
	public void enterChannel(NaiveNetMessage msg) {
		this.channelPool.enterChannel(msg);
	}

}
