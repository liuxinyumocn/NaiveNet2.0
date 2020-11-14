package cn.domoe.naivenet.Config;

import org.json.JSONException;
import org.json.JSONObject;

public class ChannelInfo {
	public boolean use;
	private String name;
	private String ip;
	private int port;
	private String token;
	private boolean auth;
	public int id = -1;
	public ChannelInfo(JSONObject cl) throws JSONException {
		use = true;
		name = cl.getString("name");
		ip = cl.getString("ip");
		port = cl.getInt("port");
		token = cl.getString("token");
		auth = cl.getBoolean("auth");
	}
	
	public int getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getIP() {
		return ip;
	}
	public String getToken() {
		return token;
	}
	public boolean getAuth() {
		return auth;
	}
	public int getPort() {
		return port;
	}
}
