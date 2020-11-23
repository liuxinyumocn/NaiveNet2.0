package cn.domoe.naivenet.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cn.domoe.naivenet.NaiveNetServerHandler;
import cn.domoe.naivenet.Config.ChannelInfo;
import cn.domoe.naivenet.User.CodeMap;
import cn.domoe.naivenet.User.NaiveNetBox;
import cn.domoe.naivenet.User.NaiveNetController;
import cn.domoe.naivenet.User.NaiveNetMessage;
import cn.domoe.naivenet.User.NaiveNetResponseData;
import cn.domoe.naivenet.User.NaiveNetUserMessage;
import cn.domoe.naivenet.User.User;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Channel频道资源管理池
 * 
 * */
public class NaiveNetChannelManager {

	NaiveNetServerHandler naiveNetServerHandler;
	EventLoopGroup childGroup;
	Bootstrap bootstrap;
	public NaiveNetChannelManager(NaiveNetServerHandler naiveNetServerHandler) {
		this.naiveNetServerHandler = naiveNetServerHandler;
		this.childGroup = naiveNetServerHandler.naiveServer.getWorkerGroup();
		this.bootstrap = new Bootstrap();
		this.hashMapChannelAndUser = new ConcurrentHashMap<>();
		this.hashMapChannelAndNaiveNetMessage = new ConcurrentHashMap<>();
		this.init();
	}
	
