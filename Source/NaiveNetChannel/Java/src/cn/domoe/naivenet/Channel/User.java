package cn.domoe.naivenet.Channel;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.channel.Channel;

/**
 *  NaiveNet User类 每一个User 实例将对应一个NaiveNetClient客户端句柄
 * */
public class User {

	private Channel socketChannel;
	private UserManager userManager;
	
	private long start_timestamp = 0;
	private long end_timestamp = 0;
	private long lastmsg_timestamp = 0;
	
	private NaiveNetHandler naiveNetHandler;
	
	private NaiveNetEvent onQuit = null;
	private NaiveNetEvent onBreak = null;
	private NaiveNetEvent onRecover = null;
	
	public User(Channel sc,UserManager manager) {
		this.socketChannel = sc;
		this.userManager = manager;
		dataqueue = new ConcurrentLinkedQueue<>();
		naiveNetHandler = new AuthHandler();
	
	}

	private static boolean INIT = false;

	/**
	 * 用户全局初始化
	 * */
	public static void INIT() {
		if(!User.INIT)
			User.INIT = true;
		NaiveNetBox box = new NaiveNetBox();
		box.addController(new CtrlOnBreak());
		box.addController(new CtrlOnRecover());
		box.addController(new CtrlOnQuit());
		User.AddNSBox(box);
	}
	
	/**
	 * 	获得该用户的 {@link Channel} 句柄，请注意，如果你不是NaiveNet源码的维护人员，
	 *  通常情况下不需要获取该句柄，甚至更不应该借助该句柄进行任何的I/O操作，使用该句柄将可能导
	 *  致NaiveNet组件工作异常
	 * */
	public Channel getSocketChannel() {
		return this.socketChannel;
	}

	private byte[] cacheBuffer = null;
	
	/**
	 * 	获得该用户的断包缓存数据，请注意，如果你不是NaiveNet源码的维护人员，
	 *  通常情况下不需要获取该句柄，甚至更不应该借助该句柄进行任何的I/O操作，使用该句柄将可能导
	 *  致NaiveNet组件工作异常
	 * */
	public byte[] getCacheBuffer() {
		return cacheBuffer;
	}
	

	/**
	 * 	设置该用户的断包缓存数据，请注意，如果你不是NaiveNet源码的维护人员，
	 *  通常情况下不需要获取该句柄，甚至更不应该借助该句柄进行任何的I/O操作，使用该句柄将可能导
	 *  致NaiveNet组件工作异常
	 * */
	public void setCacheBuffer(byte[] cacheBuffer) {
		this.cacheBuffer = cacheBuffer;
	}
	
	private ConcurrentLinkedQueue<byte[]> dataqueue;
	

	/**
	 * 	向该用户发起数据投递，请注意，如果你不是NaiveNet源码的维护人员，
	 *  通常情况下不需要调用该方法，请使用 user.request()，使用该句柄将可能导
	 *  致NaiveNet组件工作异常
	 * */
	public void push(byte[] data) {
		dataqueue.add(data);
		if(doing)
			return;
		NaiveNetThreadManager.push(this);
	}
	private boolean doing = false;

	/**
	 * 	触发事件队列，请注意，如果你不是NaiveNet源码的维护人员，
	 *  通常情况下不需要调用该方法，请使用 user.request()，使用该句柄将可能导
	 *  致NaiveNet组件工作异常
	 * */
	public void doit() {
		doing = true;
		while(true) {
			byte[] data = dataqueue.poll();
			if(data == null) {
				doing = false;
				return;
			}
			naiveNetHandler.doit(data);
		}
		
	}
	
	/**
	 * 用于身份验证的数据处理Handler
	 * */
	class AuthHandler implements NaiveNetHandler{

		@Override
		public void doit(byte[] data) {
			//验证身份是否合法
			byte[] _token = userManager.getToken();
			if(data.length != _token.length) {
				//断开该链接
				userManager.unlogUserQuit(User.this);
				return;
			}
			
			for(int i = 0;i<data.length;i++) {
				if(data[i] != _token[i]) {
					//身份非法
					//断开该链接
					userManager.unlogUserQuit(User.this);
					return;
				}
			}
			//System.out.println(new String(data));
			//身份合法
			send("NAIVENETCHANNEL CODE[OK]".getBytes());
			this.success();
		}
		
		void success() {
			//移除unlog
			userManager.log(User.this);
			naiveNetHandler = new DealHandler();
			userManager.onNewUser(User.this);
		}
		
	}
	
	/**
	 * 处理已经完成身份验证的Handler
	 * */
	class DealHandler implements NaiveNetHandler{

		@Override
		public void doit(byte[] data) {
			parseMsg(data);
		}
		
	}
	
