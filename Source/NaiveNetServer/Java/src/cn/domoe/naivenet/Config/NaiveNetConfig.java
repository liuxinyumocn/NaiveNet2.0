package cn.domoe.naivenet.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
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
	
	private void init() throws Exception {
		this.loadBaseConf();
		if(!this.loadTempChannelInfo())
			this.loadChannelConf();
	}
	
	/**
	 * 	将Channel保存到临时的配置文件中
	 * @throws IOException 
	 * */
	private void saveChannelInfo() throws IOException {
		byte[] temp = this.getChannelConf().getBytes();
		File file = new File("NaiveNetChannel.temp");
		if(!file.exists()) {
			if(!file.exists()) {
				file.createNewFile();
			}
		}
		FileOutputStream fout = new FileOutputStream(file.getAbsolutePath());
		fout.write(temp);
		fout.flush();
		fout.close();
	}

	private boolean loadTempChannelInfo() {
		File file = new File("NaiveNetChannel.temp");
		if(!file.exists()) {
			file = new File(this.root + "NaiveNetChannel.temp");
			if(file.exists()) {
				//开始先加载缓存配置
				try {
					FileInputStream fin = new FileInputStream(file.getAbsolutePath());
					byte[] bs = new byte[fin.available()];
					fin.read(bs);
					fin.close();
					String content = new String(bs,"utf-8");
					this.setChannelInfo(content);
//					JSONArray arr = new JSONArray(content);
//					//将缓存全部填入配置文件中
//					for(int i = 0;i<arr.length();i++) {
//						JSONObject item = arr.getJSONObject(i);
//						ChannelInfo info = new ChannelInfo(item);
//						
//					}
					return true;
				}catch(Exception e) {
					//发生错误 不做任何处理
					//System.out.println("没有缓存数据将默认初始化。");
				}
			}
		}
		
		return false;
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
			baseConf.put("SECRET",new Value(json.getString("SECRET")));
			
		}catch(Exception e) {
			throw new Exception("Configuration parsing failed");
		}finally {
			fin.close();
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
		fin.close();
		String content = new String(bs,"utf-8");
		this.setChannelInfo(content);
	}
	
	public void setChannelInfo(String content) throws Exception {
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
		this.saveChannelInfo();
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
	
	/**
	 * 	获取当前配置的JSON字符串
	 * */
	public String getChannelConf() {
		JSONArray arr = new JSONArray();
		for(int i = 0 ; i < this.channelList.size();i++) {
			ChannelInfo info = this.channelList.get(i);
			if(info.use) {
				try {
					arr.put(info.getJSON());
				} catch (JSONException e) {
					return "[]";
				}
			}
		}
		return arr.toString();
	}
}