package cn.domoe.naivenet.Channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Channel
 * */
class NaiveNetChannelServer {

	private int port;
	private UserManager userManger;
	
	private Selector selector;
	private ServerSocketChannel server;
	
	private ConcurrentHashMap<SocketChannel,User> hashmapSocketChannelAndUser;
	
	public NaiveNetChannelServer(int port, UserManager userManager) {
		
		this.port = port;
		this.userManger = userManager;
		this.hashmapSocketChannelAndUser = new ConcurrentHashMap<>();
		this.userManger.naiveNetChannelServer = this;
	}
	
	public void launch(int Max_Thread) throws IOException {
		
		if(Max_Thread < 0) {
			Max_Thread = Runtime.getRuntime().availableProcessors() * 4;
		}
		NaiveNetThreadManager.Init(Max_Thread);
		selector = Selector.open();
		server = ServerSocketChannel.open();
		InetSocketAddress isa = new InetSocketAddress("127.0.0.1",this.port);
		server.bind(isa);
		server.configureBlocking(false);
		server.register(selector, SelectionKey.OP_ACCEPT);
		NaiveNetThreadPool.getPool().submit(new AcceptThread());
	}

	private boolean blocking = false;
	class AcceptThread implements Runnable {

		@Override
		public void run() {
			while(true) {
				
				int num;
				try {
					blocking = true;
					//System.out.println("等待");
					num = selector.select();
					blocking = false;
				}catch(Exception e) {
					//System.out.println("ServerChannel Accept Error,Shutdown Service");
					shutdown();
					break;
				}
				if(num <= 0) {
					continue;
				}
				SelectionKey[] removelist = new SelectionKey[selector.selectedKeys().size()];
				int removelist_index = 0;
				for(SelectionKey sk : selector.selectedKeys()) {
					removelist[removelist_index] = sk;
					if(sk.isAcceptable()) {
						SocketChannel sc;
						try {
							sc = server.accept();
						}catch (IOException e) {
							continue;
						}
						try {
							sc.configureBlocking(false);
							sc.register(selector, SelectionKey.OP_READ);
							
							//创建新用户
							User user = userManger.createUser(sc);
							hashmapSocketChannelAndUser.put(sc,user);
							
						}catch(Exception e) {
							
						}
						sk.interestOps(SelectionKey.OP_ACCEPT);
					}else if(sk.isReadable()) {
						SocketChannel sc = (SocketChannel)sk.channel();
						User user = hashmapSocketChannelAndUser.get(sc);
						if(user == null)
							continue;
						ByteBuffer buff = ByteBuffer.allocate(1024);
						byte[] data = new byte[1024];
						int index = 0;
						try {
							while(sc.read(buff) > 0) {
								byte[] b = buff.array();
								if(buff.position() + index > data.length) { //延长缓冲区
									byte[] data2 = new byte[data.length + 1024];
									System.arraycopy(data, 0, data2, 0, data.length);
									data = data2;
								}
								
								for(int i = 0;i< buff.position();i++) {
									data[index++] = b[i];
								}
								buff.clear();
							}
							//数据读取完毕
							if(index == 0) {
								//说明与NS网络发生断开
								user.onQuit();
							}else {
								byte[] _data = new byte[index];
								System.arraycopy(data, 0, _data, 0, _data.length);
								try {
									userManger.onRead(user,_data);
								} catch (Exception e) {
									
								}
								sk.interestOps(SelectionKey.OP_READ);
							}
						}catch(Exception e) {
							sk.cancel();
							if(sk.channel() != null) {
								user.onQuit();
							}
						}
					}
					
				}
				

				for(int i = 0;i<removelist.length;i++) {
					selector.selectedKeys().remove(removelist[i]);
				}
				
			}
			
		}
		
	}
	
	public void shutdown() {

		
	}

	/**
	 * 	关闭掉User句柄
	 * */
	public void close(User user) {
		SocketChannel c = user.getSocketChannel();
		try{
			this.hashmapSocketChannelAndUser.remove(c);
		}catch(Exception e) {
			
		}
		try {
			c.close();
		} catch (IOException e) {
			
		}
		
	}
}
