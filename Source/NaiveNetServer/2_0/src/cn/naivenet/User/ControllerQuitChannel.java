package cn.naivenet.User;

import cn.domoe.naivenet.User.CodeMap;
import cn.domoe.naivenet.User.NaiveNetController;
import cn.domoe.naivenet.User.NaiveNetMessage;
import cn.domoe.naivenet.User.NaiveNetResponseData;

/**
 * 	客户端请求退出某个频道
 * 	param 是频道ID
 * */
class ControllerQuitChannel extends NaiveNetController {

	public ControllerQuitChannel() {
		super("quitChannel");

	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		try {
			Integer channelID = Integer.parseInt(new String(msg.param));
			//退出特定的频道
			msg.user.channelPool.quitChannel(channelID);
			return null;
		}catch(Exception e) {
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.DATA_FORMAT_ERROR,false);
			return res;
		}
	}
	
}