package cn.domoe.naivenet.Server;

import cn.domoe.naivenet.User.UserManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class NaiveNetHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private UserManager userManager;
	
	public NaiveNetHttpServerHandler(UserManager userManager) {
		this.userManager = userManager;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
		System.out.println(msg.getUri());
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1
				,HttpResponseStatus.NOT_FOUND
				,Unpooled.wrappedBuffer("test".getBytes()));
		HttpHeaders heads = response.headers();
		heads.add(HttpHeaderNames.CONTENT_TYPE,HttpHeaderValues.TEXT_PLAIN + "; charset=UTF-8");
		heads.add(HttpHeaderNames.CONTENT_LENGTH,response.content().readableBytes());
		heads.add(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);
		
		ctx.writeAndFlush(response);
		
	}
	
	 @Override
	 public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		 //连接发生断开
		 System.out.println("HTTP 连接断开");

	 }

	 @Override
	 public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	     System.out.println("HTTP 异常发生");
	     ctx.close();
	 }

}