	/**
	 * 处理消息数据
	 * */
	private void parseMsg(byte[] data) {
		this.lastmsg_timestamp = System.currentTimeMillis();
		NaiveNetUserMessage msg = new NaiveNetUserMessage(data,this);
		
		if(msg.control == 1) {	//请求类型
			if(msg.channelid == 0) { //来自NS的请求
				this.dealNSToNC(msg);	//当前版本对于NS的请求暂不需要进行回复
			}else {	//来自客户端的请求
				NaiveNetResponseData res = this.dealCToNC(msg);
				this.response(res);
			}
		}else if(msg.control == 0 || msg.control == 3) { //回复
			this.dealResponse(msg);
			this.checkWTList();
		}
	}
	
	private void dealResponse(NaiveNetUserMessage msg) {
		NaiveNetRequestData _origin_req = this.temp_msg_list.get((int)msg.msgid);
		if(_origin_req == null)
			return;
		if(_origin_req.finished == true)
			return;
		_origin_req.finished = true;
		
		if(_origin_req.onResponse != null) {
			int code = 200;
			if(msg.control == 3) {
				try {
					code = Integer.parseInt(new String(msg.param));
				}catch(Exception e) {
					code = 502;
				}
			}
			_origin_req.onResponse.OnComplete(code, msg.param);
		}
	}
	
	//建立Module 与 Controller机制
	private List<NaiveNetBox> boxs = new ArrayList<>();
	
	/**
	 *  为当前的user实例添加 {@link NaiveNetBox}
	 *  @param box 提供 {@link NaiveNetBox} 实例
	 *  该 {@link NaiveNetBox} 中的Controller将仅对该user生效
	 *  若 {@link NaiveNetBox} 事件中有同名 Controller 则仅触发先添加的 Controller。
	 *  <p> 为user实例添加的 Controller 优先级比 User类 的优先级高
	 * */
	public void addBox(NaiveNetBox box) {
		boxs.add(box);
	}
	
	/**
	 *  为当前的user实例移除 {@link NaiveNetBox}
	 *  @param box 提供 {@link NaiveNetBox} 实例
	 *  如果参数 box 存在则移除，不存在则无事发生。
	 * */
	public void removeBox(NaiveNetBox box) {
		boxs.remove(box);
	}
	private static List<NaiveNetBox> BOXs = new ArrayList<>();
	
	/**
	 *  为所有的User添加 {@link NaiveNetBox}
	 *  @param box 提供 {@link NaiveNetBox} 实例
	 *  通常该方法应在NaiveNetChannel系统启动时调用，不应该出现在 OnNewUser 事件之后触发
	 * */
	public static void AddBox(NaiveNetBox box) {
		BOXs.add(box);
	}
	
	/**
	 *  为所有的User移除 {@link NaiveNetBox}
	 *  @param box 提供 {@link NaiveNetBox} 实例
	 *  通常该方法应在NaiveNetChannel系统启动时调用，不应该出现在 OnNewUser 事件之后触发
	 * */
	public static void RemoveBox(NaiveNetBox mod) {
		BOXs.remove(mod);
	}
	private static List<NaiveNetBox> NSBOXs = new ArrayList<>();
	private static void AddNSBox(NaiveNetBox mod) {
		NSBOXs.add(mod);
	}
	
	private NaiveNetResponseData dealCToNC(NaiveNetUserMessage msg) {
		NaiveNetResponseData res = null;
		for(int i = 0;i<this.boxs.size();i++) {
			res = this.boxs.get(i).deal(msg);
			if(res != null)
				return res;
		}
		for(int i = 0;i<User.BOXs.size();i++) {
			res = User.BOXs.get(i).deal(msg);
			if(res != null) 
				return res;
		}
		res = new NaiveNetResponseData(msg,CodeMap.NOT_FOUNTD_CONTROLLER,false);
		return res;
	}
	
	private void dealNSToNC(NaiveNetUserMessage msg) {
		NaiveNetResponseData res = null;
		for(int i = 0;i<User.NSBOXs.size();i++) {
			res = User.NSBOXs.get(i).deal(msg);
			if(res != null)
				return;
		}
	}
	
	private static class CtrlOnBreak extends NaiveNetController {

		public CtrlOnBreak() {
			super("onbreak");
			// TODO Auto-generated constructor stub
		}

		@Override
		public NaiveNetResponse onRequest(NaiveNetMessage msg) {
			msg.user.onBreak();
			return null;
		}
		
	}

	private static class CtrlOnRecover extends NaiveNetController {

		public CtrlOnRecover() {
			super("onrecover");
			// TODO Auto-generated constructor stub
		}

		@Override
		public NaiveNetResponse onRequest(NaiveNetMessage msg) {
			msg.user.onRecover();
			return null;
		}
		
	}

	private static class CtrlOnQuit extends NaiveNetController {

