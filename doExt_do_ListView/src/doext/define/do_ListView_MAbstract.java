package doext.define;

import core.object.DoUIModule;
import core.object.DoProperty;
import core.object.DoProperty.PropertyDataType;

public abstract class do_ListView_MAbstract extends DoUIModule {

	protected do_ListView_MAbstract() throws Exception {
		super();
	}

	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception {
		super.onInit();
		// 注册属性
		this.registProperty(new DoProperty("selectedColor", PropertyDataType.String, "ffffff00", true));
		this.registProperty(new DoProperty("templates", PropertyDataType.String, "", false));
		this.registProperty(new DoProperty("headerView", PropertyDataType.String, "", true));
		this.registProperty(new DoProperty("footerView", PropertyDataType.String, "", true));
		this.registProperty(new DoProperty("isShowbar", PropertyDataType.Bool, "", true));
		this.registProperty(new DoProperty("isHeaderVisible", PropertyDataType.Bool, "false", true));
		this.registProperty(new DoProperty("isFooterVisible", PropertyDataType.Bool, "false", true));
	}
}