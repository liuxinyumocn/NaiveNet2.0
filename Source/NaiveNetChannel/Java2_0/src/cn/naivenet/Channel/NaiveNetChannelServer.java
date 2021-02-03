package cn.naivenet.Channel;

import java.util.concurrent.ConcurrentLinkedDeque;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NaiveNetChannelServer {

	private ServerBootstrap boot;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
	private int port;
	private byte[] token;
	
	ChannelFuture future;
	
	private ConcurrentLinkedDeque<ClientHandler> clientList;
	
	public NaiveNetChannelServer(int port, String token) {
		this.port = port;
		this.token = ("NAIVENETCHANNEL TOKEN["+token+"]").getBytes();;
		this.boot = new ServerBootstrap();
		this.bossGroup = new NioEventLoopGroup();
		this.workerGroup = new NioEventLoopGroup();
		this.clientList = new ConcurrentLinkedDeque<>();
	}
	
	/**
	 * 	以启动服务
	 * 	@param port 服务端口号
	 * 	@throws Exception 
	 * 	
	 * */
	public void launch() throws Exception {
		this.start();
	}
	
	private void start() throws Exception {
		try {
			boot.group(bossGroup,workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new NaiveNetChannelInitializer())
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childOption(ChannelOption.SO_KEEPALIVE, true);
			future = boot.bind(port).sync();
			future.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	
	class NaiveNetChannelInitializer extends ChannelInitializer<SocketChannel>{

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ClientHandler client = new ClientHandler(ch,NaiveNetChannelServer.this);
			clientList.add(client);
			
			ChannelPipeline pip = ch.pipeline();
			pip.addLast(new NaiveNetDecoder());
			pip.addLast(new NaiveNetEncoder());
			pip.addLast(new NettyHandler_Auth(client,NaiveNetChannelServer.this));
			
		}
		
	}
	
	private ClientSocketEvent onNewClient; //当发生新的用户句柄连接时的事件回调接口
	/**
	 * 	设置当有新的客户端句柄产生时的事件回调接口
	 * 	@param e ClientSocketEvent 接口的实现
	 * */
	public void setOnNewClientEvent(ClientSocketEvent e) {
		this.onNewClient = e;
	}

	/**
	 * 	有句柄通信发生关闭，对其进行回收操作
	 * */
	public void _onClientClose(ClientHandler c) {
		//发生异常，连接已经关闭
		boolean res = this.clientList.remove(c);
	}
	
	/**
	 * 	获取TOKEN字节集
	 * */
	public byte[] getToken() {
		return token;
	}

	public void _onNewUser(ClientHandler client) {
		if(onNewClient != null) {
			onNewClient.on(client,null);
		}
	}

	/**
	 * 	关闭服务
	 * 	@throws Exception 关闭失败时抛出异常
	 * */
	public void shutdown() throws Exception {
		if (future != null)
			future.channel().closeFuture().sync();
	}
	
}
