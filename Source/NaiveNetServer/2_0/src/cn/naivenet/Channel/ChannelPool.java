package cn.naivenet.Channel;

import java.util.concurrent.ConcurrentHashMap;

import cn.naivenet.Config.ChannelInfo;
import cn.naivenet.User.CodeMap;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;
import cn.naivenet.User.User;

public class ChannelPool {

	private ChannelManager channelManager;
	private User user;
	

	private ConcurrentHashMap<Integer,ChannelHandler> hashmapIDAndChannel; //已经建立连接的Channel表
	private ConcurrentHashMap<ChannelHandler,Integer> hashmapChannelAndID;	//已经建立连接的Channel表
	
	public ChannelPool(ChannelManager channelManager, User user) {
		this.channelManager = channelManager;
		this.user = user;
		this.initEvent();
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
				
			}
			
		};
		
		onRead = new ChannelSocketEvent() {

			@Override
			public void on(ChannelHandler handler, byte[] data) {
				
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
		ch.setOnUnAuthListener(onAuth);
		
	}

}
