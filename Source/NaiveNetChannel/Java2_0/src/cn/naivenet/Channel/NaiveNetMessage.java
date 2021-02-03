package cn.naivenet.Channel;

public abstract class NaiveNetMessage {

	public byte control;
	public byte msgid;
	public int channelid;
	public byte[] channeldata; //频道数据格式
	public String controller;
	public byte[] param;
	public User user;
	public byte code;
	//public Channel channelOrigin;
	
	public byte[] data;
	
	/**
	 * 	获得用于回应的 {@link NaiveNetResponse} 句柄
	 * 	res = msg.getResponseHandler()
	 * 	return res
	 * */
	public NaiveNetResponse getResponseHandler() {
		return new NaiveNetResponse(this);
	}

	/**
	 * 	获得用于回应的 {@link NaiveNetResponse} 句柄，并直接设置回应正文，正文可以是 byte[] 也可以是 String
	 * 	res = msg.getResponseHandler()
	 * 	return res
	 * */
	public NaiveNetResponse getResponseHandler(byte[] content) {
		NaiveNetResponse res = this.getResponseHandler();
		res.setContent(content);
		return res;
	}

	/**
	 * 	获得用于回应的 {@link NaiveNetResponse} 句柄，并直接设置回应正文，正文可以是 byte[] 也可以是 String
	 * 	res = msg.getResponseHandler()
	 * 	return res
	 * */
	public NaiveNetResponse getResponseHandler(String content) {
		NaiveNetResponse res = this.getResponseHandler();
		res.setContent(content);
		return res;
	}
}
