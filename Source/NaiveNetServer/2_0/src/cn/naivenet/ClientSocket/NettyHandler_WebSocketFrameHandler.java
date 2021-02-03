package cn.naivenet.ClientSocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class NettyHandler_WebSocketFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

	private ClientHandler client;
	
	public NettyHandler_WebSocketFrameHandler(ClientHandler client) {
		this.client = client;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("断线了");
		this.client._onClose();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) throws Exception {
		ByteBuf buf = msg.content();
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		
		//新的数据已经完全读取（已经处理断粘包）
		this.client._onRead(data);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//通信句柄发生异常，Pool回收通信句柄，并将异常向外传播
		cause.printStackTrace();
		ctx.close();
		this.client._onExceptionCaught(cause);
	}

}
