package cn.naivenet.User;

import java.util.ArrayList;
import java.util.List;



public class NaiveNetBox {
	

	private  List<NaiveNetController> controllers = new ArrayList<>();
	
	public void addController(NaiveNetController ctrl) {
		controllers.add(ctrl);
	}
	
	public void removeController(NaiveNetController ctrl) {
		controllers.remove(ctrl);
	}

	public NaiveNetResponseData deal(NaiveNetMessage msg) {
		for(int i = 0;i<controllers.size();i++) {
			if(controllers.get(i).name.equals(msg.controller)) {
				NaiveNetResponseData res = controllers.get(i).onRequest(msg);
				if(res != null)
					return res;
				res = new NaiveNetResponseData(msg,CodeMap.OK,true);
				return res;
			}
		}
		return null;
	}

}
