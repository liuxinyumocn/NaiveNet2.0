package cn.domoe.naivenet.Channel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NaiveNetThreadPool {
	private static ExecutorService threadPool = Executors.newCachedThreadPool() ; //静态线程池全局共享

	public static ExecutorService getPool(){
		return threadPool;
	}
}
