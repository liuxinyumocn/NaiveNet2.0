package cn.domoe.naive.log;

public class Log {

	public static void write(String str){
		System.out.println(str);
	}
	public static void write(int str){
		System.out.println(str);
	}
	
	public static void print(byte[] n) {
		
		System.out.println("当前数据序列共长"+n.length);
		for(int i = 0; i<n.length;i++) {
			System.out.print(n[i]);
			System.out.print(" ");
		}
		System.out.println("");
	}
	
}
