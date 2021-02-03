package cn.naivenet.Channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

class NaiveNetEncoder extends MessageToByteEncoder<byte[]>{

	private static byte[] calNumber(int len) {
		int length = len/255 + 1;
		byte[] res = new byte[length];
		for(int i = 0; i< res.length;i++) {
			if(len > 255) {
				res[i] = (byte)255;
				len -= 255;
			}else {
				res[i] = (byte)len;
				break;
			}
		}
		return res;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) throws Exception {
		byte[] data = (byte[])msg;
		//进行编码
		byte[] number = NaiveNetEncoder.calNumber(data.length);
		out.writeBytes(number);
		out.writeBytes(data);
	}
	
}
