package doext.implement;

import doext.define.do_ListView_MAbstract;

/**
 * 自定义扩展组件Model实现，继承Do_ListView_MAbstract抽象类；
 * 
 */
public class do_ListView_Model extends do_ListView_MAbstract {

	public do_ListView_Model() throws Exception {
		super();
	}
	
	@Override
	public void didLoadView() throws Exception {
		super.didLoadView();
		((do_ListView_View)this.getCurrentUIModuleView()).loadDefalutScriptFile();
	}
}
