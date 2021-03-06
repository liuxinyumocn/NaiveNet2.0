package cn.naivenet.Channel;

import java.util.concurrent.ScheduledFuture;

import cn.naivenet.TimerEvent.Timer;
import cn.naivenet.User.CodeMap;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

/**
 * 	用户与频道的连接句柄
 * 	使用该句柄可以与Channel直接产生通信IO操作
 * */
public class ChannelHandler {

	private Channel channel;
	private ChannelPool pool;
	private byte level = 0;	//0 未认证 1 认证
	private NaiveNetMessage msg;
	
	public ChannelHandler(Channel channel) {
		this.channel = channel;
		this.initCheck();
	}

	/**
	 * 	获取最初用于反馈客户端的消息句柄
	 * */
	public NaiveNetMessage getMessageHandler() {
		return msg;
	}
	
	
	private ScheduledFuture authtimer;
	/**
	 * 	初始化身份验证模块
	 * 	在与Channel完成通信后的3000ms内，Channel端必须给出正确的回应
	 * */
	private void initCheck() {
		authtimer = Timer.SetTimeout(new Runnable() {

			@Override
			public void run() {
				//如果到达3s后未完成验证，则该链接为失效连接
				if(level == 0) { //无效的连接
					onUnAuth.on(ChannelHandler.this, null);
					try {
						ChannelHandler.this.close();
					} catch (Exception e) {
						
					}
				}
			}
			
		}, 3000);
		
	}

	private ChannelSocketEvent onRead;
	private ChannelSocketEvent onClose;
	private ChannelSocketEvent onExceptionCaught;
	private ChannelSocketEvent onUnAuth;
	private ChannelSocketEvent onAuth;
	
	/**
	 * 	当该通信句柄有数据抵达时发起回调
	 * 	@param e 处理事件的接口
	 * */
	public void setOnReadListener(ChannelSocketEvent e) {
		this.onRead = e;
	}
	
	/**
	 * 	当该通信句柄关闭通信通道时触发
	 * 	@param e 处理事件的接口
	 * */
	public void setOnCloseListener(ChannelSocketEvent e) {
		this.onClose = e;
	}
	
	/**
	 * 	当该通信句柄发生异常时触发
	 * 	@param e 处理事件的接口
	 * */
	public void setOnExceptionCaughtListener(ChannelSocketEvent e) {
		this.onExceptionCaught = e;
	}
	
	/**
	 * 	当该通信句柄授权失败时
	 * 	@param e 处理事件的接口
	 * */
	public void setOnUnAuthListener(ChannelSocketEvent e) {
		this.onUnAuth = e;
	}
	
	/**
	 * 	当该通信句柄授权成功时
	 * 	@param e 处理事件的接口
	 * */
	public void setOnAuthListener(ChannelSocketEvent e) {
		this.onAuth = e;
	}
	
	/**
	 * 	新数据抵达
	 * */
	public void _onRead(byte[] data) {
		if(onRead != null)
			this.onRead.on(this, data);
	}
	
	/**
	 * 	通信发生关闭
	 * */
	public void _onClose() {
		if(level == 0) {
			//同时通知授权失败
			if(this.onUnAuth != null)
				this.onUnAuth.on(this, null);
		}
		if(this.onClose != null)
			this.onClose.on(this, null);
	}
	
	/**
	 * 	通信发生异常
	 * */
	public void _onExceptionCaught(Throwable cause) {
		//发生异常，连接已经关闭
		if(this.onExceptionCaught != null)
			this.onExceptionCaught.on(this, null);
	}
	
	/**
	 * 	关闭该通道
	 * @throws Exception 
	 * */
	public void close() throws Exception {
		this.channel.close().sync();
	}

	public Channel getChannel() {
		return this.channel;
	}

	public void _onAuthChannel() {
		if(this.onAuth != null)
			this.onAuth.on(this, null);
		this.level = 1;
		if(authtimer != null)
			Timer.CancelTimeout(authtimer);
		//回应客户端
		NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.OK,true);
		msg.user.responseClient(res);
	}

	/**
	 * 	向Channel发送数据
	 * */
	public void send(byte[] data) {
		this.channel.writeAndFlush(data);
	}

	public void setMsg(NaiveNetMessage msg2) {
		this.msg = msg2;
	}
	
}
