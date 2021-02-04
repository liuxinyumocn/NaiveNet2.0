package cn.naivenet.ClientSocket;

import java.util.concurrent.ScheduledFuture;

import cn.naivenet.TimerEvent.Timer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

/**
 * 	用户连接句柄
 * 	使用该句柄可以与客户端直接产生通信IO操作
 * */
public class ClientHandler {

	private SocketChannel channel;
	private ClientConnPool pool;
	
	public ClientHandler(SocketChannel channel,ClientConnPool pool) {
		this.channel = channel;
		this.pool = pool;
		
		this.resetTimeOutCheck();
	}

	private ClientSocketEvent onRead;				//当该通信句柄有数据抵达时
	private ClientSocketEvent onClose;				//当该通信关闭时
	private ClientSocketEvent onExceptionCaught;	//当该通信发生异常时

	/**
	 * 	当该通信句柄有数据抵达时发起回调
	 * 	@param e 处理事件的接口
	 * */
	public void setOnReadListener(ClientSocketEvent e) {
		this.onRead = e;
	}
	
	/**
	 * 	当该通信句柄关闭通信通道时触发
	 * 	@param e 处理事件的接口
	 * */
	public void setOnCloseListener(ClientSocketEvent e) {
		this.onClose = e;
	}
	
	/**
	 * 	当该通信句柄发生异常时触发
	 * 	@param e 处理事件的接口
	 * */
	public void setOnExceptionCaughtListener(ClientSocketEvent e) {
		this.onExceptionCaught = e;
	}
	

	/**
	 * 	新数据抵达
	 * */
	public void _onRead(byte[] data) {
		this.resetTimeOutCheck();
		if(onRead != null)
			this.onRead.on(this,data);
	}

	/**
	 * 	通信发生关闭
	 * */
	public void _onClose() {
		if(timeoutTimertask != null) {
			Timer.CancelTimeout(timeoutTimertask);
		}
		this.pool._onClientClose(this);
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
	 * 	向客户端发送数据包
	 * */
	public void send(byte[] data) {
		
		BinaryWebSocketFrame f= new BinaryWebSocketFrame();
		f.content().writeBytes(data);
		this.channel.writeAndFlush(f);
		
	}
	
	/**
	 * 	关闭网络通信通道
	 * 	立即关闭通信通道，这可能导致正在发送的数据未来得及抵达客户端通信就被中断
	 * 	如果需要发送离别消息，使用 sendAndClose() 函数
	 * */
	public void close() {
		if(this.channel != null)
			this.channel.close();
	}
	
	/**
	 * 	发送离别消息后再中断网络
	 * 	@param data 消息正文
	 * */
	public void sendAndClose(byte[] data) {
		BinaryWebSocketFrame f= new BinaryWebSocketFrame();
		f.content().writeBytes(data);
		ChannelFuture future = this.channel.writeAndFlush(f);
		future.addListener(ChannelFutureListener.CLOSE);
	}

	
	private ScheduledFuture timeoutTimertask;
	
	/**
	 * 	超时检测器
	 * 	当超过特定时间客户端与服务器没有产生通信，则判定客户端发生离线
	 * */
	private void resetTimeOutCheck() {
		
		if(timeoutTimertask != null) {
			Timer.CancelTimeout(timeoutTimertask);
		}
		
		timeoutTimertask = Timer.SetTimeout(new Runnable() {

			@Override
			public void run() {
				//数据超时
				timeoutTimertask = null;
				close();
			}
			
		}, this.pool.getTimeOutBreak());
		
	}

	/**
	 * 	获取原始通信句柄
	 * */
	public SocketChannel getChannel() {
		return this.channel;
	}
}
