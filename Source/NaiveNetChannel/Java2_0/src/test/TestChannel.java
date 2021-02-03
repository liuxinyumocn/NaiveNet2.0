package test;

import cn.naivenet.Channel.NaiveNetBox;
import cn.naivenet.Channel.NaiveNetChannel;
import cn.naivenet.Channel.NaiveNetController;
import cn.naivenet.Channel.NaiveNetEvent;
import cn.naivenet.Channel.NaiveNetMessage;
import cn.naivenet.Channel.NaiveNetResponse;
import cn.naivenet.Channel.User;

public class TestChannel {

	NaiveNetChannel channel;
	
	public TestChannel() {
		
		//静态User Controller
		NaiveNetBox box1 = new NaiveNetBox();
		box1.addController(new NaiveNetController("ctrl1") {

			@Override
			public NaiveNetResponse onRequest(NaiveNetMessage msg) {
				System.out.println("ctrl1 访问到了 参数是："+new String(msg.param));
				msg.user.auth(null);
				
				return msg.getResponseHandler("ctrl1  response data 123");
			}
			
		});
		User.AddBox(box1);
		
		channel = new NaiveNetChannel(5000,"abcd");
		try {
			channel.setOnNewUserListener(new NaiveNetEvent() {

				@Override
				public void on(User user, byte[] data) {
					user.setOnBreakListener(new NaiveNetEvent() {

						@Override
						public void on(User user, byte[] data) {
							System.out.println("break");
						}
						
					});
					
					user.setOnQuitListener(new NaiveNetEvent() {

						@Override
						public void on(User user, byte[] data) {
							// TODO Auto-generated method stub
							System.out.println("quit");
							
						}
						
					});
					
					user.setOnRecoverListener(new NaiveNetEvent() {

						@Override
						public void on(User user, byte[] data) {

							System.out.println("recover");
						}
						
					});
					
				}
				
			});

			channel.launch();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		new TestChannel();
	}
}
