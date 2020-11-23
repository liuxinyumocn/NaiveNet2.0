package cn.domoe.naivenet.Channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

class UserManager {
	
	private NaiveNetEvent onNewUser = null;
	private ConcurrentLinkedDeque<User> users;
	private ConcurrentLinkedDeque<User> unlog;
	private byte[] token;
	
	public UserManager(String token) {
		this.unlog = new ConcurrentLinkedDeque<>();
		this.users = new ConcurrentLinkedDeque<>();
		this.msgqueueToNS = new ConcurrentLinkedDeque<>();
		this.token = ("NAIVENETCHANNEL TOKEN["+token+"]").getBytes();
		User.INIT();
	}

	public byte[] getToken() {
		return token;
	}
	
	public User createUser(Channel sc) {
		User user = new User(sc,this);
		this.unlog.add(user);
		return user;
	}

	public void onRead(User user, byte[] _data) {
		byte[] cache = user.getCacheBuffer();
		if(cache == null) {
			this.parsePackage(user,_data);
		}else {
			byte[] data2 = new byte[_data.length + cache.length];
			System.arraycopy(cache, 0, data2, 0, cache.length);
			System.arraycopy(_data, 0, data2, cache.length, _data.length);
			this.parsePackage(user,data2);
		}
	}
	
	private void parsePackage(User user,byte[] data) {
		while(true) {
			int len = 0;
			int index = 0;
			for(;index<data.length;index++) {
				int a = data[index]&0x0FF;
				len += a;
				if(a != 255)
					break;
			}
			index++;
			int l = index + len;
			if(l> data.length) { //发生断包
				user.setCacheBuffer(data);
				return;
			}
			
			if(l== data.length) {
				user.setCacheBuffer(null);
				byte[] _data = new byte[len];
				System.arraycopy(data, index, _data, 0, _data.length);
				user.push(_data);
				return;
			}
			//发生粘包
			byte[] _buf = new byte[data.length - l];
			System.arraycopy(data, l, _buf, 0, _buf.length);
			byte[] _data = new byte[l];
			System.arraycopy(data, index, _data, 0, len);
			data = _buf;
			user.push(_data);
		}
		
	}


	private ConcurrentLinkedDeque<Msg> msgqueueToNS;
	private boolean sending = false;
	public NaiveNetChannelServer naiveNetChannelServer;
	
	public void pushUserMessage(Channel socketChannel, byte[] data) {
		this.msgqueueToNS.add(new Msg(socketChannel,data));
		if(sending) {
			return;
		}
		sending = true;
		NaiveNetThreadPool.getPool().submit(new SendThread());
	}
	
	class Msg {
		public Channel socketChannel; 
		public byte[] data;
		public Msg(Channel socketChannel2, byte[] data) {
			this.socketChannel = socketChannel2;
			this.data = data;
		}
		
	}

	class SendThread implements Runnable{

		@Override
		public void run() {
			while(true) {
				Msg msg = msgqueueToNS.poll();
				if(msg == null)
				{
					sending = false;
					return;
				}
				//发送数据前需要对数据装箱
				byte[] header = UserManager.calNumber(msg.data.length);
				ByteBuf buf = msg.socketChannel.alloc().buffer();
				buf.writeBytes(header);
				buf.writeBytes(msg.data);
				msg.socketChannel.writeAndFlush(buf);
				
				
			}
			
		}
		
	}
	
	private static byte[] calNumber(int len) {
		int length = len/255 + 1;
		byte[] res = new byte[length];
		for(int i = 0; i< res.length;i++) {
			if(len > 255) {
				res[i] = (byte)255;
				len -= 255;
			}else {
				res[i] = (byte)len;
				break;
			}
		}
		return res;
	}

	/**
	 * 新用户抵达
	 * */
	public void setOnNewUserListener(NaiveNetEvent e) {
		this.onNewUser = e;
	}

	public void onNewUser(User user) {
		//从Unlog表移除 增加到 Users表
		try {
			this.unlog.remove(user);
		}catch(Exception e) {
			
		}
		this.users.add(user);
		if(this.onNewUser != null)
			this.onNewUser.on(user, null);
	}

	
	/**
	 * 	对已经登录的用户句柄进行回收
	 * */
	public void userQuit(User user) {
		
		try {
			this.users.remove(user);
			this.unlog.remove(user);
			Channel channel = user.getSocketChannel();
			this.naiveNetChannelServer.close(user);
			channel.close();
		}catch(Exception e) {
			
		}
		
	}
	
	/**
	 * 	对未登录的用户句柄进行回收
	 * */
	public void unlogUserQuit(User user) {
		
		try {
			this.unlog.remove(user);
			this.naiveNetChannelServer.close(user);
		}catch(Exception e) {
			
		}
		
	}

	public void log(User user) {
		this.unlog.remove(user);
	}

	
}
