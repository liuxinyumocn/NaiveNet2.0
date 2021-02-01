package cn.naivenet.Channel;

import java.nio.channels.Channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

public class NettyHandler_ChannelAuth extends ChannelInboundHandlerAdapter {

	private ChannelHandler channelHandler;
	private ChannelManager channelManager;
	
	public NettyHandler_ChannelAuth(ChannelHandler chd ,ChannelManager chm) {
		this.channelHandler = chd;
		this.channelManager = chm;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//发生断线
		channelManager.onChannelClose(channelHandler);
		channelHandler._onClose();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		byte[] data = (byte[])msg;
		String response = new String(data);
		if(response.equals("NAIVENETCHANNEL CODE[OK]")) { //回应正确
			ChannelPipeline pip = ctx.channel().pipeline();
			pip.remove(this);
			pip.addLast(new NettyHandler_ChannelDeal(this.channelHandler,this.channelManager));
			
		}else { //回应不正确
			ctx.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}

}
