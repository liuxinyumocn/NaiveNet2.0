package cn.domoe.naivenet.Server;

import cn.domoe.naivenet.User.User;
import cn.domoe.naivenet.User.UserManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

public class NewUserConnect extends ChannelInboundHandlerAdapter{
	
	private UserManager userManager;
	
	public NewUserConnect(UserManager userManager) {
		this.userManager = userManager;
	}
	
	@Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		//新的连接建立
		Channel channel = ctx.channel();
		ChannelPipeline pipeline = ctx.pipeline();
		pipeline.remove(this);
		
		this.userManager.createUser(channel);
	}

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    	//连接发生断开
    	User user = this.userManager.FindUser(ctx.channel());
    	if(user != null)
    		user.onBreak();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	User user = this.userManager.FindUser(ctx.channel());
    	if(user != null)
    		user.onQuit();
        ctx.close();
    }
	
	
}
