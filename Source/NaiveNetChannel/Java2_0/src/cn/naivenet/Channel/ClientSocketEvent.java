package cn.naivenet.Channel;

/**
 * 	客户端句柄回调事件接口
 * */
public interface ClientSocketEvent {
	/**
	 * 	当发生某事后的事件回调
	 * 	@param handler 引发事件的ClientHandler对象
	 * 	@param data 如果该事件伴随了数据，则在data中，否则data为null
	 * */
	public void on(ClientHandler handler,byte[] data);
}
