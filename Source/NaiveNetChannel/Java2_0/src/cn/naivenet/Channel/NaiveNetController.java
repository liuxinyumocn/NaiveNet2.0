package cn.naivenet.Channel;


public abstract class NaiveNetController {
	public String name ;
	
	/**
	 * 	Controller父类
	 *  @param name 填入Controller的名称，供客户端访问，该name将对应客户端的 request({  controller: xxxx  }) xxxx部分
	 * */
	public NaiveNetController(String name) {
		this.name = name;
	}
	
	/**
	 * 	客户端请求该Controller触发的事件函数定义
	 * 	@param msg 中携带了相关请求参数，常用的包括：<br>
	 * 	msg.controller 请求的控制器名称<br>
	 * 	msg.param 请求所携带的参数字符集类型 使用 new String( msg.param , "utf-8" ) 可转换为字符串<br>
	 *  msg.user 请求源 user 实例，可用来判断请求者<br>
	 *  res = msg.getResponseHandler() 获取用于回应的句柄，可填写回应的数据<br>
	 *  return res; 客户端将获得回应<br>
	 *  其他参数通常不需要使用，也不要去访问以及修改 
	 * */
	public abstract NaiveNetResponse onRequest(NaiveNetMessage msg);
}
