package cn.naivenet.Channel;

public interface NaiveNetOnResponse {

	/**
	 * 	当请求发生响应则触发该回调。
	 *  @param code 回应的代码 请参阅全局CODE说明，data 回应的正文，字节集类型，使用 new String(data, "utf-8") 可转换为字符串
	 * */
	public void OnComplete(int code,byte[] data);
	
}
