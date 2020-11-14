package cn.domoe.naivenet.Channel;

public interface NaiveNetEvent {
	
	/**
	 * 	某个事件的触发
	 * 	@param user 触发源user实例，data 可能携带的数据 通常情况下为 null 不需要使用
	 * */
	public void on(User user,byte[] data) ;
	
}
