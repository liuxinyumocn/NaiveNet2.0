package cn.naivenet.Channel;

public class CodeMap {

	public static final byte[] NOT_FOUNTD_CONTROLLER = "400".getBytes(); //未发现控制器
	public static final byte[] PERMISSION_DENIED = "500".getBytes(); //权限不足
	public static final byte[] OK = "200".getBytes();
	public static final byte[] NOT_FOUND_CHANNEL = "401".getBytes(); //未发现频道
	public static final byte[] CANNOT_BE_ESTABLISHED = "402".getBytes(); //无法与频道建立连接
	public static final byte[] CHANNEL_REFUSE_CONNECT = "403".getBytes(); //远程主机拒绝建立连接或没有正确回应
	public static final byte[] CHANNEL_NOT_ESTABLISHED_WITH_SERVER = "404".getBytes(); //远程主机拒绝建立连接或没有正确回应
	public static final byte[] DATA_FORMAT_ERROR = "501".getBytes(); //数据格式异常
	public static final byte[] UNKNOW_ERROR = "502".getBytes(); //未知错误
	public static final byte[] RECOVER_FAILED = "503".getBytes();	//恢复失败
	
}
