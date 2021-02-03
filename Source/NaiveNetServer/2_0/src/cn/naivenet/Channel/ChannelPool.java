package cn.naivenet.Channel;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.naivenet.Config.ChannelInfo;
import cn.naivenet.User.CodeMap;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetRequestData;
import cn.naivenet.User.NaiveNetResponseData;
import cn.naivenet.User.NaiveNetUserMessage;
import cn.naivenet.User.User;

public class ChannelPool {

	private ChannelManager channelManager;
	private User user;
	

	private ConcurrentHashMap<Integer,ChannelHandler> hashmapIDAndChannel; //已经建立连接的Channel表
	private ConcurrentHashMap<ChannelHandler,Integer> hashmapChannelAndID;	//已经建立连接的Channel表
	
	public ChannelPool(ChannelManager channelManager, User user) {
		this.channelManager = channelManager;
		this.user = user;
		this.hashmapChannelAndID = new ConcurrentHashMap<>();
		this.hashmapIDAndChannel = new ConcurrentHashMap<>();
		this.initEvent();
	}
	
	/**
	 *	申请退出一个Channel
	 *	@param channelID 需要退出的频道ID 
	 **/
	public void quitChannel(Integer channelID) {
		ChannelHandler ch = this.hashmapIDAndChannel.get(channelID);
		if(ch != null) {
			try {
				ch.close();
			} catch (Exception e) {
				
			}
		}
		
	}

	/**
	 * 	申请连接一个Channel
	 * 	@param 消息原文
	 * */
	public void enterChannel(NaiveNetMessage msg) {
		String channelName = new String(msg.param);
		ChannelInfo channelInfo = this.channelManager.naiveNetServerHandler.config.getChannelInfo(channelName);
		if(channelInfo.getAuth() && !this.user.isAuth()) {	//权限不足 权限控制模块
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.PERMISSION_DENIED,false);
			this.user.responseClient(res);
			return;
		}
		Integer id = this.channelManager.naiveNetServerHandler.config.getChannelInfo(channelName).id;
		ChannelHandler _channel = this.hashmapIDAndChannel.get(id);
		if(_channel != null) {
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.OK,true);
			this.user.responseClient(res);
			return;
		}

		try {
			ChannelHandler ch = this.channelManager.connect(msg,channelName);
			if(ch == null)
				return;
			//注册相关的回调事件
			//System.out.println("添加");
			this.hashmapChannelAndID.put(ch, id);
			this.hashmapIDAndChannel.put(id, ch);
			this.registerEvent(ch);

		}catch(Exception e) {
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.CANNOT_BE_ESTABLISHED,false);
			this.user.responseClient(res);
			return;
		}
	}

	/**
	 * 	初始化Handler需要的事件对象
	 * */
	private void initEvent() {
		onClose = new ChannelSocketEvent() {

			@Override
			public void on(ChannelHandler handler, byte[] data) {
				//频道连接断开
				Integer id = hashmapChannelAndID.get(handler);
				//System.out.println("移除");
				hashmapChannelAndID.remove(handler);
				hashmapIDAndChannel.remove(id);
				
				//通知客户端频道已经断开连接
				
			}
			
		};

		onUnAuth = new ChannelSocketEvent() {

			@Override
			public void on(ChannelHandler handler, byte[] data) {
				//没有授权成功
				//通知客户端没有授权成功
				NaiveNetMessage nnm = handler.getMessageHandler();
				NaiveNetResponseData nrd = new NaiveNetResponseData(nnm,CodeMap.CHANNEL_REFUSE_CONNECT,false);
				nnm.user.responseClient(nrd);
				
			}
			
		};
		
		onAuth = new ChannelSocketEvent() {

			@Override
			public void on(ChannelHandler handler, byte[] data) {
				//授权成功
				NaiveNetMessage msg = handler.getMessageHandler();
				NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.OK,true);
				msg.user.responseClient(res);
			}
			
		};
		
		onRead = new ChannelSocketEvent() {

			@Override
			public void on(ChannelHandler handler, byte[] data) {
				
				NaiveNetUserMessage msg = new NaiveNetUserMessage(data,user);
				if(msg.channelid == 0) { //对NS的
					if(msg.control == 1) { //请求类型
						channelManager.dealNCToNS(msg,handler);
					}else if(msg.control == 0) { //回复类型
						
					}
				}else {
					msg.setChannelID(getChannelID(handler));
					user.dealNCToC(msg);
				}
				
			}
			
		};
	}
	
	private ChannelSocketEvent onClose;		//当发生与频道断开连接的事件
	private ChannelSocketEvent onUnAuth;	//当发生没有正确完成授权时
	private ChannelSocketEvent onAuth;		//当发生正确完成授权时
	private ChannelSocketEvent onRead;		//当有来自Channel的新消息抵达时
	
	/**
	 * 	为新添加的CH注册事件
	 * */
	private void registerEvent(ChannelHandler ch) {
		
		ch.setOnCloseListener(onClose);
		ch.setOnReadListener(onRead);
		ch.setOnUnAuthListener(onUnAuth);
		ch.setOnAuthListener(onAuth);
		
	}

	/**
	 * 	处理客户端发往NC的数据请求
	 * */
	public void dealClientToNC(NaiveNetUserMessage msg) {
		ChannelHandler ch = this.hashmapIDAndChannel.get(msg.channelid);
		//System.out.println(this.hashmapIDAndChannel.size());
		if(ch == null) {
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.CHANNEL_NOT_ESTABLISHED_WITH_SERVER,false);
			msg.user.responseClient(res);
			return;
		}
		//System.out.println(this.hashmapIDAndChannel.size());
		ch.send(msg.data);
		
	}

	/**
	 * 	当用户发生断线恢复时对所有已经连接的NC发起通知
	 * */
	public void onUserRecover() {
		NaiveNetRequestData data = new NaiveNetRequestData(1,0,0,"onrecover",new byte[0]);
		this.notifyChannels(data.genData());
	}
	
	/**
	 * 	广播给所有已经建立连接的NC消息
	 * */
	private void notifyChannels(byte[] data) {
		Iterator it = this.hashmapIDAndChannel.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry e = (Map.Entry)it.next();
			ChannelHandler ch = (ChannelHandler)e.getValue();
			ch.send(data);
		}
	}

	/**
	 * 	当用户发生网络断线时的通知
	 * */
	public void onUserBreak() {
		NaiveNetRequestData data = new NaiveNetRequestData(1,0,0,"onbreak",new byte[0]);
		this.notifyChannels(data.genData());
	}

	/**
	 * 	提供Channel句柄给出ChannelID
	 * */
	public int getChannelID(ChannelHandler channel) {
		return this.hashmapChannelAndID.get(channel);
	}

	/**
	 * 	退出所有的Channel
	 * */
	public void quitAll() {
		Iterator it = this.hashmapIDAndChannel.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry e = (Map.Entry)it.next();
			ChannelHandler ch = (ChannelHandler)e.getValue();
			try {
				ch.close();
			} catch (Exception e1) {
			}
		}
	}
}
