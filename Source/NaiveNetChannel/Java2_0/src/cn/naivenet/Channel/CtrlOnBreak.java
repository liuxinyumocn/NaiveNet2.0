package cn.naivenet.Channel;


class CtrlOnBreak  extends NaiveNetController{

	public CtrlOnBreak() {
		super("onbreak");
	}

	@Override
	public NaiveNetResponse onRequest(NaiveNetMessage msg) {
		msg.user._onBreak();
		return null;
	}

}
