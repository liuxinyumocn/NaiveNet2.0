package cn.naivenet.Channel;


class CtrlOnQuit extends NaiveNetController{

	public CtrlOnQuit() {
		super("onquit");	}

	@Override
	public NaiveNetResponse onRequest(NaiveNetMessage msg) {
		msg.user._onQuit();
		return null;
	}

}
