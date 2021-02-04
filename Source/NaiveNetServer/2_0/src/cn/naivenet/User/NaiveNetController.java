package cn.naivenet.User;


public abstract class NaiveNetController {
	public String name ;
	public NaiveNetController(String name) {
		this.name = name;
	}
	public abstract NaiveNetResponseData onRequest(NaiveNetMessage msg);
}
