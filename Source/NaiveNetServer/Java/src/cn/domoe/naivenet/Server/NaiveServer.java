package cn.domoe.naivenet.Server;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;

import cn.domoe.naivenet.NaiveNetServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

public class NaiveServer {

	private NaiveNetServerHandler naiveNetServer;
	private ServerBootstrap boot;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
	private int port;
	
	private boolean openssl = false;
	private String SSLKeyStoreFilePath;
	private String SSLPassword;
	
	public NaiveServer(NaiveNetServerHandler naiveNetServer) {
		this.naiveNetServer = naiveNetServer;
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		boot = new ServerBootstrap();
	}
	
	/**
	 * 以普通Socket启动服务器
	 * @throws Exception 
	 * */
	public void launch(int port) throws Exception {
		this.port = port;
		
		this.start();
	}
	
	/**
	 * 以SSL安全协议启动服务器
	 * 提供jks文件绝对路径以及对应密码
	 * @throws Exception 
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
				.option(ChannelOption.SO_BACKLOG, 1024);
			ChannelFuture future = boot.bind(port).sync();
			future.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	
	public EventLoopGroup getWorkerGroup() {
		return this.workerGroup;
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
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			pipeline.addLast(new NewUserConnect(naiveNetServer.userManager));
			
			//出入流量计数器
			pipeline.addLast(new NaiveNetInCounterHandler(naiveNetServer.userManager));
			pipeline.addLast(new NaiveNetOutCounterHandler(naiveNetServer.userManager));
			
			//开启SSL安全模式
			if(openssl == true) {
				SSLEngine sslEngine = sslContext.newEngine(ch.alloc());
				pipeline.addLast(new SslHandler(sslEngine));
			}
			
			//解析连接协议 每个连接协议只解析1次 解析之后该Handler将自动解除 并配置对应的接收器
			pipeline.addLast(new NaiveNetProtocolParse(naiveNetServer.userManager));
			
			
		}
		
	}

}
