package doext.implement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoScriptEngineHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIListData;
import core.interfaces.DoIPage;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoEventCenter;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoProperty;
import core.object.DoSourceFile;
import core.object.DoUIContainer;
import core.object.DoUIModule;
import doext.define.do_ListView_IMethod;
import doext.define.do_ListView_MAbstract;
import doext.listview.fling.DoFlingRunnable;
import doext.listview.pullToRefresh.DoPullToRefreshView;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,Do_ListView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_ListView_View extends DoPullToRefreshView implements DoIUIModuleView, do_ListView_IMethod, android.widget.AdapterView.OnItemClickListener,
		android.widget.AdapterView.OnItemLongClickListener, OnScrollListener {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_ListView_MAbstract model;
	protected MyAdapter myAdapter;
	private MyListView listview;
	private int oldFirstVisiblePosition;
	private int oldLastVisiblePosition;
	private double xZoom;
	private double yZoom;

	private class MyListView extends ListView {
		public MyListView(Context context) {
			super(context);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			try {
				DoInvokeResult _result = new DoInvokeResult(model.getUniqueKey());
				JSONObject _value = new JSONObject();
				_value.put("w", w / xZoom);
				_value.put("h", h / yZoom);
				_value.put("oldw", oldw / xZoom);
				_value.put("oldh", oldh / yZoom);
				_result.setResultNode(_value);
				model.getEventCenter().fireEvent("sizeChanged", _result);
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("sizeChanged event", e);
			}
		}

	}

	public do_ListView_View(Context context) {
		super(context);
		this.setOrientation(VERTICAL);
		listview = new MyListView(context);
		myAdapter = new MyAdapter();

	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_ListView_MAbstract) _doUIModule;
		xZoom = _doUIModule.getXZoom();
		yZoom = _doUIModule.getYZoom();
		TYPEID = this.model.getTypeID();
		listview.setOnItemClickListener(this);
		listview.setOnItemLongClickListener(this);
		listview.setOnScrollListener(this);

		String _headerViewPath = this.model.getHeaderView();
		String _footerViewPath = this.model.getFooterView();
		listview.setBackgroundColor(Color.TRANSPARENT);
		listview.setDivider(new ColorDrawable(Color.TRANSPARENT));
		listview.setDividerHeight(0);
		setHeaderView(createHeaderView(_headerViewPath));
		this.addView(listview, new LinearLayout.LayoutParams(-1, -1));
		setFooterView(createFooterView(_footerViewPath));
		final boolean _isHeaderVisible = isHeaderVisible();
		final boolean _isFooterVisible = isFooterVisible();
		this.setSupportHeaderRefresh(_isHeaderVisible);
		this.setSupportFooterRefresh(_isFooterVisible);
		this.onFinishInflate();
		this.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			public void onGlobalLayout() {
				if (_isHeaderVisible || _isFooterVisible) {
					int _width = getWidth();
					int _height = getHeight();
					if (_width == 0 || _height == 0) {
						_width = -1;
						_height = -1;
					}
					listview.setLayoutParams(new LayoutParams(_width, _height));
				}
				getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
		//解决scrollview中嵌套listview //////////////////////用户变态需求，哼！！！！！！！！
//		listview.setOnTouchListener(new OnTouchListener() {
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				v.getParent().getParent().requestDisallowInterceptTouchEvent(true);
//				return false;
//			}
//		});
	}

	private View createHeaderView(String _uiPath) throws Exception {
		View _newView = null;
		if (_uiPath != null && !"".equals(_uiPath.trim())) {
			this.headerUIPath = _uiPath;
			DoIPage _doPage = this.model.getCurrentPage();
			DoSourceFile _uiFile = _doPage.getCurrentApp().getSourceFS().getSourceByFileName(_uiPath);
			if (_uiFile != null) {
				headerRootUIContainer = new DoUIContainer(_doPage);
				headerRootUIContainer.loadFromFile(_uiFile, null, null);
				if (null != _doPage.getScriptEngine()) {
					headerRootUIContainer.loadDefalutScriptFile(_uiPath);
				}
				DoUIModule _model = headerRootUIContainer.getRootView();
				_newView = (View) _model.getCurrentUIModuleView();
				// 设置headerView 的 宽高
				_newView.setLayoutParams(new LayoutParams((int) _model.getRealWidth(), (int) _model.getRealHeight()));
			} else {
				DoServiceContainer.getLogEngine().writeDebug("试图打开一个无效的页面文件:" + _uiPath);
			}
		}
		return _newView;
	}

	private View createFooterView(String _uiPath) throws Exception {
		View _newView = null;
		if (_uiPath != null && !"".equals(_uiPath.trim())) {
			this.footerUIPath = _uiPath;
			DoIPage _doPage = this.model.getCurrentPage();
			DoSourceFile _uiFile = _doPage.getCurrentApp().getSourceFS().getSourceByFileName(_uiPath);
			if (_uiFile != null) {
				footerRootUIContainer = new DoUIContainer(_doPage);
				footerRootUIContainer.loadFromFile(_uiFile, null, null);
				if (null != _doPage.getScriptEngine()) {
					footerRootUIContainer.loadDefalutScriptFile(_uiPath);
				}
				DoUIModule _model = footerRootUIContainer.getRootView();
				_newView = (View) _model.getCurrentUIModuleView();
				// 设置headerView 的 宽高
				_newView.setLayoutParams(new LayoutParams((int) _model.getRealWidth(), (int) _model.getRealHeight()));
			} else {
				DoServiceContainer.getLogEngine().writeDebug("试图打开一个无效的页面文件:" + _uiPath);
			}
		}
		return _newView;
	}

	private DoUIContainer headerRootUIContainer;
	private String headerUIPath;

	private DoUIContainer footerRootUIContainer;
	private String footerUIPath;

	public void loadDefalutScriptFile() throws Exception {
		if (headerRootUIContainer != null && headerUIPath != null) {
			headerRootUIContainer.loadDefalutScriptFile(headerUIPath);
		}
		if (footerRootUIContainer != null && footerUIPath != null) {
			footerRootUIContainer.loadDefalutScriptFile(footerUIPath);
		}
	}

	private boolean isHeaderVisible() throws Exception {
		DoProperty _property = this.model.getProperty("isHeaderVisible");
		if (_property == null) {
			return false;
		}
		return DoTextHelper.strToBool(_property.getValue(), false);
	}

	private boolean isFooterVisible() throws Exception {
		DoProperty _property = this.model.getProperty("isFooterVisible");
		if (_property == null) {
			return false;
		}
		return DoTextHelper.strToBool(_property.getValue(), false);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		if (_changedValues.containsKey("templates")) {
			String value = _changedValues.get("templates");
			if ("".equals(value)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("herderView")) {

		}
		if (_changedValues.containsKey("isShowbar")) {
			boolean _isShowbar = DoTextHelper.strToBool(_changedValues.get("isShowbar"), true);
			listview.setVerticalScrollBarEnabled(_isShowbar);
		}
		if (_changedValues.containsKey("selectedColor")) {
			try {
				String _bgColor = this.model.getPropertyValue("bgColor");
				String _selectedColor = _changedValues.get("selectedColor");
				Drawable normal = new ColorDrawable(DoUIModuleHelper.getColorFromString(_bgColor, Color.WHITE));
				Drawable selected = new ColorDrawable(DoUIModuleHelper.getColorFromString(_selectedColor, Color.WHITE));
				Drawable pressed = new ColorDrawable(DoUIModuleHelper.getColorFromString(_selectedColor, Color.WHITE));
				listview.setSelector(getBg(normal, selected, pressed));
			} catch (Exception _err) {
				DoServiceContainer.getLogEngine().writeError("do_ListView selectedColor \n\t", _err);
			}
		}

		if (_changedValues.containsKey("templates")) {
			initViewTemplate(_changedValues.get("templates"));
			listview.setAdapter(myAdapter);
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("rebound".equals(_methodName)) {
			rebound(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("bindItems".equals(_methodName)) {
			bindItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("refreshItems".equals(_methodName)) {
			refreshItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("scrollToPosition".equals(_methodName)) {
			scrollToPosition(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("showHeader".equals(_methodName)) {
			showHeader(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		// ...do something
		return false;
	}

	private void refreshItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		myAdapter.notifyDataSetChanged();
	}

	private void scrollToPosition(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws JSONException {
		int _position = DoJsonHelper.getInt(_dictParas, "position", 0);
		mIsSmooth = DoJsonHelper.getBoolean(_dictParas, "isSmooth", false);
		if (mIsSmooth) {
			listview.smoothScrollToPositionFromTop(_position, 0, 250);
		} else {
			new DoFlingRunnable().stopScroll(listview);
			listview.setSelection(_position);
		}
		oldFirstVisiblePosition = _position;
		oldLastVisiblePosition = _position + listview.getChildCount() - 1;
	}

	private void bindItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _address = DoJsonHelper.getString(_dictParas, "data", "");
		if (_address == null || _address.length() <= 0)
			throw new Exception("doListView 未指定相关的listview data参数！");
		DoMultitonModule _multitonModule = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _address);
		if (_multitonModule == null)
			throw new Exception("doListView data参数无效！");
		if (_multitonModule instanceof DoIListData) {
			DoIListData _data = (DoIListData) _multitonModule;
			myAdapter.bindData(_data);
		}
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		// ...do something
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	private void initViewTemplate(String data) {
		try {
			myAdapter.initTemplates(data.split(","));
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("解析cell属性错误： \t", e);
		}
	}

	private class MyAdapter extends BaseAdapter {
		private Map<String, String> viewTemplates = new HashMap<String, String>();
		private List<String> cellTemplates = new ArrayList<String>();
		private SparseIntArray datasPositionMap = new SparseIntArray();
		private DoIListData data;

		public void bindData(DoIListData _listData) {
			this.data = _listData;
			notifyDataSetChanged();
		}

		public void initTemplates(String[] templates) throws Exception {
			cellTemplates.clear();
			for (String templateUi : templates) {
				if (templateUi != null && !templateUi.equals("")) {
					DoSourceFile _sourceFile = model.getCurrentPage().getCurrentApp().getSourceFS().getSourceByFileName(templateUi);
					if (_sourceFile != null) {
						viewTemplates.put(templateUi, _sourceFile.getTxtContent());
						cellTemplates.add(templateUi);
					} else {
						throw new Exception("试图使用一个无效的页面文件:" + templateUi);
					}
				}
			}
		}

		@Override
		public void notifyDataSetChanged() {
			int _size = data.getCount();
			for (int i = 0; i < _size; i++) {
				try {
					JSONObject childData = (JSONObject) data.getData(i);
					Integer _index = DoTextHelper.strToInt(DoJsonHelper.getString(childData, "template", "0"), 0);
					if (_index >= cellTemplates.size() || _index < 0) {
						DoServiceContainer.getLogEngine().writeError("索引不存在", new Exception("索引 " + _index + " 不存在"));
						_index = 0;
					}
					datasPositionMap.put(i, _index);
				} catch (Exception e) {
					DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
				}
			}
			super.notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			if (data == null) {
				return 0;
			}
			return data.getCount();
		}

		@Override
		public Object getItem(int position) {
			try {
				return data.getData(position);
			} catch (JSONException e) {
				DoServiceContainer.getLogEngine().writeError("do_ListView_View getItem \n\t", e);
			}
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return datasPositionMap.get(position);
		}

		@Override
		public int getViewTypeCount() {
			return cellTemplates.size();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View _childView = null;
			try {
				JSONObject childData = (JSONObject) data.getData(position);
				DoIUIModuleView _doIUIModuleView = null;
				int _index = DoTextHelper.strToInt(DoJsonHelper.getString(childData, "template", "0"), 0);
				if (_index >= cellTemplates.size() || _index < 0) {
					DoServiceContainer.getLogEngine().writeError("索引不存在", new Exception("索引 " + _index + " 不存在"));
					_index = 0;
				}
				String templateUI = cellTemplates.get(_index);
				if (convertView == null) {
					String content = viewTemplates.get(templateUI);
					DoUIContainer _doUIContainer = new DoUIContainer(model.getCurrentPage());
					_doUIContainer.loadFromContent(content, null, null);
					_doUIContainer.loadDefalutScriptFile(templateUI);// @zhuozy效率问题，listview第一屏可能要加载多次模版、脚本，需改进需求设计；
					_doIUIModuleView = _doUIContainer.getRootView().getCurrentUIModuleView();
				} else {
					_doIUIModuleView = (DoIUIModuleView) convertView;
				}
				if (_doIUIModuleView != null) {
					_doIUIModuleView.getModel().setModelData(childData);

					_childView = (View) _doIUIModuleView;
					// 设置headerView 的 宽高
					_childView.setLayoutParams(new AbsListView.LayoutParams((int) _doIUIModuleView.getModel().getRealWidth(), (int) _doIUIModuleView.getModel().getRealHeight()));
					if (_childView instanceof ViewGroup) {
						((ViewGroup) _childView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
					}
				}
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
			}
			if (_childView == null) {
				return new View(getContext());
			}
			return _childView;
		}
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		doListView_LongTouch(position);
		doListView_LongTouch1(position, view.getY());
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		doListView_Touch(position);
		doListView_Touch1(position, view.getY());
	}

	private void doListView_Touch(int position) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		_invokeResult.setResultInteger(position);
		this.model.getEventCenter().fireEvent("touch", _invokeResult);
	}

	private void doListView_Touch1(int position, float y) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		JSONObject _obj = new JSONObject();
		try {
			_obj.put("position", position);
			_obj.put("y", y / this.model.getYZoom());
		} catch (Exception e) {
		}
		_invokeResult.setResultNode(_obj);
		this.model.getEventCenter().fireEvent("touch1", _invokeResult);
	}

	private void doListView_LongTouch(int position) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		_invokeResult.setResultInteger(position);
		this.model.getEventCenter().fireEvent("longTouch", _invokeResult);
	}

	private void doListView_LongTouch1(int position, float y) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		JSONObject _obj = new JSONObject();
		try {
			_obj.put("position", position);
			_obj.put("y", y / this.model.getYZoom());
		} catch (Exception e) {
		}
		_invokeResult.setResultNode(_obj);
		this.model.getEventCenter().fireEvent("longTouch1", _invokeResult);
	}

	private StateListDrawable getBg(Drawable normal, Drawable selected, Drawable pressed) {
		StateListDrawable bg = new StateListDrawable();
		bg.addState(View.PRESSED_ENABLED_STATE_SET, pressed);
		bg.addState(View.ENABLED_FOCUSED_STATE_SET, selected);
		bg.addState(View.ENABLED_STATE_SET, normal);
		bg.addState(View.FOCUSED_STATE_SET, selected);
		bg.addState(View.EMPTY_STATE_SET, normal);
		return bg;
	}

	private void rebound(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		if (mPullState == PULL_DOWN_STATE) {
			savaTime(System.currentTimeMillis());
			onHeaderRefreshComplete();
		} else if (mPullState == PULL_UP_STATE) {
			onFooterRefreshComplete();
		}
	}

	private void showHeader(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		autoRefresh();
	}

	@Override
	protected void fireEvent(int mHeaderState, int newTopMargin, String eventName) {
		DoEventCenter _eventCenter = this.model.getEventCenter();
		if (_eventCenter == null) {
			return;
		}
		int offset = mHeaderView.getHeight() + newTopMargin;
		if (mHeaderState == RELEASE_TO_REFRESH) {
			offset = mHeaderView.getHeight();
		}
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		try {
			JSONObject _node = new JSONObject();
			_node.put("state", mHeaderState);
			_node.put("offset", (Math.abs(offset) / this.model.getYZoom()) + "");
			_invokeResult.setResultNode(_node);
			_eventCenter.fireEvent(eventName, _invokeResult);
		} catch (Exception _err) {
			DoServiceContainer.getLogEngine().writeError("DoListView " + eventName + " \n", _err);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {

	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (!mIsSmooth) {//false不触发scroll事件
			return;
		}
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		int firstVisiblePosition = view.getFirstVisiblePosition();
		int lastVisiblePosition = view.getLastVisiblePosition();
		if (lastVisiblePosition != -1) {
			if (oldFirstVisiblePosition == firstVisiblePosition && oldLastVisiblePosition == lastVisiblePosition) {
				return;
			}
			oldFirstVisiblePosition = firstVisiblePosition;
			oldLastVisiblePosition = lastVisiblePosition;
			try {
				JSONObject _node = new JSONObject();
				_node.put("firstVisiblePosition", firstVisiblePosition);
				_node.put("lastVisiblePosition", lastVisiblePosition);
				_invokeResult.setResultNode(_node);
				this.model.getEventCenter().fireEvent("scroll", _invokeResult);
			} catch (Exception _err) {
				DoServiceContainer.getLogEngine().writeError("DoListView scroll" + " \n", _err);
			}
		}
	}

	@Override
	public void onFinishInflate() {
		super.onFinishInflate();
		mAdapterView = this.listview;
	}
}
