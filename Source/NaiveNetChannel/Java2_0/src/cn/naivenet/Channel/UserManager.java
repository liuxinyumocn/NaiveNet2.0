package cn.naivenet.Channel;

import java.util.concurrent.ConcurrentLinkedDeque;

class UserManager {

	private ConcurrentLinkedDeque<User> userList;
	
	public UserManager() {
		userList = new ConcurrentLinkedDeque<>();
	}

	/**
	 * 	创建新用户
	 * 	@param handler 用户的通信句柄
	 * */
	public void createUser(ClientHandler handler) {
		User user = new User(handler,this);
		userList.add(user);
		if(this.onNewUser != null)
			this.onNewUser.on(user, null);
	}

	private NaiveNetEvent onNewUser;
	public void setOnNewUserListener(NaiveNetEvent e) {
		onNewUser = e;
	}

	/**
	 * 	该接口开发者请勿调用
	 * 	用户退出且不可恢复或NS与Channel发生断线
	 * */
	public void _onUserQuit(User user) {
		userList.remove(user);
	}

}
