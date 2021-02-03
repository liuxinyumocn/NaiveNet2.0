package cn.naivenet.Channel;

public class NaiveNetUserMessage extends NaiveNetMessage{

	public NaiveNetUserMessage(byte[] data, User user) {
		this.user = user;
		this.data = data;
		
		this.control = data[0];													//控制帧
		this.msgid = data[1];											//消息ID
		Number c = NaiveNetUserMessage.parseNumber(2, data);
		this.channelid = c.value;												//频道ID
		this.channeldata = new byte[c.length];
		System.arraycopy(data, 2, this.channeldata, 0, c.length);
		
		if(control == 0 || control == 3) { //回复请求
			// 0 控制帧 1 消息ID 
			int s= 2 + c.length;
			//参数
			Content param = NaiveNetUserMessage.parsePart(s, data);
			this.param = param.content;											//参数
		}else{//请求操作
			// 0 控制帧 1 消息ID 
			int s= 2 + c.length;
			Content ctrl = NaiveNetUserMessage.parsePart(s, data);
			controller = new String(ctrl.content);
			//参数
			Content param = NaiveNetUserMessage.parsePart(ctrl.index, data);
			this.param = param.content;											//参数
		}
	}

	static class Number {
		public Number(int l,int v) {
			length = l;
			value = v;
		}
		public int length = 0;
		public int value = 0;
	}
	
	static class Content{
		public int index;
		public byte[] content;
		public Content(int l ,byte[] data) {
			index = l;
			content = data;
		}
		
	}
	
	/**
	 * 解析长度
	 * start 解析的起始索引
	 * data[] 数据
	 * 
	 * Number value 解析的值
	 * 		  length 该值占的位数
	 * */
	private static Number parseNumber(int start ,byte[] data) {
		int value = 0;
		int n = 0;
		for(int i = start;i<data.length;i++) {
			int a = data[i]&0x0FF;
			value += a;
			n++;
			if(a != 255)
			{
				return new Number(n,value);
			}
		}
		
		return new Number(n,value);
	}
	

	/*
	 *	 解析片段
	 * */
	private static Content parsePart(int start , byte[] data) {
		//解析长度
		Number length = NaiveNetUserMessage.parseNumber(start, data);
		if(length.value == 0) { //说明正文长度为0
			return new Content(start + 1,new byte[0]);
		}
		byte[] content = new byte[length.value];
		int j = start + length.length;
		for(int i = 0;j<data.length&&i<content.length;i++) {
			content[i] = data[j];
			j++;
		}
		return new Content(length.length + content.length + start,content);
	}
	
}
