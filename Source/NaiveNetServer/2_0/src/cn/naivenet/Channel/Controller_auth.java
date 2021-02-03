package cn.naivenet.Channel;

import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

public class Controller_auth extends NaiveNetController{

	public Controller_auth() {
		super("auth");
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {		
		msg.user.auth();
		return null;
	}

}
