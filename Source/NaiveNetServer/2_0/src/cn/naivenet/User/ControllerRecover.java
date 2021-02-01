package cn.naivenet.User;

import cn.domoe.naivenet.User.CodeMap;
import cn.domoe.naivenet.User.NaiveNetController;
import cn.domoe.naivenet.User.NaiveNetMessage;
import cn.domoe.naivenet.User.NaiveNetResponseData;
import cn.domoe.naivenet.User.User;
import cn.domoe.naivenet.User.UserManager;
import io.netty.channel.Channel;

/**
 * 	客户端此前发生断线，请求进行连接恢复
 * */
class ControllerRecover extends NaiveNetController {

	public ControllerRecover() {
		super("recover");
		// TODO Auto-generated constructor stub
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		//使用网络恢复功能当前用户不能是已经Auth的状态
		if(msg.user.isLog()) {
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.RECOVER_FAILED,false);
			return res;
		}
		
		String token = new String(msg.param);
		User user = UserManager.this.logedUsers.get(token);
		if(user == null) { //说明没有用户信息或者用户信息错误 无法进行网络恢复
			NaiveNetResponseData res = new NaiveNetResponseData(msg,CodeMap.RECOVER_FAILED,false);
			return res;
		}
		//存在则恢复
		Channel old = user.channel;
		//先替换当前的句柄映射
		user.replaceNetwork(msg.user); 					//将新的Channel替换到旧的User的channel
		channelAndUserMap.put(msg.user.channel, user);	//将新的Channel与旧的User进行关联
		channelAndUserMap.remove(old);			//移除旧的Channel与旧的User的关联
		unlogUsers.remove(msg.user);					//未登录列表中移除新的User
		//关闭掉旧的UserChannel
		try {
			old.close().sync();
		} catch (InterruptedException e) {
			
		}
		return null;
	}
	
}