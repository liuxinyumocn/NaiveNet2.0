package cn.naivenet.Channel;

import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

public class Controller_clearsession  extends NaiveNetController{

	public Controller_clearsession() {
		super("clearsession");
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {		
		msg.user.clearSession();
		return null;
	}

}