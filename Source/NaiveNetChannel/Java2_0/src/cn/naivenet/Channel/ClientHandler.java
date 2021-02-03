package cn.naivenet.Channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;

public class ClientHandler {

	private SocketChannel socketChannel;
	private NaiveNetChannelServer naiveNetChannelServer;
	
	public ClientHandler(SocketChannel ch, NaiveNetChannelServer naiveNetChannelServer) {
		this.socketChannel = ch;
		this.naiveNetChannelServer = naiveNetChannelServer;
		
	}

	private ClientSocketEvent onRead;				//当该通信句柄有数据抵达时
	private ClientSocketEvent onClose;				//当该通信关闭时

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
	 * 	新数据抵达
	 * */
	public void _onRead(byte[] data) {
		if(onRead != null)
			this.onRead.on(this,data);
	}
	
	/**
	 * 	通信发生关闭
	 * */
	public void _onClose() {
		this.naiveNetChannelServer._onClientClose(this);
		if(this.onClose != null)
			this.onClose.on(this, null);
	}
	
	/**
	 * 	向外发送数据
	 * */
	public void send(byte[] data) {
		this.socketChannel.writeAndFlush(data);
	}
	
	/**
	 * 	关闭网络通信通道
	 * 	立即关闭通信通道，这可能导致正在发送的数据未来得及抵达客户端通信就被中断
	 * 	如果需要发送离别消息，使用 sendAndClose() 函数
	 * */
	public void close() {
		if(this.socketChannel != null)
			this.socketChannel.close();
	}
	
	/**
	 * 	发送离别消息后再中断网络
	 * 	@param data 消息正文
	 * */
	public void sendAndClose(byte[] data) {
		ChannelFuture f = this.socketChannel.writeAndFlush(data);
		f.addListener(ChannelFutureListener.CLOSE);
	}
	
}
