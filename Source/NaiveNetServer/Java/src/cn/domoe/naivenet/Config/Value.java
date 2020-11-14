package cn.domoe.naivenet.Config;

public class Value {

	private int intv;
	public Value(int v) {
		intv = v;
	}

	private String stringv;
	public Value(String string) {
		this.stringv = string;
	}

	private boolean bl = false;
	public Value(boolean bl) {
		this.bl = bl;
	}

	public int getInt() {
		return this.intv;
	}
	
	public String getStr() {
		return this.stringv;
	}
	
	public boolean getBool() {
		return this.bl;
	}
	
}