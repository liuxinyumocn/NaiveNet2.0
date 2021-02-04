package cn.naivenet.Channel;

import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

public class Controller_setsession extends NaiveNetController{

	public Controller_setsession() {
		super("setsession");
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {		
		ValueInfo key = Controller_setsession.parseValue(0, msg.param);
		byte[] keyBytes = key.value;
		ValueInfo value = Controller_setsession.parseValue(key.end, msg.param);
		byte[] valueBytes = value.value;
		msg.user.setSession(new String(keyBytes),valueBytes);
		return null;
	}
	
	static class ValueInfo{
		public int end = 0;
		public byte[] value = null;
	}
	private static ValueInfo parseValue(int start,byte[] data) {
		//Log.print(data);
		ValueInfo res = new ValueInfo();
		//先检测长度
		int length = 0;
		int j = 1;
		for(;start<data.length;start++) {
			length += (int)data[start];
			j++;
			if(data[start] != 255) {
				start++;
				break;
			}
		}
		res.value = new byte[length];
		for(int i = 0;i<res.value.length;i++) {
			res.value[i] = data[start++];
		}
		res.end = start;
		return res;
	}

}