package cn.domoe.naivenet.Channel;

public class NaiveNetResponse {

	public NaiveNetMessage msg;
	public byte[] content = null;
	
	public NaiveNetResponse(NaiveNetMessage msg) {
		this.msg = msg;
	}

	/**
	 * 	设置回应的正文
	 *  @param content 回应的正文字节集 推荐填入字符串
	 * */
	public void setContent(byte[] content) {
		this.content = content;
	}
	
	/**
	 * 	设置回应的正文
	 *  @param content 回应的字符串
	 * */
	public void setContent(String content) {
		this.setContent(content.getBytes());
	}
	
}
