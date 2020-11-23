package cn.domoe.naivenet.Config;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class NaiveNetConfig2 {

	private String root = "";
	
	/**
	 * 提供配置根目录
	 * @throws Exception 
	 * */
	public NaiveNetConfig2(String root) throws Exception {
		this.root = root;
		baseConf = new HashMap<>();
		channelIDAndInfo = new HashMap<>();
		channelNameAndInfo = new HashMap<>();
		
		this.init();
	}
	
	/**
	 * 在开发环境中，将采用开发目录为根目录
	 * 在生产环境中，将采用与JAR文件同级目录为根目录
	 * @throws Exception 
	 * */
	public NaiveNetConfig2() throws Exception {
		String src = NaiveNetConfig.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		int pos = src.lastIndexOf("/");
		root = src.substring(0,pos+1);

		baseConf = new HashMap<>();
		channelIDAndInfo = new HashMap<>();
		channelNameAndInfo = new HashMap<>();
		
		this.init();
	}
	
	private Map<String,Value> baseConf;
	private Map<Integer,ChannelInfo> channelIDAndInfo;
	private Map<String,ChannelInfo> channelNameAndInfo;
	private int channelID = 1;
	
	private void init() throws Exception {
		this.loadBaseConf();
		if(!this.loadTempChannelConf()) {
			//如果临时配置文件没有加载成功，则使用标准配置文件
			this.loadChannelConf();
		}
		
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
	
	private boolean loadTempChannelConf() {
		File file = new File("NaiveNetChannel.temp");
		if(!file.exists()) {
			file = new File(this.root + "NaiveNetChannel.json");
			if(file.exists()) {
				//开始先加载缓存配置
				try {
					FileInputStream fin = new FileInputStream(file.getAbsolutePath());
					byte[] bs = new byte[fin.available()];
					fin.read(bs);
					String content = new String(bs,"utf-8");
					JSONArray arr = new JSONArray(content);
					//将缓存全部填入配置文件中
					for(int i = 0;i<arr.length();i++) {
						JSONObject item = arr.getJSONObject(i);
						ChannelInfo info = new ChannelInfo(item);
						channelIDAndInfo.put(new Integer(item.getInt("id")), info);
						channelNameAndInfo.put(item.getString("name"), info);
					}
					fin.close();
					return true;
				}catch(Exception e) {
					//发生错误 不做任何处理
					//System.out.println("没有缓存数据将默认初始化。");
				}
			}
		}
		
		return false;
	}
	
	/**
	 * 	添加ChannelInfo
	 * */
	private void putChannelInfo(ChannelInfo info) {
		//先判断是否已经存在对应Name的Channel
		ChannelInfo old = channelNameAndInfo.get(info.getName());
		if(old == null) {
			//不存在 寻找下一个可用ID
			while(true) {
				Integer nextid = channelID++;
				if(!channelIDAndInfo.containsKey(nextid)) {
					channelIDAndInfo.put(nextid, info);
					info.id = nextid;
					return;
				}
			}
		}else {
			//存在则替换
			info.id = old.id;
			Integer oldid = old.id;
			channelIDAndInfo.replace(oldid, info);
			channelNameAndInfo.replace(old.getName(), info);
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
			//加载配置项
			JSONArray jsonArr = new JSONArray(content);
			for(int i = 0;i<jsonArr.length();i++) {
				ChannelInfo info = new ChannelInfo(jsonArr.getJSONObject(i));
				putChannelInfo(info);
			}
		}catch(Exception e) {
			
		}finally{
			fin.close();
		}
		
	}
	
	/**
	 * 	覆盖原来的配置
	 * */
	public void setChannelConf() {
		
	}
	
	/**
	 * 	获取当前Channel配置的JSON字符串
	 * */
	public void getChannelConfStr() {
		
	}
	
}
