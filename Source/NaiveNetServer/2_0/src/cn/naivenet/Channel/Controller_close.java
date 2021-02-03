package cn.naivenet.Channel;

import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

public class Controller_close  extends NaiveNetController{

	public Controller_close() {
		super("close");
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {		
		msg.user.quit();
		return null;
	}

}