package cn.domoe.naivenet.Server;

import cn.domoe.naivenet.User.User;
import cn.domoe.naivenet.User.UserManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 入流量计数器
 * */
public class NaiveNetInCounterHandler extends ChannelInboundHandlerAdapter{

	private UserManager userManager;
	
	public NaiveNetInCounterHandler(UserManager userManager) {
		this.userManager = userManager;
	}
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		User user = userManager.FindUser(ctx.channel());
		if(user != null)
			user.InCounter(((ByteBuf)msg).readableBytes());
		
		ctx.fireChannelRead(msg);
	}
	
}
