package cn.naivenet;

import cn.naivenet.Config.NaiveNetConfig;
import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

class ControllerGetChannelConf extends NaiveNetController{

	private NaiveNetConfig config;
	
	public ControllerGetChannelConf(NaiveNetConfig config) {
		super("getChannelConf");
		this.config = config;
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		String json = config.getChannelConf();
		return new NaiveNetResponseData(msg,json.getBytes(),true);
	}

}
