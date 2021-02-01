package cn.naivenet.Channel;

import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyHandler_ChannelDeal extends ChannelInboundHandlerAdapter {

	private ChannelHandler channelHandler;
	private ChannelManager channelManager;
	
	public NettyHandler_ChannelDeal(ChannelHandler chd ,ChannelManager chm) {
		this.channelHandler = chd;
		this.channelManager = chm;
	}
	
	

}
