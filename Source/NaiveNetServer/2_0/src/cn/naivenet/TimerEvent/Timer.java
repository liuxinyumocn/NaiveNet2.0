package cn.naivenet.TimerEvent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 	周期回调任务触发器，
 * 	业务中所有需要发生回调的任务向Timer提交事件注册，在特定延迟后触发事件回调
 * 	该回调为全局唯一单线程处理，若回调任务存在耗时逻辑可能会影响后续回调事件的触发
 * */
public class Timer {

	private static ScheduledExecutorService scheduledExecutorService;
	
	static {
		scheduledExecutorService = new ScheduledThreadPoolExecutor(4);
	}
	
	public static ScheduledFuture SetTimeout(Runnable command,int timeout) {
		return scheduledExecutorService.schedule(command, timeout, TimeUnit.MILLISECONDS);
	}
	
	public static void CancelTimeout(ScheduledFuture f) {
		if(f != null) {
			f.cancel(true);
		}
	}
	
	public static void shutdown() {
		scheduledExecutorService.shutdown();
	}
	
}
