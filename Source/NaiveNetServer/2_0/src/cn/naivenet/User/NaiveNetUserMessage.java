package cn.naivenet.User;

public class NaiveNetUserMessage extends NaiveNetMessage{

	public NaiveNetUserMessage(byte[] data, User user) {
		
		this.user = user;
		this.data = data;
		
		this.control = data[0];													//控制帧
		this.msgid = data[1];													//消息ID
		Number c = NaiveNetUserMessage.parseNumber(2, data);
		
		this.channelid = c.value;												//频道ID
		this.channeldata = new byte[c.length];
		System.arraycopy(data, 2, this.channeldata, 0, c.length);
		
		if(control == 0) { //回复请求
			// 0 控制帧 1 消息ID 
			int s= 2 + c.length;
			//参数
			Content param = NaiveNetUserMessage.parsePart(s, data);
			this.param = param.content;											//参数
		}else{//请求操作
			// 0 控制帧 1 消息ID 
			int s= 2 + c.length;
			Content ctrl = NaiveNetUserMessage.parsePart(s, data);
			try {
				controller = new String(ctrl.content,"utf-8");					//控制器名称
			}catch(Exception e) {
				controller = "";
			}
			//参数
			Content param = NaiveNetUserMessage.parsePart(ctrl.index, data);
			this.param = param.content;											//参数
		}
		
		//this.print();
		
	}
	
	/**
	 * 	设置频道ID并重新整理 data
	 * */
	public void setChannelID(int channelID) {
		this.channelid = channelID;
		byte[] newchannelID = NaiveNetUserMessage.calNumber(channelID);
		if(newchannelID.length == this.channeldata.length) {
			for(int i = 0;i<newchannelID.length;i++) {
				this.data[i+2] = newchannelID[i];
			}
		}else {
			byte[] newdata = new byte[this.data.length - this.channeldata.length + newchannelID.length];
			int index = 0;
			System.arraycopy(data, 0, newdata, index, 2);
			index+=2;
			System.arraycopy(newchannelID, 0, newdata, index, newchannelID.length);
			index+=newchannelID.length;
			System.arraycopy(data, 2+this.channeldata.length, newdata, index, newdata.length - index);
			this.data = newdata;
		}
		this.channeldata = newchannelID;
	}
	
	public static byte[] calNumber(int len) {
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
	
	private void print() {
		System.out.println("controller : ["+this.controller+"]");
		System.out.println("param :["+new String(this.param)+"]");
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
		public Content(int i ,byte[] data) {
			index = i;
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
	

	/**
	 * 	解析片段
	 * 	从长度位开始解析
	 * 	start 第一个长度位
	 * 	data byte[]
	 * 
	 * 	返回值 
	 * 		index 下一个索引位置 
	 * 		value 正文内容
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
