package cn.naivenet.User;


/**
 * 	客户端申请关闭连接（不可恢复 资源全部释放）
 * */
class ControllerClose extends NaiveNetController {

	public ControllerClose() {
		super("close");

	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		msg.user.quit();
		return null;
	}
	
}