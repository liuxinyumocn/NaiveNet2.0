package cn.naivenet.Channel;

import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

public class Controller_getlinkinfo extends NaiveNetController{

	public Controller_getlinkinfo() {
		super("getlinkinfo");
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {		
		String json = msg.user.getLinkInfo();
		NaiveNetResponseData res = new NaiveNetResponseData(msg,json.getBytes(),true);
		return res;
	}

}