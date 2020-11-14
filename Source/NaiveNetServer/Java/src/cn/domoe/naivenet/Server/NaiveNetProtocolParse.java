package cn.domoe.naivenet.Server;

import cn.domoe.naivenet.User.UserManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * NaiveNet 请求协议解析器
 * 
 * */
public class NaiveNetProtocolParse extends ChannelInboundHandlerAdapter{

	private UserManager userManager;
	
	private HttpServerCodec hHttpServerCodec;
	private ChunkedWriteHandler hChunkedWriteHandler;
	private HttpObjectAggregator hHttpObjectAggregator;
	private NaiveNetHttpServerHandler hNaiveNetHttpServerHandler;
	private WebSocketServerProtocolHandler hWebSocketServerProtocolHandler;
	private NaiveNetBinaryWebSocketFrameHandler hNaiveNetBinaryWebSocketFrameHandler;
	private NaiveNetNaiveProtocolHandler hNaiveNetNaiveProtocolHandler;
	
	public NaiveNetProtocolParse(UserManager userManager) {
		this.userManager = userManager;
		

		hHttpServerCodec = new HttpServerCodec();
		hChunkedWriteHandler = new ChunkedWriteHandler();
		hHttpObjectAggregator = new HttpObjectAggregator(1024 * 8);
		hNaiveNetHttpServerHandler = new NaiveNetHttpServerHandler(userManager);
		hWebSocketServerProtocolHandler = new WebSocketServerProtocolHandler("/");
		hNaiveNetBinaryWebSocketFrameHandler = new NaiveNetBinaryWebSocketFrameHandler(userManager);
		hNaiveNetNaiveProtocolHandler =new NaiveNetNaiveProtocolHandler();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		//进行协议解析
		ByteBuf buf = (ByteBuf)msg;
		buf.markReaderIndex();
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		buf.resetReaderIndex();
		
		ChannelPipeline pipeline = ctx.channel().pipeline();
		
		//解析协议类型
		//如果前几位是 NAIVENET 则判定为NaiveNet网络协议 否则按照HTTP或者WS协议处理
		int index = NaiveNetProtocolParse.bytearrindexof(data, data.length, "NAIVENET".getBytes(), 8);
		if(index == -1) { //按照HTTP 或者 WS处理
			pipeline.addLast(hHttpServerCodec);
			pipeline.addLast(hChunkedWriteHandler);
			pipeline.addLast(hHttpObjectAggregator);
			
			index = NaiveNetProtocolParse.bytearrindexof(data, data.length, "Sec-WebSocket-Key".getBytes(), 1024);
			if(index == -1) {
				pipeline.addLast(hNaiveNetHttpServerHandler);
				System.out.println("HTTP");
			}else {
				pipeline.addLast(hWebSocketServerProtocolHandler);
				pipeline.addLast(hNaiveNetBinaryWebSocketFrameHandler);
				//pipeline.addLast(new NaiveNetBinaryWebSocketOutBoundHandler());
				System.out.println("WS");
			}
		}else { //否则按照NAIVENET协议通信
			System.out.println("NAIVE");
			pipeline.addLast(hNaiveNetNaiveProtocolHandler);
		}
		pipeline.remove(this);
		ctx.fireChannelRead(msg);
	}
	
	/**
	 * 寻找B byte[] 在 A byte[] 中首次出现的位置，且规定识别提前结束的索引位置 END 索引位置是识别首位的索引位置，若没有出现，则return -1
	 * */
	private static int bytearrindexof(byte[] A,int ALEN,byte[] B,int END) {
		if(B.length == 0 || ALEN == 0)
			return -1;
		for(int a = 0;a <= ALEN - B.length && a <= END;a++) {
			int same = 0;
			for(int b = 0;b<B.length;b++) {
				if(A[a+b] == B[b]) {
					same++;
				}else {
					same = -1;
					break;
				}
			}
			if(same != -1)
				return a;
		}
		return -1;
	}
	
}
