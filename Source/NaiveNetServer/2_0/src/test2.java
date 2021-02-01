import java.net.SocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class test2 {

	
	
	EventLoopGroup boss;
	Bootstrap boot;
	
	Channel c;
	
	public test2() throws Exception {
		
		boss = new NioEventLoopGroup();
		boot = new Bootstrap();
		boot.group(boss)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					System.out.println("初始化"); //1
					System.out.println(ch);
					c = ch;
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new Handler1());
					pipeline.addLast(new Handler2());
				}
				
			});
		
		System.out.println("连接1");
		ChannelFuture cf = boot.connect("127.0.0.1",5000).sync();
		Channel channel = cf.channel();
		System.out.println(channel);
		System.out.println(c == channel);
		ByteBuf buf = channel.alloc().buffer();
		buf.writeBytes("hello".getBytes());
		System.out.println("发消息");
		channel.writeAndFlush(buf);
		
		Thread.sleep(5000);
		

		System.out.println("连接2");
		ChannelFuture cf2 = boot.connect("127.0.0.1",5000).sync();
		Channel channel2 = cf2.channel();
		ByteBuf buf2 = channel2.alloc().buffer();
		buf2.writeBytes("hello2".getBytes());
		System.out.println("发消息2");
		channel2.writeAndFlush(buf2);
		


		Thread.sleep(5000);
		System.out.println("发消息3");
		buf = channel.alloc().buffer();
		buf.writeBytes("hello1111".getBytes());
		System.out.println("发消息2");
		channel.writeAndFlush(buf);
		
		

		Thread.sleep(5000);
		System.out.println("关闭1");
		channel.close();
		
		Thread.sleep(5000);
		System.out.println("关闭2");
		channel2.close();
		
	}
	
	
	class Handler1 extends ChannelInboundHandlerAdapter {
		public Handler1() {
			System.out.println("handler1"); //2
		}
		
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			System.out.println("active");
			super.channelActive(ctx);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			System.out.println("Inactive");
			super.channelInactive(ctx);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			System.out.println("channelRead");
			super.channelRead(ctx, msg);
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			System.out.println("channelReadComplete");
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
			System.out.println("handlerAdded");
			super.handlerAdded(ctx);
		}

		@Override
		public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
			System.out.println("handlerRemoved");
			super.handlerRemoved(ctx);
		}
	}
	
	class Handler2 extends ChannelOutboundHandlerAdapter {

		public Handler2() {
			System.out.println("handler2"); //2
		}
		
		@Override
		public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
				ChannelPromise promise) throws Exception {
			System.out.println("connect");
			super.connect(ctx, remoteAddress, localAddress, promise);
		}

		@Override
		public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
			System.out.println("disconnect");
			super.disconnect(ctx, promise);
		}

		@Override
		public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
			System.out.println("close");
			super.close(ctx, promise);
		}

		@Override
		public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
			System.out.println("deregister");
			super.deregister(ctx, promise);
		}

		@Override
		public void read(ChannelHandlerContext ctx) throws Exception {
			System.out.println("read");
			super.read(ctx);
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			System.out.println("write");
			super.write(ctx, msg, promise);
		}

		@Override
		public void flush(ChannelHandlerContext ctx) throws Exception {
			System.out.println("flush");
			super.flush(ctx);
		}

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			System.out.println("handlerAdded2");
			super.handlerAdded(ctx);
		}

		@Override
		public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
			System.out.println("handlerRemoved2");
			super.handlerRemoved(ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.out.println("exceptionCaught");
			super.exceptionCaught(ctx, cause);
		}
		
	}
	
	public static void main(String[] args) {
		try {
			new test2();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("123123213");
			e.printStackTrace();
		}
	}
	
}
