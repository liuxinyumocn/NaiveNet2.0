package cn.naivenet.User;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;

import cn.naivenet.NaiveNetServerHandler;

/**
 * 	申请进入管理员模式
 * */
class ControllerAdmin extends NaiveNetController {

	NaiveNetServerHandler naivenetServerHandler;
	
	public ControllerAdmin(NaiveNetServerHandler naivenetServerHandler) {
		super("admin");
		this.naivenetServerHandler = naivenetServerHandler;
	}

	@Override
	public NaiveNetResponseData onRequest(NaiveNetMessage msg) {
		/*
		 * 	管理员模式申请规则：
		 * 	需要发送一个JSON字符串，字符串内包括：
		 * 	timestamp（毫秒）、nonceStr（不小于16位的随机字符串）、sign（使用MD5完成对  timestamp + nonceStr + secret 的加密）
		 * 	提供的时间戳不能与服务器的系统时间相差超过30秒
		 * 	1分钟内相同的 nonceStr 字符串不能重复使用
		 * */
		long timestamp;
		String nonceStr;
		String sign;
		try {
			JSONObject json = new JSONObject(new String(msg.param,"utf-8"));
			if(!json.has("timestamp") || !json.has("nonceStr") || !json.has("sign"))
				throw new Exception();
			timestamp = json.getLong("timestamp");
			nonceStr = json.getString("nonceStr");
			sign = json.getString("sign");
			
		} catch (Exception e) {
			return new NaiveNetResponseData(msg,CodeMap.DATA_FORMAT_ERROR,false);
		}
		
		if(Math.abs(System.currentTimeMillis() - timestamp) > 1000 * 30 || naivenetServerHandler.config.getConf("SECRET").getStr().length() == 0) {
			return new NaiveNetResponseData(msg,CodeMap.PERMISSION_DENIED,false);
		}
		if(nonceStr.length() < 6) {
			return new NaiveNetResponseData(msg,CodeMap.PERMISSION_DENIED,false);
		}
		String key = timestamp + nonceStr + naivenetServerHandler.config.getConf("SECRET").getStr();
		String sign2 = MD5(key);
		if(sign2.equals(sign)) { //验证成功注册相关的Controller
			User.AddBox(naivenetServerHandler.getAdminBox());
			msg.user.auth();
			return new NaiveNetResponseData(msg,CodeMap.OK,true);
		}else {
			return new NaiveNetResponseData(msg,CodeMap.PERMISSION_DENIED,false);
		}
	}
	
	/**
	 * 	MD5签名算法
	 * 	@param content 原文内容
	 * 	@return String 签名内容
	 * */
	private static String MD5(String content) {
		byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(
            		content.getBytes());
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = "0" + md5code;
        }
        return md5code;
	}
}
