package cn.naivenet.ClientSocket;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.net.ssl.KeyManagerFactory;

import cn.naivenet.NaiveNetServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 	客户端连接池
 * 	该池维护用户的链接通信句柄
 * */
public class ClientConnPool {

	private NaiveNetServerHandler naivenetserverhandler;
	
	private ServerBootstrap boot;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
	private int port;
	
	ChannelFuture future;
	
	private boolean openssl = false;
	private String SSLKeyStoreFilePath;
	private String SSLPassword;
	
	private int timeout_break = 10000;
	
	private ConcurrentLinkedDeque<ClientHandler> clientList; //用户连接句柄表
	
	public ClientConnPool(NaiveNetServerHandler naivenetserverhandler) {
		this.naivenetserverhandler = naivenetserverhandler;
		this.boot = new ServerBootstrap();
		this.bossGroup = new NioEventLoopGroup();
		this.workerGroup = new NioEventLoopGroup();
		this.clientList = new ConcurrentLinkedDeque<>();
		
	}

	/**
	 * 	获取断线超时时间
	 * */
	public int getTimeOutBreak() {
		return this.timeout_break;
	}

	/**
	 * 	设置断线超时时间
	 * */
	public void setTimeOutBreak(int value) {
		this.timeout_break = value;
	}

	/**
	 * 	以普通协议启动服务
	 * 	@param port 服务端口号
	 * 	@throws Exception 
	 * 	
	 * */
	public void launch(int port) throws Exception {
		this.port = port;
		this.start();
	}
	
	/**
	 *	 以SSL安全协议启动服务器
	 * 	提供jks文件绝对路径以及对应密码
	 * 	@param prot 端口号
	 * 	@param keyStoreFilePath jks文件绝对路径
	 * 	@param password jks对应文件密码
	 * 	@throws Exception 
	 * */
	public void launch(int port,String keyStoreFilePath,String password) throws Exception {
		this.port = port;
		
		this.openssl = true;
		this.SSLKeyStoreFilePath = keyStoreFilePath;
		this.SSLPassword = password;
		
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
		
		private SslContext sslContext;
		
		public NaiveNetChannelInitializer() throws Exception{
			if(!openssl)
				return;
			
			char[] password = SSLPassword.toCharArray();
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(new FileInputStream(SSLKeyStoreFilePath),password);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore,password);
			sslContext = SslContextBuilder.forServer(kmf).build();
		}

		@Override
		protected void initChannel(SocketChannel arg0) throws Exception {
			//产生新的连接
			ClientHandler client = new ClientHandler(arg0,ClientConnPool.this);
			clientList.add(client);
			ChannelPipeline pipeline = arg0.pipeline();
			
			//出入流量计数器
//			pipeline.addLast(new InCounterHandler());
//			pipeline.addLast(new OutCounterHandler());
			
			//解析协议
			pipeline.addLast(new HttpServerCodec());
			pipeline.addLast(new ChunkedWriteHandler());
			pipeline.addLast(new HttpObjectAggregator(1024 * 8));
			pipeline.addLast(new WebSocketServerProtocolHandler("/"));
			pipeline.addLast(new NettyHandler_WebSocketFrameHandler(client));
			
			
			//初始化任务结束后产生用户句柄的创建回调，在业务处理期间产生的异常，将自动回收连接。
			if(onNewClient != null)
				onNewClient.on(client,null);
			
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
	 * 	关闭服务
	 * @throws Exception 关闭失败时抛出异常
	 * */
	public void shutdown() throws Exception {
		if (future != null)
			future.channel().closeFuture().sync();
	}
	
}
