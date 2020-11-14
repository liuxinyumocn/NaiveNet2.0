package cn.domoe.naivenet.User;

import cn.domoe.naivenet.User.NaiveNetMessage;

public abstract class NaiveNetController {
	public String name ;
	public NaiveNetController(String name) {
		this.name = name;
	}
	public abstract NaiveNetResponseData onRequest(NaiveNetMessage msg);
}
