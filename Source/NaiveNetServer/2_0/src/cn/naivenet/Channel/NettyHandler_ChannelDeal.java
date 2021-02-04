package cn.naivenet.Channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyHandler_ChannelDeal extends ChannelInboundHandlerAdapter {

	private ChannelHandler channelHandler;
	private ChannelManager channelManager;
	
	public NettyHandler_ChannelDeal(ChannelHandler chd ,ChannelManager chm) {
		this.channelHandler = chd;
		this.channelManager = chm;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		channelManager.onChannelClose(channelHandler);
		channelHandler._onClose();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		byte[] data = (byte[])msg;
		channelHandler._onRead(data);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//cause.printStackTrace();
		ctx.close();
	}
	
	

}
