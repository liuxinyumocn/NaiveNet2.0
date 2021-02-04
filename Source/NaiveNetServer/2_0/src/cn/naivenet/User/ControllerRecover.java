package cn.naivenet.User;

import cn.naivenet.ClientSocket.ClientHandler;

/**
 * 	客户端此前发生断线，请求进行连接恢复
 * */
class ControllerRecover extends NaiveNetController {

	private UserManager userManager;

	public ControllerRecover(UserManager userManager) {
		super("recover");
		this.userManager = userManager;
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		//使用网络恢复功能当前用户不能是已经Auth的状态
		if(msg.user.isAuth()) {
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.RECOVER_FAILED,false);
			return res;
		}
		
		String token = new String(msg.param);
		User user = this.userManager.getUserBySESSION(token);
		if(user == null) { //说明没有用户信息或者用户信息错误 无法进行网络恢复
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.RECOVER_FAILED,false);
			return res;
		}
		//替换通信句柄		
		user.recoverChannelHandler(msg);
		
		return new NaiveNetResponseData();	//使用CANCEL不提交回复
	}
	
}