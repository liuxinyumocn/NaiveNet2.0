package cn.naivenet.TimerEvent;

import java.util.ArrayList;

/**
 * 	周期回调任务触发器，
 * 	业务中所有需要发生回调的任务向Timer提交事件注册，在特定延迟后触发事件回调
 * 	该回调为全局唯一单线程处理，若回调任务存在耗时逻辑可能会影响后续回调事件的触发
 * */
public class Timer {

	static {
		sleep = new SleepThread();
	}
	
	
	/**
	 * 	任务清单
	 * */
	private static ArrayList<Task> tasklist = new ArrayList<>();
	
	/**
	 *	休眠中的回调线程
	 * */
	private static Thread sleepthread;
	private static Task currentWaitTask;
	private static SleepThread sleep;
	
	/**
	 * 	添加任务到清单中，
	 * 	@param timerTask 提供任务回调的具体函数实现，
	 * 	@param timeout 提供等待的时长，单位毫秒
	 * 
	 *  @return 返回用于取消的Task事件句柄，使用 .cancelTask( Task task) 可对未完成的任务终止触发
	 * */
	public static Task SetTimeOut(TimerTask timerTask , long timeout) {
		if (timerTask == null || timeout <= 0)
			return null;
		long now = System.currentTimeMillis();
		Task t = new Task();
		t.planFinishedTimeStamp = now + timeout;
		t.timeout = timeout;
		t.timertask = timerTask;
		
		if (currentWaitTask == null) {
			synchronized (tasklist) {
				//将任务添加至清单队列中
				tasklist.add(t);
			}
			//创建新的休眠线程
			sleepthread = new Thread(sleep);
			sleepthread.start();
		}else if(t.planFinishedTimeStamp < currentWaitTask.planFinishedTimeStamp) {
			//取消当前休眠
			if (sleepthread != null)
				sleepthread.interrupt();
			//清空当前任务
			currentWaitTask = null;
		
			synchronized (tasklist) {
				//将任务添加至清单队列中
				tasklist.add(t);
			}
			
			//创建新的休眠线程
			sleepthread = new Thread(sleep);
			sleepthread.start();
		}else {
			synchronized (tasklist) {
				//将任务添加至清单队列中
				tasklist.add(t);
			}
		}
		return t;
	}

	/**
	 * 	取消还未执行的任务
	 * 	@param 由 setTimeout 返回的任务句柄
	 * 	@return 是否存在对应的未执行任务，若存在则取消其任务并返回 true ，否则返回 false
	 * */
	public static boolean CancelTask(Task task) {
		if( task == null)
			return false;
		
		int index = -1;
		synchronized (tasklist) {
			for (int i = 0 ; i < tasklist.size();i++) {
				if (tasklist.get(i) == task) {
					if (currentWaitTask == task) { //需要提前取消
						index = i;
					}else {
						tasklist.remove(i);
						return true;
					}
				}
			}
		}
		if(index != -1) {
			//取消当前休眠
			if (sleepthread != null)
				sleepthread.interrupt();
			tasklist.remove(index);
			//清空当前任务
			currentWaitTask = null;
			//创建新的休眠线程
			sleepthread = new Thread(sleep);
			sleepthread.start();
			return true;
		}
		
		return false;
	}
	
	static private class SleepThread implements Runnable{

		@Override
		public void run() {
			while(true) {
				//找到任务队列中已经过期的任务立即执行
				long now = System.currentTimeMillis();
				long min = 0;
				int min_index = -1;
				Task min_task = null;
				synchronized (tasklist) {
					if (tasklist.size() == 0) {
						currentWaitTask = null;
						break;
					}
					for( int i = 0 ; i < tasklist.size();i++) {
						Task t = tasklist.get(i);
						if (t.planFinishedTimeStamp < now) { //已经过期立即执行
							tasklist.remove(i);
							min_index = -1;
							currentWaitTask = t;
							t.timertask.Event();
							break;
						}else {
							if (min == 0 || t.planFinishedTimeStamp < min) {
								min = t.planFinishedTimeStamp;
								min_index = i;
							}
						}
					}
					if(min_index != -1)
						min_task = tasklist.get(min_index);
					else
						continue;
				}
				
				currentWaitTask = min_task;
				try {
					Thread.sleep(min_task.planFinishedTimeStamp - now);
				} catch (InterruptedException e) {
					//说明新的任务结束直接比当前任务更早
					break;
				}
				
				synchronized (tasklist) {
					tasklist.remove(min_task);
					min_task.timertask.Event();
				}
				
			}
			
		}
		
	}
	
}
