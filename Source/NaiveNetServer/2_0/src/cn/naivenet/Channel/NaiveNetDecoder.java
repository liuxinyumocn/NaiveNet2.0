package cn.naivenet.Channel;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;


/**
 * 	NaiveNet自解码器 对数据流进行拆包解码处理
 * */
public class NaiveNetDecoder extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		in.markReaderIndex();
		byte[] data = new byte[in.readableBytes()];
		in.readBytes(data);
		in.resetReaderIndex();
		
		int value = 0; //len
		int index = 0;
		for(;index < data.length ;index++) {
			int a = data[index]&0x0FF;
			value += a;
			if(a != 255) {
				break;
			}
		}
		index++;
		//value 代表取出的数据长度
		//index 代表最后一位数值标识的索引
		if(index + value > data.length) { //发生断包
			return;
		}
		//读取的长度为 index + value
		in.readBytes(index).release();
		data = new byte[value];
		in.readBytes(data);
		
		out.add(data);
	}
	
	

}