		public CtrlOnQuit() {
			super("onquit");
			// TODO Auto-generated constructor stub
		}

		@Override
		public NaiveNetResponse onRequest(NaiveNetMessage msg) {
			//System.out.println("quit 3");
			msg.user.onQuit();
			return null;
		}
		
	}
	
	
	
	
	//发送数据给NS
	private void send(byte[] data) {
		//System.out.println("开始发送数据");
		//Log.print(data);
		this.userManager.pushUserMessage(this.socketChannel,data);
	}
	
	/**
	 * 进行回复
	 * */
	private void response(NaiveNetResponseData data) {
		this.send(data.genData());
	}

	/**
	 * 设置当前用户发生退出的监听器，退出代表用户离开当前NaiveNetChannel，并不代表退出应用<br>
	 * @param e 是 {@link NaiveNetEvent} 的实现类
	 * <p> 新添加的监听器会覆盖旧的监听器
	 * */
	public void setOnQuitListener(NaiveNetEvent e) {
		this.onQuit = e;
	}
	

	/**
	 * 设置当前用户发生断线的监听器，断线代表用户客户端发生网络异常，有可能在数秒或数分钟后恢复<br>
	 * @param e 是 {@link NaiveNetEvent} 的实现类
	 * <p> 新添加的监听器会覆盖旧的监听器
	 * */
	public void setOnBreakListener(NaiveNetEvent e) {
		this.onBreak = e;
	}
	
	/**
	 * 设置当前用户发生网络恢复的监听器，恢复代表用户断线后又重新恢复的回调事件<br>
	 * @param e 是 {@link NaiveNetEvent} 的实现类
	 * <p> 新添加的监听器会覆盖旧的监听器
	 * */
	public void setOnRecoverListener(NaiveNetEvent e) {
		this.onRecover = e;
	}
	
	/**
	 * 开发者请勿调用该方法
	 * */
	public void onQuit() {
		if(this.onQuit != null)
			this.onQuit.on(this, null);
		
		this.userManager.userQuit(this);
	}

	/**
	 * 开发者请勿调用该方法
	 * */
	private void onBreak() {
		if(this.onBreak != null)
			this.onBreak.on(this, null);
	}
	
	/**
	 * 开发者请勿调用该方法
	 * */
	private void onRecover() {
		if(this.onRecover != null)
			this.onRecover.on(this, null);
	}
	
	/**
	 * 	向客户端发起请求<br>
	 *  @param controller 代表客户端中注册的Controller名称 ， data 为携带的参数（以字节集发送） ，onResponse 当客户端接收到请求发生回调的事件，应为{@link NaiveNetOnResponse}的实现
	 * */
	public void request(String controller,byte[] data,NaiveNetOnResponse onResponse) {
		NaiveNetRequestData req = new NaiveNetRequestData(
					NaiveNetRequestData.TO_NAIVENET_CLIENT,
					controller,
					data,
					onResponse
				);
		this.genTempMsgId(req);
	}
	
	/**
	 * 	向客户端发起请求<br>
	 *  @param controller 代表客户端中注册的Controller名称 ， data 为携带的参数（以字符串发送） ，onResponse 当客户端接收到请求发生回调的事件，应为{@link NaiveNetOnResponse}的实现 也可以填入 null 代表忽略回应
	 * */
	public void request(String controller,String data,NaiveNetOnResponse onResponse) {
		this.request(controller, data.getBytes(), onResponse);
	}
	
	/**
	 * 	基于用户的临时消息ID生成器
	 * */
	private ConcurrentHashMap<Integer,NaiveNetRequestData> temp_msg_list = new ConcurrentHashMap<>();
	private ConcurrentLinkedQueue<NaiveNetRequestData> temp_msg_wt_list = new ConcurrentLinkedQueue<>();
	private int temp_msg_id = 0;
	private void genTempMsgId(NaiveNetRequestData data) {
		boolean f = false; //寻找了 126个 均无法填入 则放入缓存队列
		boolean n = false;
		Integer id = 0;
		for(int i = 0;i < 126;i++) {
			temp_msg_id++;
			if(temp_msg_id > 125)
				temp_msg_id = 0;
			NaiveNetRequestData old = this.temp_msg_list.get(temp_msg_id);
			if(old == null) {
				f = true;	//没找到
				n = true;	//是新的
				id = temp_msg_id;
				break;
			}
			if(old.finished == true) {
				f = true;
				id = temp_msg_id;
				break;
			}
		}
		
		if(!f) {
			this.temp_msg_wt_list.add(data);
			return;
		}
		data.msgid = (byte)(int)id;
		if(n)
			this.temp_msg_list.put(id, data);
		else
			this.temp_msg_list.replace(id, data);
		//System.out.println(data.msgid);
		this.send(data.genData());
	}
	
