package cn.naivenet.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import cn.naivenet.NaiveNetServerHandler;
import cn.naivenet.Config.ChannelInfo;
import cn.naivenet.User.CodeMap;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;
import cn.naivenet.User.User;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 
 * */
public class ChannelManager {

	public NaiveNetServerHandler naiveNetServerHandler;
	
	EventLoopGroup group;
	Bootstrap boot;
	
	private ConcurrentLinkedDeque<ChannelHandler> channelList;
	private ConcurrentHashMap<Channel,ChannelHandler> hashmap;
	
	public ChannelManager(NaiveNetServerHandler naiveNetServerHandler) {
		this.naiveNetServerHandler = naiveNetServerHandler;
		group = new NioEventLoopGroup();
		boot = new Bootstrap();
		channelList = new ConcurrentLinkedDeque<>();
		
		this.init();
	}

	/**
	 * 	初始化Netty
	 * */
	private void init() {
		boot.group(group)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					
					Channel c = ch;
					ChannelHandler chd = hashmap.get(c);
					hashmap.remove(c);
					
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new NaiveNetDecoder());
					pipeline.addLast(new NaiveNetEncoder());
					pipeline.addLast(new NettyHandler_ChannelAuth(chd,ChannelManager.this));
					
				}
				
			});
		
	}

	/**
	 * 	为用户创建ChannelPool
	 * 	@param user 用户句柄
	 * 	@return ChannelPool实例
	 * */
	public ChannelPool createChannelPool(User user) {
		
		return new ChannelPool(this,user);
	}

	/**
	 * 	请求与某一个频道建立连接
	 * 	@throws Exception 
	 * 	
	 * */
	public ChannelHandler connect(NaiveNetMessage msg, String channelName) throws Exception {
		ChannelInfo info = this.naiveNetServerHandler.config.getChannelInfo(channelName);
		if(info == null) { //不存在的频道信息
			NaiveNetResponseData nrd = new NaiveNetResponseData(msg,CodeMap.NOT_FOUND_CHANNEL,false);
			msg.user.responseClient(nrd);
			return null;
		}
		ChannelFuture cf = boot.connect(info.getIP(),info.getPort()).sync();
		//连接建立成功
		ChannelHandler handler = new ChannelHandler(cf.channel(),msg);
		channelList.add(handler);
		hashmap.put(handler.getChannel(), handler);
		return handler;
	}
	

	/**
	 * 	当一个连接关闭后移除该连接信息
	 * */
	public void onChannelClose(ChannelHandler channelHandler) {
		this.channelList.remove(channelHandler);
	}

}