	/**
	 *	 初始化连接句柄
	 * */
	public void init() {
		bootstrap.group(childGroup)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new NaiveNetDecoder());
					pipeline.addLast(new NaiveNetEncoder());
					pipeline.addLast(new AuthInboundHandler());
					
				}
				
			});
		//事件注册
		this.register();
	}
	
	/**
	 * 	频道AuthHandler
	 * */
	class AuthInboundHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			byte[] data = (byte[])msg;
			String response = new String(data);
			//System.out.println(response);

			Channel channel = ctx.channel();
			NaiveNetMessage nnm = hashMapChannelAndNaiveNetMessage.get(channel);
			if(nnm == null)
				return;
			hashMapChannelAndNaiveNetMessage.remove(channel);

			if(response.equals("NAIVENETCHANNEL CODE[OK]")) { //成功建立连接
				nnm.user.channelPool.connSuccess(nnm,channel);
				ChannelPipeline pipeline = channel.pipeline();
				pipeline.remove(this);
				pipeline.addLast(new DealInboundHandler());
				pipeline.addLast(new NaiveNetSendToChannelOutboundHandler());
				hashMapChannelAndUser.put(channel,nnm.user);
			}else {
				//连接建立 但是远程主机并没有正确回应
				ctx.close();
				NaiveNetResponseData nrd = new NaiveNetResponseData(nnm,CodeMap.CHANNEL_REFUSE_CONNECT,false);
				nnm.user.responseClient(nrd);
				return;
			}
			

		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			try {
				hashMapChannelAndNaiveNetMessage.remove(ctx.channel());
			}catch(Exception e) {
				
			}
			//cause.printStackTrace();
			ctx.close();
			
		}
	}
	
	/**
	 * 	频道DealHandler
	 * */
	class DealInboundHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			byte[] data = (byte[])msg; //来自NC的请求
			Channel channel = ctx.channel();
			User user = hashMapChannelAndUser.get(channel);
			if(user == null)
				return;
			
			NaiveNetUserMessage nnm = new NaiveNetUserMessage(data,user);
			
			//处理
			if(nnm.channelid == 0) { //对NS的
				if(nnm.control == 1) { //请求类型
					dealNCToNS(nnm,channel);
				}else if(nnm.control == 0) { //回复类型
					
				}
			}else { //对Client
				//对Client的请求直接原样转发给User
				//设定ChannelID
				//nnm.channelid = user.channelPool.getChannelID(channel);
				nnm.setChannelID(user.channelPool.getChannelID(channel));
				//System.out.println(nnm.channelid);
				user.dealNCToC(nnm);
			}

		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			//cause.printStackTrace();
			try {
				hashMapChannelAndUser.remove(ctx.channel());
			}catch(Exception e) {
				
			}
			ctx.close();
			
		}
	}
	
	
	
	//已经建立连接的连接池
	private ConcurrentHashMap<Channel,User> hashMapChannelAndUser;

	
	private ConcurrentHashMap<Channel,NaiveNetMessage> hashMapChannelAndNaiveNetMessage; //未与NC完成授权的句柄
	/**
	 * 请求访问一个频道
	 * @throws InterruptedException 
	 * */
	public void connect(NaiveNetMessage msg, String channelName) throws Exception {
		
		
		
		ChannelInfo info = this.naiveNetServerHandler.config.getChannelInfo(channelName);
		if(info == null) { //不存在的频道信息
			NaiveNetResponseData nrd = new NaiveNetResponseData(msg,CodeMap.NOT_FOUND_CHANNEL,false);
			msg.user.responseClient(nrd);
			return;
		}

		ChannelFuture cf = this.bootstrap.connect(info.getIP(),info.getPort()).sync();//建立连接后立即发送授权秘钥
		String content = "NAIVENETCHANNEL TOKEN["+info.getToken()+"]";
		Channel _channel = cf.channel();
		_channel.writeAndFlush(content.getBytes());
		
		//连接建立 等待Channel回执消息
		this.hashMapChannelAndNaiveNetMessage.put(_channel,msg);
		
	}
	
	private void dealNCToNS(NaiveNetMessage msg, Channel channel) {
		NaiveNetResponseData res = null;
		for(int i = 0;i<this.boxs.size();i++) {
			res = this.boxs.get(i).deal(msg);
			if(res != null) {
				this.responseNC(res,channel);
				return;
			}
		}
		//未发现控制器
		NaiveNetResponseData nrd = new NaiveNetResponseData(msg,CodeMap.NOT_FOUNTD_CONTROLLER,false);
		this.responseNC(nrd,channel);
	}

	/**
	 * 	对频道发来的请求进行回复
	 * */
	private void responseNC(NaiveNetResponseData res, Channel channel) {
		if(res.getCancel())
			return;
		channel.writeAndFlush(res.genData());
	}

	private List<NaiveNetBox> boxs = new ArrayList<>();
	public void addBox(NaiveNetBox mod) {
		boxs.add(mod);
	}
	public void removeBox(NaiveNetBox mod) {
		boxs.remove(mod);
	}
	private void register() {
		NaiveNetBox box = new NaiveNetBox();
		box.addController(new Ctrl1());
		box.addController(new Ctrl2());
		box.addController(new Ctrl3());
		box.addController(new Ctrl4());
		box.addController(new Ctrl5());
		box.addController(new Ctrl6());
		box.addController(new Ctrl7());
		box.addController(new Ctrl8());
		box.addController(new Ctrl9());
		box.addController(new Ctrl10());
		this.addBox(box);
	}
	

	//NC为Client设置授权状态
	class Ctrl1 extends NaiveNetController{

		public Ctrl1() {
			super("auth");
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			//System.out.println("--------------------------");
			//System.out.println("开始授权");
			
			msg.user.auth();
			return null;
		}
		
	}

	//NC获取基本连接信息
	class Ctrl2 extends NaiveNetController{

		public Ctrl2() {
			super("linkinfo");
			// TODO Auto-generated constructor stub
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	static class ValueInfo{
		public int end = 0;
		public byte[] value = null;
	}
	private static ValueInfo parseValue(int start,byte[] data) {
		//Log.print(data);
		ValueInfo res = new ValueInfo();
		//先检测长度
		int length = 0;
		int j = 1;
		for(;start<data.length;start++) {
			length += (int)data[start];
			j++;
			if(data[start] != 255) {
				start++;
				break;
			}
		}
		res.value = new byte[length];
		for(int i = 0;i<res.value.length;i++) {
			res.value[i] = data[start++];
		}
		res.end = start;
		return res;
	}
	
	//设置SESSION
	class Ctrl3 extends NaiveNetController{

		public Ctrl3() {
			super("setsession");
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			ValueInfo key = NaiveNetChannelManager.parseValue(0, msg.param);
			byte[] keyBytes = key.value;
			ValueInfo value = NaiveNetChannelManager.parseValue(key.end, msg.param);
			byte[] valueBytes = value.value;
			msg.user.setSession(new String(keyBytes),valueBytes);
			return null;
		}
		
	}class Ctrl4 extends NaiveNetController{

		public Ctrl4() {
			super("getsession");
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			
			byte[] value = msg.user.getSession(new String(msg.param));
			if(value == null) {
				value = "".getBytes();
			}
			NaiveNetResponseData res = new NaiveNetResponseData(msg,value,true);
			return res;
		}
		
	}
	class Ctrl5 extends NaiveNetController{

		public Ctrl5() {
			super("clearsession");
			// TODO Auto-generated constructor stub
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			msg.user.clearSession();
			return null;
		}
		
	}

	//关闭连接
	class Ctrl6 extends NaiveNetController{

		public Ctrl6() {
			super("close");
			// TODO Auto-generated constructor stub
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	

	//关闭连接
	class Ctrl7 extends NaiveNetController{

		public Ctrl7() {
			super("ping");
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			NaiveNetResponseData res = new NaiveNetResponseData(msg,msg.user.getPing().getBytes(),true);
			return res;
		}
		
	}

	class Ctrl8 extends NaiveNetController{

		public Ctrl8() {
			super("getlinkinfo");
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			String json = msg.user.getLinkInfo();
			NaiveNetResponseData res = new NaiveNetResponseData(msg,json.getBytes(),true);
			return res;
		}
		
	}

	class Ctrl9 extends NaiveNetController{

		public Ctrl9() {
			super("quitchannel");
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			Integer channelID = msg.channelid;
			msg.user.channelPool.quitChannel(channelID);
			return null;
		}
		
	}
	
	class Ctrl10 extends NaiveNetController{

		public Ctrl10() {
			super("close");
		}

		@Override
		public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
			msg.user.onQuit();
			return null;
		}
		
	}
	
	public void closeChannel(Channel channel) {
		this.hashMapChannelAndUser.remove(channel);
		
	}
}
