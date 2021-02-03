package cn.naivenet.Channel;

import cn.naivenet.TimerEvent.Task;
import cn.naivenet.TimerEvent.Timer;
import cn.naivenet.TimerEvent.TimerTask;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

/**
 * 	用于验证NS身份有效性
 * */
class NettyHandler_Auth extends ChannelInboundHandlerAdapter {
	
	private ClientHandler client;
	private NaiveNetChannelServer server;
	private Task t;
	
	public NettyHandler_Auth(ClientHandler client,NaiveNetChannelServer server) {
		this.client = client;
		this.server = server;
		
		//需要在3000ms内完成授权，如果没有完成则提前结束
		this.initAuthCheck();
	}

	/**
	 * 	初始化授权检查器
	 * */
	private void initAuthCheck() {
		t = Timer.SetTimeOut(new TimerTask() {

			@Override
			public void Event() {
				
				fail();
				
			}}, 3000);
		
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		this.client._onClose();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		byte[] data = (byte[])msg;
		byte[] token = server.getToken();
		
		if(data.length != token.length) {
			this.fail();
			return;
		}else {
			for(int i = 0;i<data.length;i++) {
				if(data[i] != token[i]) {
					this.fail();
					return;
				}
			}
		}
		success(ctx);
	}
	
	/**
	 * 	失败，退出频道
	 * */
	private void fail() {
		client.close();
	}
	
	/**
	 * 	成功
	 * */
	private void success(ChannelHandlerContext ctx) {
		Timer.CancelTask(t);
		client.send("NAIVENETCHANNEL CODE[OK]".getBytes());
		ChannelPipeline pip = ctx.pipeline();
		pip.addLast(new NettyHandler_Deal(client,server));
		pip.remove(this);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//cause.printStackTrace();
		ctx.close();
	}

}
