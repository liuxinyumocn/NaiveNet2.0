package cn.domoe.naivenet.Server;

import cn.domoe.naivenet.User.User;
import cn.domoe.naivenet.User.UserManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class NaiveNetBinaryWebSocketFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame>{

	private UserManager userManager;
	
	public NaiveNetBinaryWebSocketFrameHandler(UserManager userManager) {
		this.userManager = userManager;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) throws Exception {
		ByteBuf buf = msg.content();
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		User user = userManager.FindUser(ctx.channel());
		//buf.release();
		if(user != null)
			user.onMessage(data);
	}
	
	 @Override
	 public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

	 }

	 @Override
	 public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		 //连接发生断开执行句柄回收
		 User user = userManager.FindUser(ctx.channel());
		 if(user != null)
			 user.onBreak();

	 }

	 @Override
	 public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		 User user = userManager.FindUser(ctx.channel());
		 if(user != null)
			 user.onBreak();
	 }

}
