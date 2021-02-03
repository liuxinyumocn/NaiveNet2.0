package cn.naivenet.Channel;

import java.util.ArrayList;
import java.util.List;


public class NaiveNetBox {
	

	private  List<NaiveNetController> controllers = new ArrayList<>();
	

	/**
	 * 	添加 Controller
	 * 	@param ctrl 是{@link NaiveNetController}的实例或子类<br>
	 *	例如：
	 *  box.addController(
	 *  	new NaiveNetController( "控制器的名称" ){
	 *  		.....
	 *  	}
	 *  )
	 * */
	public void addController(NaiveNetController ctrl) {
		controllers.add(ctrl);
	}
	

	/**
	 * 	移除已经添加的 Controller
	 * */
	public void removeController(NaiveNetController ctrl) {
		controllers.remove(ctrl);
	}

	/**
	 * 	开发者请勿执行该方法，可能导致NaiveNet工作异常
	 * */
	public NaiveNetResponseData deal(NaiveNetMessage msg) {
		for(int i = 0;i<controllers.size();i++) {
			if(controllers.get(i).name.equals(msg.controller)) {
				NaiveNetResponse res = controllers.get(i).onRequest(msg);
				if(res != null)
					return new NaiveNetResponseData(res);
				return new NaiveNetResponseData(msg,CodeMap.OK,true);
			}
		}
		return null;
	}


}
