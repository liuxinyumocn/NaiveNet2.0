package cn.domoe.naivenet.Config;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class NaiveNetConfig {

	private String root = "";
	
	/**
	 * 提供配置根目录
	 * @throws Exception 
	 * */
	public NaiveNetConfig(String root) throws Exception {
		this.root = root;
		baseConf = new HashMap<>();
		channelList = new ArrayList<>();
		
		this.init();
	}
	
	/**
	 * 在开发环境中，将采用开发目录为根目录
	 * 在生产环境中，将采用与JAR文件同级目录为根目录
	 * @throws Exception 
	 * */
	public NaiveNetConfig() throws Exception {
		
		String src = NaiveNetConfig.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		int pos = src.lastIndexOf("/");
		root = src.substring(0,pos+1);
		
		baseConf = new HashMap<>();
		channelList = new ArrayList<>();
		
		this.init();
	}
	
	private Map<String,Value> baseConf;
	private List<ChannelInfo> channelList;
	
	public void init() throws Exception {
		this.loadBaseConf();
		this.loadChannelConf();
	}
	
	private void loadBaseConf() throws Exception {
		File file = new File("NaiveNetConfig.json");
		if(!file.exists()) {
			file = new File(this.root + "NaiveNetConfig.json");
			if(!file.exists()) {
				throw new Exception("Not found NaiveNetConfig.json file");
			}
		}
		
		FileInputStream fin = new FileInputStream(file.getAbsolutePath());
		byte[] bs = new byte[fin.available()];
		fin.read(bs);
		String content = new String(bs,"utf-8");
		try {
			JSONObject json = new JSONObject(content);

			baseConf.put("SERVER_PORT",new Value(json.getInt("SERVER_PORT")));
			baseConf.put("SSL_JKS_FILEPATH",new Value(json.getString("SSL_JKS_FILEPATH")));
			baseConf.put("SSL_PASSWORD_FILEPATH",new Value(json.getString("SSL_PASSWORD_FILEPATH")));
			baseConf.put("USER_BREAK_TIMEOUT",new Value(json.getInt("USER_BREAK_TIMEOUT")));
			baseConf.put("USER_QUIT_TIMEOUT",new Value(json.getInt("USER_QUIT_TIMEOUT")));
			baseConf.put("USER_AUTH_TIMEOUT",new Value(json.getInt("USER_AUTH_TIMEOUT")));
			
		}catch(Exception e) {
			throw new Exception("Configuration parsing failed");
		}
		
	}
	
	private void loadChannelConf() throws Exception {
		File file = new File("NaiveNetChannel.json");
		if(!file.exists()) {
			file = new File(this.root + "NaiveNetChannel.json");
			if(!file.exists()) {
				throw new Exception("Not found NaiveNetChannel.json file");
			}
		}
		
		FileInputStream fin = new FileInputStream(file.getAbsolutePath());
		byte[] bs = new byte[fin.available()];
		fin.read(bs);
		String content = new String(bs,"utf-8");
		try {
			JSONArray jsonArr = new JSONArray(content);
			//频道加载采取同一个NS，只增不删原则，即此前的频道若删除，ID保留，但不再提供该频道信息。ID为在list中的索引+1
			ChannelInfo[] channels = new ChannelInfo[jsonArr.length()];
			for(int i = 0;i<channels.length;i++) {
				channels[i] = new ChannelInfo(jsonArr.getJSONObject(i));
			}
			List<ChannelInfo> newList = new ArrayList<>();
			for(int i = 0; i< channelList.size();i++) { //先将原有的填好
				boolean had = false;
				ChannelInfo n = channelList.get(i);
				for(int j =0;j<channels.length;j++) {
					if(channels[j].getName().equals(n.getName())) { //找到了
						had = true;
						channels[j].id = n.id;
						newList.add(channels[j]);//存入新的
						break;
					}
				}
				if(!had) { //没找到 位置仍然要填上
					n.use = false;
					newList.add(n);
				}
			}
			
			List<ChannelInfo> add =new ArrayList<>();
			//追加剩余新增的频道信息
			for(int i = 0;i<channels.length;i++) {
				ChannelInfo n = channels[i];
				boolean find= false;
				for(int j =0;j<newList.size();j++) {
					if(newList.get(j).getName().equals(n.getName())) {
						find = true;
						break;
					}
				}
				if(!find) {
					add.add(n);
				}
			}
			int index = newList.size()+1;
			for(int i = 0;i<add.size();i++) {
				ChannelInfo n = add.get(i);
				n.id = index++;
				newList.add(n);
			}
			//替换
			channelList = newList;
		}catch(Exception e) {
			throw new Exception("NaiveNetChannel.json cannot be parsed");
		}
	}
	
	public ChannelInfo getChannelInfo(String channelName) {
		for(int i = 0;i<channelList.size();i++) {
			ChannelInfo n = channelList.get(i);
			if(n.getName().equals(channelName)) {
				if(n.use)
					return n;
				return null;
			}	
		}
		return null;
	}
	
	public ChannelInfo getChannel(int id) {
		if(id <= 0 || id > channelList.size())
			return null;
		return channelList.get(id-1);
	}
	
	public Value getConf(String key) {
		return baseConf.get(key);
	}
}
