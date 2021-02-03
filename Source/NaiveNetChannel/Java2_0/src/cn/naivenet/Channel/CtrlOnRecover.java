package cn.naivenet.Channel;


class CtrlOnRecover extends NaiveNetController{

	public CtrlOnRecover() {
		super("onrecover");
	}

	@Override
	public NaiveNetResponse onRequest(NaiveNetMessage msg) {
		msg.user._onRecover();
		return null;
	}

}
