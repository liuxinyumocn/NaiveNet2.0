package cn.naivenet.User;


/**
 * 用于回复的数据体
 * */
public class NaiveNetResponseData {
	boolean cancel = false;
	byte msgid;
	byte[] channeldata;
	byte[] content;
	byte control;
	
	public boolean getCancel() {
		return this.cancel;
	}
	
	public NaiveNetResponseData() {
		this.cancel = true;
	}
	
	public NaiveNetResponseData(byte msgid,byte[] channeldata,byte[] content) {
		this.control = 0;
		this.msgid = msgid;
		this.channeldata = channeldata;
		this.content = content;
	}
	
	/**
	 * SUCCESS 是否是成功的连接
	 * @param SUCCESS 是否是成功的回复标识
	 * */
	public NaiveNetResponseData(NaiveNetMessage msg, byte[] content,boolean SUCCESS) {
		if(SUCCESS)
			this.control = 0;
		else
			this.control = 3;
		this.msgid = msg.msgid;
		this.channeldata = msg.channeldata;
		this.content = content;
	}

//	public NaiveNetResponseData(NaiveNetResponse res) {
//		this.control = 0;
//		this.msgid = res.msg.msgid;
//		this.channeldata = res.msg.channeldata;
//		this.content = res.data;
//	}

	/**
	 * 给出输出文
	 * */
	public byte[] genData() {
		byte[] number = NaiveNetResponseData.calNumber(this.content);
		byte[] data = new byte[channeldata.length + 2 + number.length + this.content.length];
		data[0] = this.control;
		data[1] = this.msgid;
		int index = 2;
		System.arraycopy(this.channeldata, 0, data, index, this.channeldata.length);
		index +=  this.channeldata.length;
		System.arraycopy(number, 0, data, index,number.length);
		index += number.length;
		System.arraycopy(this.content, 0, data, index, this.content.length);
		return data;
	}

	//计算数字位
	public static byte[] calNumber(byte[] data) {
		int len = data.length;
		return NaiveNetResponseData.calNumber(len);
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
}