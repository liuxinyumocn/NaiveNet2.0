package cn.domoe.naivenet.Channel;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.domoe.naivenet.Config.ChannelInfo;
import cn.domoe.naivenet.User.CodeMap;
import cn.domoe.naivenet.User.NaiveNetMessage;
import cn.domoe.naivenet.User.NaiveNetRequestData;
import cn.domoe.naivenet.User.NaiveNetResponseData;
import cn.domoe.naivenet.User.NaiveNetUserMessage;
import cn.domoe.naivenet.User.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;

public class NaiveNetChannelPool {

	private User user;
	private NaiveNetChannelManager channelManager;
	
	public NaiveNetChannelPool(User user, NaiveNetChannelManager channelManager) {
		this.user = user;
		this.channelManager = channelManager;
		this.hashmapIDAndChannel = new ConcurrentHashMap<>();
	}

	private ConcurrentHashMap<Integer,Channel> hashmapIDAndChannel; //已经建立连接的Channel表
	
	/**
	 * 申请加入一个Channel
	 * */
	public void enterChannel(NaiveNetMessage msg) {
		
		String channelName = new String(msg.param);
		ChannelInfo channelInfo = this.channelManager.naiveNetServerHandler.config.getChannelInfo(channelName);
		if(channelInfo.getAuth() && !this.user.isLog()) {	//权限不足
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.PERMISSION_DENIED,false);
			this.user.responseClient(res);
			return;
		}
		Integer id = this.channelManager.naiveNetServerHandler.config.getChannelInfo(channelName).id;
		Channel _channel = hashmapIDAndChannel.get(id);
		if(_channel != null) { //已经建立连接
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.OK,true);
			this.user.responseClient(res);
			return;
		}
		
		//尝试与该频道建立连接
		try {
			this.channelManager.connect(msg,channelName);
		} catch (Exception e) {
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.CANNOT_BE_ESTABLISHED,false);
			this.user.responseClient(res);
			return;
		}
		
		
	}

	//连接成功后的回调，num 用于反馈的消息句柄 channel 产生的网络连接句柄
	public void connSuccess(NaiveNetMessage nnm, Channel channel) {
		String channelName = new String(nnm.param);
		Integer channelid = channelManager.naiveNetServerHandler.config.getChannelInfo(channelName).id;
		hashmapIDAndChannel.put(channelid, channel);
		NaiveNetResponseData res = new NaiveNetResponseData(nnm,CodeMap.OK,true);
		this.user.responseClient(res);
	}


	/**
	 * 	当用户发生Break时通知所有已经连接的Channel用户发生了Break
	 * */
	public void onUserBreak() {
		NaiveNetRequestData data = new NaiveNetRequestData(1,0,0,"onbreak",new byte[0]);
		byte[] _data = data.genData();
		Iterator it2 = this.hashmapIDAndChannel.entrySet().iterator();
		while(it2.hasNext()) {
			Map.Entry entry = (Map.Entry)it2.next();
			Channel channel = (Channel)entry.getValue();
			channel.writeAndFlush(_data);
		}
	}
	
	/**
	 * 	用户发生了网络恢复，通知给所有已经建立连接的NC
	 * */
	public void onUserRecover() {
		NaiveNetRequestData data = new NaiveNetRequestData(1,0,0,"onrecover",new byte[0]);
		byte[] _data = data.genData();
		Iterator it2 = this.hashmapIDAndChannel.entrySet().iterator();
		while(it2.hasNext()) {
			Map.Entry entry = (Map.Entry)it2.next();
			Channel channel = (Channel)entry.getValue();
			channel.writeAndFlush(_data);
		}
	}

	/**
	 * 	当用户发生Quit时通知所有已经连接的Channel用户发生了Quit
	 * */
	public void onUserQuit() {
//		NaiveNetRequestData data = new NaiveNetRequestData(1,0,0,"onquit",new byte[0]);
//		byte[] _data = data.genData();
		Iterator it2 = this.hashmapIDAndChannel.entrySet().iterator();
		while(it2.hasNext()) {
			Map.Entry entry = (Map.Entry)it2.next();
			Integer id = (Integer)entry.getKey();
			this.quitChannel(id);
		}
		
	}
	

	/**
	 * 	用户申请退出特定频道
	 * */
	public void quitChannel(Integer channelID) {
//		NaiveNetRequestData data = new NaiveNetRequestData(1,0,0,"onquit",new byte[0]);
//		byte[] _data = data.genData();
		Channel channel = this.hashmapIDAndChannel.get(channelID);
		if(channel == null)
			return;
		this.hashmapIDAndChannel.remove(channelID);
		try {
			this.channelManager.closeChannel(channel);
			channel.close().sync();
		} catch (InterruptedException e) {
			
		}
//		if(channel != null) {
//			ChannelFuture future = channel.writeAndFlush(_data);
//		}
	}

	/**
	 * 	客户端向NC发起请求
	 * */
	public void dealCToNC(NaiveNetMessage msg) {
		Channel channel = this.hashmapIDAndChannel.get(msg.channelid);
		if(channel == null) {
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.CHANNEL_NOT_ESTABLISHED_WITH_SERVER,false);
			msg.user.responseClient(res);
			return;
		}
		channel.writeAndFlush(msg.data);		
	}
	
	/**
	 * 	客户端对NC的请求进行回复
	 * */
	public void responseClientToNC(NaiveNetMessage msg) {
		System.out.println("回应NC"+msg.channelid);
		Channel channel = this.hashmapIDAndChannel.get(msg.channelid);
		if(channel == null) { //说明与NC已经断开连接无需回复
			return;
		}
		//Log.print(msg.data);
		channel.writeAndFlush(msg.data);
		
	}


}
