package cn.naivenet.TimerEvent;

public class test {
	public static void main(String[] args) {

		Task t = Timer.SetTimeOut(new TimerTask() {

			@Override
			public void Event() {
				System.out.println("5秒后触发");
				
			}
			
		}, 5000);
		
		Timer.CancelTask(t);
		
	}
}
