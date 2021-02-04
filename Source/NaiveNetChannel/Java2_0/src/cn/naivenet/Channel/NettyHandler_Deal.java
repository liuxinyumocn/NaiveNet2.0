package cn.naivenet.Channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

class NettyHandler_Deal extends ChannelInboundHandlerAdapter{

	ClientHandler client;
	NaiveNetChannelServer server;
	
	public NettyHandler_Deal(ClientHandler client, NaiveNetChannelServer server) {
		this.client = client;
		this.server = server;
		
		this.server._onNewUser(client);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		server._onClientClose(client);
		client._onClose();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		byte[] data = (byte[])msg;
		client._onRead(data);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	
	
}
