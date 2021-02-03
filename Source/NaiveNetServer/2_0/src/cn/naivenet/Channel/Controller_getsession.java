package cn.naivenet.Channel;

import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

public class Controller_getsession extends NaiveNetController{

	public Controller_getsession() {
		super("getsession");
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {		
		byte[] value = msg.user.getSession(new String(msg.param));
		if(value == null) {
			value = "".getBytes();
		}
		NaiveNetResponseData res = new NaiveNetResponseData(msg,value,true);
		return res;
	}

}