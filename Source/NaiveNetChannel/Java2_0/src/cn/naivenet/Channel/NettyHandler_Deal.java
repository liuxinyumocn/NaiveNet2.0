package cn.naivenet.Channel;

import io.netty.channel.ChannelInboundHandlerAdapter;

class NettyHandler_Deal extends ChannelInboundHandlerAdapter{

	ClientHandler client;
	NaiveNetChannelServer server;
	
	public NettyHandler_Deal(ClientHandler client, NaiveNetChannelServer server) {
		this.client = client;
		this.server = server;
		
		this.server._onNewUser(client);
	}

	
	
}
