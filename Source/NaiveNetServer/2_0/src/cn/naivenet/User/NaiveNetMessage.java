package cn.naivenet.User;

public abstract class NaiveNetMessage {

	public byte control;
	public byte msgid;
	public int channelid;
	public byte[] channeldata; //频道数据格式
	public String controller;
	public byte[] param; 	//参数
	public User user;
	public byte code;
	//public Channel channelOrigin;
	
	public byte[] data;	//原始数据
}
