package cn.naivenet.Channel;

import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

public class Controller_quitchannel extends NaiveNetController{

	public Controller_quitchannel() {
		super("quitchannel");
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		Integer channelID = msg.channelid;
		msg.user.quitChannel(channelID);
		return null;
	}

}