package cn.naivenet;

import java.io.UnsupportedEncodingException;

import cn.naivenet.Config.NaiveNetConfig;
import cn.naivenet.User.CodeMap;
import cn.naivenet.User.NaiveNetController;
import cn.naivenet.User.NaiveNetMessage;
import cn.naivenet.User.NaiveNetResponseData;

class ControllerSetChannelConf extends NaiveNetController{

	private NaiveNetConfig config;
	
	public ControllerSetChannelConf(NaiveNetConfig config) {
		super("setChannelConf");
		this.config = config;
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		try {
			String json = new String(msg.param,"utf-8");
			System.out.println(json);
			config.setChannelInfo(json);
		} catch (UnsupportedEncodingException e) {
			return new NaiveNetResponseData(msg,CodeMap.DATA_FORMAT_ERROR,false);
		} catch (Exception e) {
			return new NaiveNetResponseData(msg,CodeMap.DATA_FORMAT_ERROR,false);
		}
		return new NaiveNetResponseData(msg,CodeMap.OK,true);
	}

}
