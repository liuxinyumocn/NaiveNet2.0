package testChannel;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import cn.domoe.naivenet.Channel.NaiveNetBox;
import cn.domoe.naivenet.Channel.NaiveNetChannel;
import cn.domoe.naivenet.Channel.NaiveNetController;
import cn.domoe.naivenet.Channel.NaiveNetEvent;
import cn.domoe.naivenet.Channel.NaiveNetMessage;
import cn.domoe.naivenet.Channel.NaiveNetOnResponse;
import cn.domoe.naivenet.Channel.NaiveNetResponse;
import cn.domoe.naivenet.Channel.User;

public class testChannel {

	int time = 0;
	
	private NaiveNetChannel naiveNetChannel;
	NaiveNetBox box1;
	public testChannel() {
		naiveNetChannel = new NaiveNetChannel(5000,"abcdefg");
		//注册事件
		naiveNetChannel.setOnNewUserListener(new NewUser());

		NaiveNetBox box = new NaiveNetBox();
		box.addController(new NaiveNetController("test") {

			@Override
			public NaiveNetResponse onRequest(NaiveNetMessage msg) {
				//msg.user.auth(null);
				try {
					String data = new String(msg.data,"utf-8");
					System.out.println(data);
				} catch (UnsupportedEncodingException e) {
					
				}
				return null;
			}
			
		});
		User.AddBox(box);
	
	}
	
	class NewUser implements NaiveNetEvent{

		@Override
		public void on(User user, byte[] data) {
			System.out.println("新用户抵达");

			user.setOnBreakListener(new NaiveNetEvent() {

				@Override
				public void on(User user, byte[] data) {
					System.out.println("用户发生了断线");
					user.setSession("啊哈", "嘿嘿".getBytes(), null);
				}
				
			});
			user.setOnRecoverListener(new NaiveNetEvent() {

				@Override
				public void on(User user, byte[] data) {
					System.out.println("用户发生了恢复");
					user.getSession("啊哈", new NaiveNetOnResponse() {

						@Override
						public void OnComplete(int code, byte[] data) {
							
							try {
								System.out.println("SESSION:"+new String(data,"utf-8"));
							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						}
						
					});
				}
				
			});
			
			user.auth(null);
			user.request("clientCtrl", "你好".getBytes(), new NaiveNetOnResponse() {

				@Override
				public void OnComplete(int code, byte[] data) {
					System.out.println("收到的回应");
					System.out.println(code);
					System.out.println(new String(data));
					
				}
				
			});
			
			user.setOnQuitListener(new NaiveNetEvent() {

				@Override
				public void on(User user, byte[] data) {
					System.out.println("用户发生了退出");
					
					user.request(
							"ctrl_name_1", 
							"请求参数".getBytes(),
							new NaiveNetOnResponse() {
								@Override
								public void OnComplete(int code, byte[] data) {
									//回调处理函数体
								}}
					);
					
					
				}
				
			});
		}
		
	}
	
	
	
	
	public static void main(String[] args) {
		//System.out.println("["+new String(new byte[] {104, 101, 97, 114, 116, 0})+"]");
		
		new testChannel().launch();
	}

	public void launch() {
		try {
			naiveNetChannel.launch();
		} catch (Exception e) {
			System.out.println("NaiveNetChannel启动失败");
			e.printStackTrace();
		}
	}
}
