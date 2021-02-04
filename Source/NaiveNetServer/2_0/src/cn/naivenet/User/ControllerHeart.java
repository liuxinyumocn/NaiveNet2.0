package cn.naivenet.User;


class ControllerHeart extends NaiveNetController {

	public ControllerHeart() {
		super("heart");
	}
	
	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		msg.user.setPing(new String(msg.param));
		return null;
	}
	
}
