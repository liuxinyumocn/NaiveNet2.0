package cn.naivenet.Channel;

public class SyncResult {
	
	public final int code;
	public final byte[] data;
	
	public SyncResult(int c,byte[] d) {
		code = c;
		data = d;
	}
	
}
