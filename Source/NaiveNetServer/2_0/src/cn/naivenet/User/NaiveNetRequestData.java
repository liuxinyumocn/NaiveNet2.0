package cn.naivenet.User;

/**
 * 	发送请求数据结构体
 * */
public class NaiveNetRequestData {

	byte msgid;
	byte[] channeldata;
	byte[] param;
	byte control;
	byte[] controller;
	
	public NaiveNetRequestData(int control,int msgid,int channelid,String controller,byte[] param) {
		this.msgid = (byte)msgid;
		this.controller = controller.getBytes();
		this.channeldata = NaiveNetRequestData.calNumber(channelid);
		this.param = param;
		this.control = (byte)control;
	}
	
	public byte[] genData() {
		byte[] param_number = NaiveNetRequestData.calNumber(this.param);
		byte[] ctrl_number = NaiveNetRequestData.calNumber(this.controller);
		byte[] data = new byte[2 + this.channeldata.length + param_number.length+ ctrl_number.length + this.param.length + this.controller.length];
		data[0] = this.control;
		data[1] = this.msgid;
		int index = 2;
		System.arraycopy(this.channeldata, 0, data, index, this.channeldata.length);
		index += this.channeldata.length;
		System.arraycopy(ctrl_number, 0, data, index, ctrl_number.length);
		index += ctrl_number.length;
		System.arraycopy(controller, 0, data, index, controller.length);
		index += controller.length;
		System.arraycopy(param_number, 0, data, index, param_number.length);
		index += param_number.length;
		System.arraycopy(param, 0, data, index, param.length);
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
