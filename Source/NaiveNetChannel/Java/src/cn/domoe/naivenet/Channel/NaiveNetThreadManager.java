package cn.domoe.naivenet.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


class NaiveNetThreadManager {

	public static void Init(int MAXTHREAD) {
		maxthread = MAXTHREAD;
	}
	
	private static int maxthread = 40; //最大的单任务处理线程
	private static ConcurrentHashMap<User,DealThread> threadmap = new ConcurrentHashMap<>();
	private static ConcurrentLinkedQueue<User> taskqueue = new ConcurrentLinkedQueue<>();
	
	/**
	 * 提交任务执行申请
	 * */
	public static void push(User user) {
		if(threadmap.size() < maxthread) {
			DealThread t = new DealThread(user);
			threadmap.put(user,t);
			NaiveNetThreadPool.getPool().submit(t);
		}else { //放入队列中
			taskqueue.add(user);
		}
	}
	
	private static class DealThread implements Runnable{

		private User user;
		public DealThread(User ns) {
			this.user = ns;
		}
		
		@Override
		public void run() {
			while(true) {
				this.user.doit();
				//检查任务队列
				User ns = taskqueue.poll();
				if(ns != null) {
					User ns2 = this.user;
					this.user = ns;
					threadmap.put(ns,this);
					threadmap.remove(ns2);
				}else {
					//任务移除队列
					threadmap.remove(user);
					break;
				}
			}
		}
		
	}
	
}
