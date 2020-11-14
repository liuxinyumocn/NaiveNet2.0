package cn.domoe.naivenet.Channel;

import cn.domoe.naivenet.User.NaiveNetMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class NaiveNetSendToChannelOutboundHandler extends ChannelOutboundHandlerAdapter{

	@Override
	public void write(ChannelHandlerContext ctx ,Object msg,ChannelPromise promise) throws Exception {
		//准备向NC发送数据 Object msg 需要提供 NaiveNetMessage
		ctx.write(msg,promise);
	
	}
	
	
}
