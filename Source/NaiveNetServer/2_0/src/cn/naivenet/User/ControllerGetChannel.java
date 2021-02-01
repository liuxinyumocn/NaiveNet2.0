package cn.naivenet.User;

import cn.naivenet.NaiveNetServerHandler;
import cn.naivenet.Config.ChannelInfo;

public class ControllerGetChannel extends NaiveNetController {
	
	private NaiveNetServerHandler naivenetServerHandler;

	public ControllerGetChannel(NaiveNetServerHandler naiveNetServer) {
		super("getChannel");
		this.naivenetServerHandler = naiveNetServer;
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		String channel = new String(msg.param);
		ChannelInfo n = naivenetServerHandler.config.getChannelInfo(channel);
		if(n == null) {
			NaiveNetResponseData nrd = new NaiveNetResponseData(msg,CodeMap.NOT_FOUND_CHANNEL,false);
			return nrd;
		}
		
		String id = Integer.toString(n.getID());
		
		NaiveNetResponseData res = new NaiveNetResponseData(msg,id.getBytes(),true);
		return res;
	}

}
