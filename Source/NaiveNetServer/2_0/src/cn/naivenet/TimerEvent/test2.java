package cn.naivenet.TimerEvent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class test2 {

	public static void main(String[] arg) {
		System.out.println(1);
		ScheduledExecutorService scheduledExecutorService=new ScheduledThreadPoolExecutor(2);
		ScheduledFuture f21 = scheduledExecutorService.schedule(new Runnable() {
            @Override
      public void run() {
         System.out.println("21");
      }
},4, TimeUnit.SECONDS);
		scheduledExecutorService.schedule(new Runnable() {
            @Override
      public void run() {
         System.out.println("22");
      }
},4, TimeUnit.SECONDS);
		scheduledExecutorService.schedule(new Runnable() {
            @Override
      public void run() {
         System.out.println("23");
      }
},4, TimeUnit.SECONDS);
		scheduledExecutorService.schedule(new Runnable() {
            @Override
      public void run() {
         System.out.println("24");
      }
},4, TimeUnit.SECONDS);
		scheduledExecutorService.schedule(new Runnable() {
            @Override
      public void run() {
         System.out.println("25");
      }
},4, TimeUnit.SECONDS);
		System.out.println("3");
		f21.cancel(true);
		
	}
	
}
