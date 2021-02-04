package cn.naivenet.Channel;

import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

public class Controller_ping extends NaiveNetController{

	public Controller_ping() {
		super("ping");
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {		
		NaiveNetResponseData res = new NaiveNetResponseData(msg,msg.user.getPing().getBytes(),true);
		return res;
	}

}