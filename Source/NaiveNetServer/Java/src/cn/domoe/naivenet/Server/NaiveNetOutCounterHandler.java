package cn.domoe.naivenet.Server;

import cn.domoe.naivenet.User.User;
import cn.domoe.naivenet.User.UserManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * 出流量计数器
 * */
public class NaiveNetOutCounterHandler extends ChannelOutboundHandlerAdapter{

	private UserManager userManager;
	
	public NaiveNetOutCounterHandler(UserManager userManager) {
		this.userManager = userManager;
	}
	
	@Override
	public void write(ChannelHandlerContext ctx ,Object msg,ChannelPromise promise) throws Exception {
		User user = userManager.FindUser(ctx.channel());
		if(user != null)
			user.OutCounter(((ByteBuf)msg).readableBytes());
		ctx.write(msg,promise);
	}
	
}
