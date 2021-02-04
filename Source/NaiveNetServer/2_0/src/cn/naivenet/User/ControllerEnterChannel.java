package cn.naivenet.User;


//请求与对应Channel建立连接
class ControllerEnterChannel extends NaiveNetController {

	public ControllerEnterChannel() {
		super("enterChannel");
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		//System.out.println("申请进入"+new String(msg.param));
		
		User user = msg.user;
		user.enterChannel(msg);
		NaiveNetResponseData res = new NaiveNetResponseData();
		return res;
	}
	
}