	private void checkWTList() {
		NaiveNetRequestData data = this.temp_msg_wt_list.poll();
		if(data != null) {
			this.genTempMsgId(data);
		}
	}
	
	/**
	 * 	为该用户授权，授权的用户才可以访问所有的NaiveNetChannel
	 *  @param repsponse {@link NaiveNetOnResponse} 的实现，当授权成功后的回调，可以填入 null
	 * */
	public void auth(NaiveNetOnResponse response) {
		NaiveNetRequestData req = new NaiveNetRequestData(
				NaiveNetRequestData.TO_NAIVENET_SERVER,
				"auth",
				new byte[0] ,
				response
			);
		this.genTempMsgId(req);
	}
	
	/**
	 * 	获取用户的连接信息
	 * 	当前版本可获得 需要使用 NaiveNetLinkInfo 解包获得具体信息以JSON字符串的字节集返回
	 * 	得到的回调中需要 
	 * 	String info_json = new String( data );
	 * 	info_json 中包括的字段有：
	 * 		ip String 用户的IP地址
	 * 		starttimestamp long 该用户首次连接时间戳
	 * */
	public void getLinkInfo(NaiveNetOnResponse response) {
		NaiveNetRequestData req = new NaiveNetRequestData(
				NaiveNetRequestData.TO_NAIVENET_SERVER,
				"getlinkinfo",
				new byte[0] ,
				response
			);
		this.genTempMsgId(req);
	}
	
	/**
	 * 	让NS断开与当前NC的会话
	 * 	Client侧无感知，当Client需要进入时会再次发生 onNewUser 事件回调重新建立连接
	 * */
	public void quitChannel(NaiveNetOnResponse response) {
		NaiveNetRequestData req = new NaiveNetRequestData(
				NaiveNetRequestData.TO_NAIVENET_SERVER,
				"quitchannel",
				new byte[0] ,
				response
			);
		this.genTempMsgId(req);
		
	}
	
	/**
	 * 	让NS强制关闭用户会话
	 * 	注意该动作将导致Client侧网络直接发生断开，并且无法恢复，所有与NS相关的NC也都会退出该用户的句柄
	 * */
	public void close(NaiveNetOnResponse response) {
		NaiveNetRequestData req = new NaiveNetRequestData(
				NaiveNetRequestData.TO_NAIVENET_SERVER,
				"close",
				new byte[0] ,
				response
			);
		this.genTempMsgId(req);
	}
	
	
	/**
	 * 	获取用户的SESSION 特定 KEY 的 VALUE
	 *  @param key 想要获得的KEY ，response 获取后的回调 {@link NaiveNetOnResponse} 的实现
	 * 	当 KEY 不存在时返回  response 长度为0的byte[]
	 * */
	public void getSession(String key,NaiveNetOnResponse response) {
		NaiveNetRequestData req = new NaiveNetRequestData(
				NaiveNetRequestData.TO_NAIVENET_SERVER,
				"getsession",
				key.getBytes(),
				response
			);
		this.genTempMsgId(req);
	}
	
	/**
	 * 	设置用户的SESSION 特定 KEY 的 VALUE
	 *  @param key 想要获得的KEY ，value 想要设置的内容 仅支持 byte[] ，response 获取后的回调 {@link NaiveNetOnResponse} 的实现 可以输入 null 代表忽略回应
	 * */
	public void setSession(String key,byte[] value,NaiveNetOnResponse response) {
		byte[] key_content = key.getBytes();
		byte[] key_header = NaiveNetRequestData.calNumber(key_content);
		byte[] value_header = NaiveNetRequestData.calNumber(value);
		byte[] _data = new byte[key_content.length + key_header.length + value_header.length + value.length];
		int index = 0;
		System.arraycopy(key_header, 0, _data, 0, key_header.length);
		index += key_header.length;
		System.arraycopy(key_content, 0, _data, index, key_content.length);
		index += key_content.length;
		System.arraycopy(value_header, 0, _data, index, value_header.length);
		index += value_header.length;
		System.arraycopy(value, 0, _data, index, value.length);
		NaiveNetRequestData req = new NaiveNetRequestData(
				NaiveNetRequestData.TO_NAIVENET_SERVER,
				"setsession",
				_data,
				response
			);
		this.genTempMsgId(req);
	}
	
	/**
	 * 	获取用户的ping值
	 *  @param response {@link NaiveNetOnResponse} 的实现
	 * */
	public void getPing(NaiveNetOnResponse response) {
		NaiveNetRequestData req = new NaiveNetRequestData(
					NaiveNetRequestData.TO_NAIVENET_SERVER,
					"ping",
					new byte[0],
					response
				);
		this.genTempMsgId(req);
	}
}
