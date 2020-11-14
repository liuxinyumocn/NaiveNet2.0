package main;

import cn.domoe.naivenet.NaiveNet;

public class Main {

	public static void main(String[] args) {

		try {
			new NaiveNet().launch();
		} catch (Exception e) {
			e.printStackTrace();
		};
		
	}

}
