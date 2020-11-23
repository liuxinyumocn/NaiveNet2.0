package cn.domoe.naivenet.Channel;

import java.io.IOException;
import java.net.InetSocketAddress;
//import java.nio.ByteBuffer;
//import java.nio.channels.SelectionKey;
//import java.nio.channels.Selector;
//import java.nio.channels.ServerSocketChannel;
//import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

//import cn.domoe.naivenet.Server.NaiveServer.NaiveNetChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Channel
 * */
class NaiveNetChannelServer {

	private int port;
	private UserManager userManger;
	
	//private Selector selector;
	//private ServerSocketChannel server;
	private ServerBootstrap boot;
	private EventLoopGroup group;
	
	private ConcurrentHashMap<Channel,User> hashmapSocketChannelAndUser;
	
	public NaiveNetChannelServer(int port, UserManager userManager) {
		
		this.port = port;
		this.userManger = userManager;
		this.hashmapSocketChannelAndUser = new ConcurrentHashMap<>();
		this.userManger.naiveNetChannelServer = this;
		
		boot = new ServerBootstrap();
		group = new NioEventLoopGroup(1);
		
	}
	
	public void launch(int Max_Thread) throws Exception {
		
		if(Max_Thread < 0) {
			Max_Thread = Runtime.getRuntime().availableProcessors() * 4;
		}
		NaiveNetThreadManager.Init(Max_Thread);
		NaiveNetThreadPool.getPool().submit(new LaunchServerThread());
		try {
			boot.group(group)
				.channel(NioServerSocketChannel.class)
				.childHandler(new NaiveNetChannelInitializer())
				.option(ChannelOption.SO_BACKLOG, 1024);
			ChannelFuture future = boot.bind(port).sync();
			future.channel().closeFuture().sync();
		}finally {
			group.shutdownGracefully();
		}
	}
	
	class LaunchServerThread implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	class NaiveNetChannelInitializer extends ChannelInitializer<SocketChannel>{

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			pipeline.addLast(new NewUserConnect());
			pipeline.addLast(new NaiveNetGetMessage());
		}
		
	}
	
	class NaiveNetGetMessage extends ChannelInboundHandlerAdapter{

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			ByteBuf buf = (ByteBuf)msg;
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
	    	User user = hashmapSocketChannelAndUser.get(ctx.channel());
	    	userManger.onRead(user,data);
			
		}
		
		
	}
	
	class NewUserConnect extends ChannelInboundHandlerAdapter{
		
		@Override
	    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			//新的连接建立
			Channel channel = ctx.channel();
			ChannelPipeline pipeline = ctx.pipeline();
			pipeline.remove(this);
			//创建新用户
			User user = userManger.createUser(channel);
			hashmapSocketChannelAndUser.put(channel,user);
			
		}

	    @Override
	    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
	    	//连接发生断开
	    	User user = hashmapSocketChannelAndUser.get(ctx.channel());
	    	if(user != null)
	    		user.onQuit();
	    }

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	    	//User user = this.userManager.FindUser(ctx.channel());
	    	User user = hashmapSocketChannelAndUser.get(ctx.channel());
	    	if(user != null)
	    		user.onQuit();
	        ctx.close();
	    }
		
	}

	//private boolean blocking = false;
//	class AcceptThread implements Runnable {
//
//		@Override
//		public void run() {
//			while(true) {
//				
//				int num;
//				try {
//					blocking = true;
//					//System.out.println("等待");
//					num = selector.select();
//					blocking = false;
//				}catch(Exception e) {
//					//System.out.println("ServerChannel Accept Error,Shutdown Service");
//					shutdown();
//					break;
//				}
//				if(num <= 0) {
//					continue;
//				}
//				SelectionKey[] removelist = new SelectionKey[selector.selectedKeys().size()];
//				int removelist_index = 0;
//				for(SelectionKey sk : selector.selectedKeys()) {
//					removelist[removelist_index] = sk;
//					//selector.selectedKeys().remove(sk);
//					
//					if(sk.isAcceptable()) {
//						SocketChannel sc;
//						try {
//							sc = server.accept();
//						}catch (IOException e) {
//							continue;
//						}
//						try {
//							sc.configureBlocking(false);
//							sc.register(selector, SelectionKey.OP_READ);
//							
//							//创建新用户
//							User user = userManger.createUser(sc);
//							hashmapSocketChannelAndUser.put(sc,user);
//							
//						}catch(Exception e) {
//							
//						}
//						sk.interestOps(SelectionKey.OP_ACCEPT);
//					}else if(sk.isReadable()) {
//						SocketChannel sc = (SocketChannel)sk.channel();
//						User user = hashmapSocketChannelAndUser.get(sc);
//						if(user == null)
//							continue;
//						ByteBuffer buff = ByteBuffer.allocate(1024);
//						//byte[] data = new byte[1024];
//						//int index = 0;
//						try {
//							
//							int len = sc.read(buff);
//							if(len == -1) {
//								//链接发生断开
//								user.onQuit();
//							}else {
//								//完整读取整个Buff
//								byte[] b = buff.array();
////								if(buff.position() + index > data.length) { //延长缓冲区
////									byte[] data2 = new byte[data.length + 1024];
////									System.arraycopy(data, 0, data2, 0, data.length);
////									data = data2;
////								}
////								
//								byte[] data = new byte[buff.position()];
//								for(int i = 0;i< data.length;i++) {
//									data[i] = b[i];
//								}
//								if(data.length > 0) {
//									userManger.onRead(user,b);
//									System.out.println(data.length);
//									sk.interestOps(SelectionKey.OP_READ);
//								}
//							}
//							
//							
//							//int len = sc.read(buff);
////							if(len == -1) {
////								user.onQuit();
////							}
//							//System.out.println(len);
////							boolean close = false;
////							while(true) {
////								int len = sc.read(buff);
////								System.out.println("aaa"+len);
////								if(len == -1)
////								{
////									System.out.println(sc.hashCode());
////									close= true;
////									break;
////								}else if(len == 0){
////									break;
////								}
////								byte[] b = buff.array();
////								if(buff.position() + index > data.length) { //延长缓冲区
////									byte[] data2 = new byte[data.length + 1024];
////									System.arraycopy(data, 0, data2, 0, data.length);
////									data = data2;
////								}
////								
////								for(int i = 0;i< buff.position();i++) {
////									data[index++] = b[i];
////								}
////								buff.clear();
////							}
//							//数据读取完毕
////							if(close) {
////								//说明与NS网络发生断开
////								user.onQuit();
////							}else{
////								if(index > 0) {
////									byte[] _data = new byte[index];
////									System.arraycopy(data, 0, _data, 0, _data.length);
////									try {
////										userManger.onRead(user,_data);
////									} catch (Exception e) {
////										
////									}
////									sk.interestOps(SelectionKey.OP_READ);
////								}
////							}
//						}catch(Exception e) {
//							//System.out.println("Exc");
//							e.printStackTrace();
//							sk.cancel();
//							if(sk.channel() != null) {
//								user.onQuit();
//							}
//						}
//					}
//					
//				}
//				
//
//				for(int i = 0;i<removelist.length;i++) {
//					selector.selectedKeys().remove(removelist[i]);
//				}
//				
//			}
//			
//		}
//		
//	}
	
	public void shutdown() {

		
	}

	/**
	 * 	关闭掉User句柄
	 * */
	public void close(User user) {
		Channel c = user.getSocketChannel();
		try{
			this.hashmapSocketChannelAndUser.remove(c);
		}catch(Exception e) {
			
		}
		c.close();
		
	}
}
