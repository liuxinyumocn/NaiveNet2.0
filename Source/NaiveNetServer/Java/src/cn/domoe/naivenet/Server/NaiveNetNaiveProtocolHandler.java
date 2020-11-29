package cn.domoe.naivenet.Server;

import cn.domoe.naivenet.User.User;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NaiveNetNaiveProtocolHandler extends ChannelInboundHandlerAdapter{
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		
		ByteBuf buf = (ByteBuf)msg;
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		System.out.println(new String(data));
		buf.release();
//		User user = userManager.FindUser(ctx.channel());
//		if(user != null)
//			user.InCounter(((ByteBuf)msg).readableBytes());
//		
		//ctx.fireChannelRead(msg);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		
		System.out.println("发生异常");
		ctx.close();
		
	}
}
