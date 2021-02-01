import cn.naivenet.ClientSocket.NettyHandler_WebSocketFrameHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class test {

	EventLoopGroup boss;
	EventLoopGroup worker;
	ServerBootstrap boot;
	
	public test() throws Exception {
		
		boss = new NioEventLoopGroup(1);
		worker = new NioEventLoopGroup();
		boot = new ServerBootstrap();
		
		try {
			boot.group(boss,worker)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInit())
				.option(ChannelOption.SO_BACKLOG, 1024);
			ChannelFuture future = boot.bind(5000).sync();
			future.channel().closeFuture().sync();
		}finally {
			worker.shutdownGracefully();
			boss.shutdownGracefully();
		}
	}
	
	class ChannelInit extends ChannelInitializer<SocketChannel>{

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			System.out.println("initChannel"); //1
			System.out.println(ch);
			
			ChannelPipeline pipeline = ch.pipeline();
			
			pipeline.addLast(new Handler1());
			
			//解析协议
//			pipeline.addLast(new HttpServerCodec());
//			pipeline.addLast(new ChunkedWriteHandler());
//			pipeline.addLast(new HttpObjectAggregator(1024 * 8));
//			pipeline.addLast(new WebSocketServerProtocolHandler("/"));
			//pipeline.addLast(new NettyHandler_WebSocketFrameHandler());
			
			System.out.println("FinishedinitChannel"); //1
			
		}
		
	}
	
	class Handler1 extends ChannelInboundHandlerAdapter {

		public Handler1() {
			System.out.println("handler1"); //2
			
		}
		
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			System.out.println("active"); //4
			super.channelActive(ctx);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			System.out.println("Inactive");
			super.channelInactive(ctx);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			System.out.println("channelRead:"); //5
			System.out.println(ctx.channel());
			ByteBuf buf = (ByteBuf)msg;
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			System.out.println(new String(data));
			//int a = 1/0;
			super.channelRead(ctx, msg);
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			System.out.println("channelReadComplete"); //6
			super.channelReadComplete(ctx);
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			System.out.println("userEventTriggered");
			super.userEventTriggered(ctx, evt);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.out.println("exceptionCaught");
			super.exceptionCaught(ctx, cause);
		}

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			System.out.println("handlerAdded"); //3
			super.handlerAdded(ctx);
		}

		@Override
		public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
			System.out.println("handlerRemoved");
			super.handlerRemoved(ctx);
		}
		
	}
	
	class Handler2 extends ChannelInboundHandlerAdapter {

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			// TODO Auto-generated method stub
			
			System.out.println("捕获异常");
			ctx.close();
			//super.exceptionCaught(ctx, cause);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			System.out.println("channelRead2");
			super.channelRead(ctx, msg);
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			System.out.println("channelReadComplete2");
			super.channelReadComplete(ctx);
		}

		
	}
	
	
	public static void main(String[] args) {

		try {
			new test();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
