package test;

import cn.naivenet.Channel.NaiveNetBox;
import cn.naivenet.Channel.NaiveNetChannel;
import cn.naivenet.Channel.NaiveNetController;
import cn.naivenet.Channel.NaiveNetControllerAsync;
import cn.naivenet.Channel.NaiveNetEvent;
import cn.naivenet.Channel.NaiveNetMessage;
import cn.naivenet.Channel.NaiveNetOnResponse;
import cn.naivenet.Channel.NaiveNetResponse;
import cn.naivenet.Channel.SyncResult;
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
				msg.user.setSession("abc", msg.param, null);
				
				return msg.getResponseHandler("ctrl1  response data"+new String(msg.param));
			}
			
		});
		box1.addController(new NaiveNetControllerAsync("ctrl2") {

			@Override
			public void onRequest(NaiveNetMessage msg) {
				msg.user.getSession("abc", new NaiveNetOnResponse() {

					@Override
					public void OnComplete(int code, byte[] data) {
						
						msg.user.response(msg.getResponseHandler(data));
						
					}
					
				});
				
			}
			
		});
		User.AddBox(box1);
		
		channel = new NaiveNetChannel(3001,"abc");
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
