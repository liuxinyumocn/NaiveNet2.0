package cn.naivenet.Config;

import java.util.Collection;

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

	public JSONObject getJSON() throws JSONException {
		JSONObject ob = new JSONObject();
		ob.put("name", this.getName());
		ob.put("ip", this.getIP());
		ob.put("port", this.getPort());
		ob.put("token", this.getToken());
		ob.put("auth", this.getAuth());
		
		return ob;
	}
}
