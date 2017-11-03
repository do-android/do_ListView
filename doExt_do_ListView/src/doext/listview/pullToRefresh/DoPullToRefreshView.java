package doext.listview.pullToRefresh;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import core.helper.DoResourcesHelper;
import core.interfaces.DoIModuleTypeID;

public class DoPullToRefreshView extends LinearLayout {
	// refresh states
	protected static final int PULL_TO_REFRESH = 0;
	protected static final int RELEASE_TO_REFRESH = 1;
	protected static final int REFRESHING = 2;
	protected static final int REFRESH_COMPLETE = 3;
	// pull state
	protected static final int PULL_UP_STATE = 100;
	protected static final int PULL_DOWN_STATE = 101;

	protected static final String PULL = "pull";
	protected static final String PUSH = "push";
	/**
	 * last y
	 */
	private int mLastMotionX, mLastMotionY;
	/**
	 * lock
	 */
	// private boolean mLock;
	/**
	 * header view
	 */
	protected View mHeaderView;
	/**
	 * footer view
	 */
	protected View mFooterView;
	/**
	 * list or grid
	 */
	protected AdapterView<?> mAdapterView;
	/**
	 * header view height
	 */
	private int mHeaderViewHeight;
	/**
	 * footer view height
	 */
	private int mFooterViewHeight;
	/**
	 * header view image
	 */
	private ImageView mHeaderImageView;
	/**
	 * footer view image
	 */
	private ImageView mFooterImageView;
	/**
	 * header tip text
	 */
	private TextView mHeaderTextView;
	/**
	 * footer tip text
	 */
	private TextView mFooterTextView;
	/**
	 * header refresh time
	 */
	private TextView mHeaderUpdateTextView;
	/**
	 * footer refresh time
	 */
	// private TextView mFooterUpdateTextView;
	/**
	 * header progress bar
	 */
	private ProgressBar mHeaderProgressBar;
	/**
	 * footer progress bar
	 */
	private ProgressBar mFooterProgressBar;
	/**
	 * header view current state
	 */
	private int mHeaderState;
	/**
	 * footer view current state
	 */
	private int mFooterState;
	/**
	 * pull state,pull up or pull down;PULL_UP_STATE or PULL_DOWN_STATE
	 */
	protected int mPullState;
	/**
	 * 变为向下的箭头,改变箭头方向
	 */
	private RotateAnimation mFlipAnimation;
	/**
	 * 变为逆向的箭头,旋转
	 */
	private RotateAnimation mReverseFlipAnimation;

	protected boolean isShowDefaultHeader = true;

	protected boolean isShowDefaultFooter = true;
	/**
	 * last update time
	 */
	public static String TYPEID = "do_ListView";
	/**
	 * 设置是否支持上拉加载
	 */
	private boolean supportFooterRefresh;

	public void setSupportFooterRefresh(boolean supportFooterRefresh) {
		this.supportFooterRefresh = supportFooterRefresh;
	}

	/**
	 * 设置是否支持下拉刷新
	 * */
	private boolean supportHeaderRefresh;

	public void setSupportHeaderRefresh(boolean supportHeaderRefresh) {
		this.supportHeaderRefresh = supportHeaderRefresh;
	}

	private Context context;

	//是否有滚动效果,值为false不出发scroll事件
	protected boolean mIsSmooth = true;

//	private Scroller mScroller = null;

	private DoPullToRefreshTools mPullToRefreshTools;

	/**
	 * 
	 * @param context
	 * @param supportHeaderRefresh
	 *            是否支持下拉刷新
	 * @param supportFootFooterRefresh
	 *            是否支持上拉加载
	 */
	public DoPullToRefreshView(Context context) {
		super(context);
		this.context = context;
		mPullToRefreshTools = new DoPullToRefreshTools(context);
		init();
	}

	/**
	 * init
	 * 
	 * @param supportHeaderRefresh
	 *            设置是否支持下拉刷新
	 * @param supportFootFooterRefresh
	 *            是否支持上拉加载
	 */
	private void init() {
//		mScroller = new Scroller(context, new DecelerateInterpolator());
		// 需要设置成vertical
		setOrientation(LinearLayout.VERTICAL);
		// Load all of the animations we need in code rather than through XML
		mFlipAnimation = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mFlipAnimation.setInterpolator(new LinearInterpolator());
		mFlipAnimation.setDuration(250);
		mFlipAnimation.setFillAfter(true);
		mReverseFlipAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
		mReverseFlipAnimation.setDuration(250);
		mReverseFlipAnimation.setFillAfter(true);
		// header view 在此添加,保证是第一个添加到linearlayout的最上端
//		addHeaderView();
	}

	private void addHeaderView() {
		// header view
		if (isShowDefaultHeader) {
			mHeaderImageView = mPullToRefreshTools.iv;
			mHeaderTextView = mPullToRefreshTools.tv;
			mHeaderUpdateTextView = mPullToRefreshTools.tv_time;
			mHeaderProgressBar = mPullToRefreshTools.progressBar;
		}
		// header layout
		measureView(mHeaderView);
		mHeaderViewHeight = mHeaderView.getMeasuredHeight();
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, mHeaderViewHeight);
		// 设置topMargin的值为负的header View高度,即将其隐藏在最上方
		params.topMargin = -(mHeaderViewHeight);
		addView(mHeaderView, params);

	}

	protected void setHeaderView(View view) {
		if (view != null) {
			mHeaderView = view;
			isShowDefaultHeader = false;
		} else {
			mHeaderView = mPullToRefreshTools.getPullToRefreshHeaderView(TYPEID);
			isShowDefaultHeader = true;
		}
		addHeaderView();
	}

	private void addFooterView() {
		// footer vie
		if (isShowDefaultFooter) {
			mFooterImageView = mPullToRefreshTools.foot_ImageView;
			mFooterProgressBar = mPullToRefreshTools.foot_ProgressBar;
			mFooterTextView = mPullToRefreshTools.foot_TextView;
		}
		// footer layout
		measureView(mFooterView);
		mFooterViewHeight = mFooterView.getMeasuredHeight();
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, mFooterViewHeight);
		// 当height = -1 时
		params.bottomMargin = -mFooterViewHeight;
		addView(mFooterView, params);
	}

	protected void setFooterView(View view) {
		if (view != null) {
			mFooterView = view;
			isShowDefaultFooter = false;
		} else {
			mFooterView = mPullToRefreshTools.getPullToRefreshViewFoot(TYPEID);
			isShowDefaultFooter = true;
		}
		addFooterView();
	}

	public void setFooterViewGone() {
		mFooterView.setVisibility(GONE);
	}

	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		int y = (int) e.getRawY();
		int x = (int) e.getRawX();
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			onHeaderRefreshComplete(mPullToRefreshTools.formatTime());
			// 首先拦截down事件,记录y坐标
			mLastMotionY = y;
			mLastMotionX = x;
			break;
		case MotionEvent.ACTION_MOVE:
			mIsSmooth = true;
			// deltaY > 0 是向下运动,< 0是向上运动
			int deltaX = x - mLastMotionX;
			int deltaY = y - mLastMotionY;
			boolean isRefresh = isRefreshViewScroll(deltaX, deltaY);
			// 一旦底层View收到touch的action后调用这个方法那么父层View就不会再调用onInterceptTouchEvent了，也无法截获以后的action
			getParent().requestDisallowInterceptTouchEvent(isRefresh);
			if (isRefresh) {
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			break;
		}
		return false;
	}

	/*
	 * 如果在onInterceptTouchEvent()方法中没有拦截(即onInterceptTouchEvent()方法中 return
	 * false)则由PullToRefreshView 的子View来处理;否则由下面的方法来处理(即由PullToRefreshView自己来处理)
	 */
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// if (mLock) {
		// return true;
		// }
		int y = (int) event.getRawY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// onInterceptTouchEvent已经记录
			// mLastMotionY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			mIsSmooth = true;
			int deltaY = y - mLastMotionY;
			if (mPullState == PULL_DOWN_STATE) {// 执行下拉
				if (supportHeaderRefresh)
					headerPrepareToRefresh(deltaY);
				// setHeaderPadding(-mHeaderViewHeight);
			} else if (mPullState == PULL_UP_STATE) {// 执行上拉
				if (supportFooterRefresh)
					footerPrepareToRefresh(deltaY);
			}
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			int topMargin = getHeaderTopMargin();
			if (mPullState == PULL_DOWN_STATE) {
				if (topMargin >= 0) {
					// 开始刷新
					if (supportHeaderRefresh)
						headerRefreshing();
				} else {
					// 还没有执行刷新，重新隐藏
					if (supportHeaderRefresh)
						startScroll(-mHeaderViewHeight, HIDE_HEADER_RESET);
				}
			} else if (mPullState == PULL_UP_STATE) {
				if (Math.abs(topMargin) >= mHeaderViewHeight + mFooterViewHeight) {
					// 开始执行footer 刷新
					if (supportFooterRefresh)
						footerRefreshing();
				} else {
					// 还没有执行刷新，重新隐藏
					if (supportFooterRefresh)
						startScroll(-mHeaderViewHeight, HIDE_FOOTER_RESET);
				}
			}
			break;
		}
		return false;
	}

	/**
	 * 是否应该到了父View,即PullToRefreshView滑动
	 * 
	 * @param deltaY
	 *            , deltaY > 0 是向下运动,< 0是向上运动
	 * @return
	 */
	private boolean isRefreshViewScroll(int deltaX, int deltaY) {
		if (mHeaderState == REFRESHING || mFooterState == REFRESHING) {
			return false;
		}
		// 对于ListView和GridView
		if (mAdapterView != null) {
			// 子view(ListView or GridView)滑动到最顶端
			int angle = (int) (Math.atan2(Math.abs(deltaY), Math.abs(deltaX)) * 100);
			if (angle > 60 && Math.abs(deltaY) > 15) {
				if (deltaY > 0 && supportHeaderRefresh) {

					View child = mAdapterView.getChildAt(0);
					if (child == null) {
						// 如果mAdapterView中没有数据,不拦截
						mPullState = PULL_DOWN_STATE;
						return true;
					}
					if (mAdapterView.getFirstVisiblePosition() == 0 && child.getTop() == 0) {
						mPullState = PULL_DOWN_STATE;
						return true;
					}
					int top = child.getTop();
					int padding = mAdapterView.getPaddingTop();
					if (mAdapterView.getFirstVisiblePosition() == 0 && Math.abs(top - padding) <= 8) {// 这里之前用3可以判断,但现在不行,还没找到原因
						mPullState = PULL_DOWN_STATE;
						return true;
					}

				} else if (deltaY < 0 && supportFooterRefresh) {
					View lastChild = mAdapterView.getChildAt(mAdapterView.getChildCount() - 1);
					if (lastChild == null) {
						// 如果mAdapterView中没有数据,不拦截
						return false;
					}
					// 最后一个子view的Bottom小于父View的高度说明mAdapterView的数据没有填满父view,
					// 等于父View的高度说明mAdapterView已经滑动到最后
					if (lastChild.getBottom() <= getHeight() && mAdapterView.getLastVisiblePosition() == mAdapterView.getCount() - 1) {
						mPullState = PULL_UP_STATE;
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * header 准备刷新,手指移动过程,还没有释放
	 * 
	 * @param deltaY
	 *            ,手指滑动的距离
	 */
	private void headerPrepareToRefresh(int deltaY) {
		int newTopMargin = changingHeaderViewTopMargin(deltaY);
		// 当header view的topMargin>=0时，说明已经完全显示出来了,修改header view 的提示状态
		if (newTopMargin >= 0 && mHeaderState != RELEASE_TO_REFRESH) {
			if (isShowDefaultHeader) {
				mHeaderTextView.setText("松开后刷新");
				mHeaderUpdateTextView.setVisibility(View.VISIBLE);
				mHeaderImageView.clearAnimation();
				mHeaderImageView.startAnimation(mFlipAnimation);
			}
			mHeaderState = RELEASE_TO_REFRESH;
			fireEvent(mHeaderState, newTopMargin, PULL);
		} else if (newTopMargin < 0 && newTopMargin > -mHeaderViewHeight) {// 拖动时没有释放
			if (isShowDefaultHeader) {
				if (mHeaderState != PULL_TO_REFRESH) {
					mHeaderImageView.clearAnimation();
					mHeaderImageView.startAnimation(mReverseFlipAnimation);
				}
				mHeaderTextView.setText("下拉刷新");
			}
			mHeaderState = PULL_TO_REFRESH;
			fireEvent(mHeaderState, newTopMargin, PULL);
		}
	}

	/**
	 * footer 准备刷新,手指移动过程,还没有释放 移动footer view高度同样和移动header view
	 * 高度是一样，都是通过修改header view的topmargin的值来达到
	 * 
	 * @param deltaY
	 *            ,手指滑动的距离
	 */
	private void footerPrepareToRefresh(int deltaY) {
		int newTopMargin = changingHeaderViewTopMargin(deltaY);
		// 如果header view topMargin 的绝对值大于或等于header + footer 的高度
		// 说明footer view 完全显示出来了，修改footer view 的提示状态
		if (Math.abs(newTopMargin) >= (mHeaderViewHeight + mFooterViewHeight) && mFooterState != RELEASE_TO_REFRESH) {
			if (isShowDefaultFooter) {
				mFooterTextView.setText("松开后加载");
				mFooterImageView.clearAnimation();
				mFooterImageView.startAnimation(mFlipAnimation);
			}
			mFooterState = RELEASE_TO_REFRESH;
			fireEvent(mFooterState, newTopMargin, PUSH);
		} else if (Math.abs(newTopMargin) < (mHeaderViewHeight + mFooterViewHeight)) {
			if (isShowDefaultFooter) {
				if (mFooterState != PULL_TO_REFRESH) {
					mFooterImageView.clearAnimation();
					mFooterImageView.startAnimation(mReverseFlipAnimation);
				}
				mFooterTextView.setText("上拉加载更多");
			}
			mFooterState = PULL_TO_REFRESH;
			fireEvent(mFooterState, newTopMargin, PUSH);
		}
	}

	/**
	 * 修改Header view top margin的值
	 * 
	 * @param deltaY
	 */
	private int changingHeaderViewTopMargin(int deltaY) {
		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
		float newTopMargin = params.topMargin + deltaY * 0.3f;
		// 这里对上拉做一下限制,因为当前上拉后然后不释放手指直接下拉,会把下拉刷新给触发了,感谢网友yufengzungzhe的指出
		// 表示如果是在上拉后一段距离,然后直接下拉
		if (deltaY > 0 && mPullState == PULL_UP_STATE && Math.abs(params.topMargin) <= mHeaderViewHeight) {
			return params.topMargin;
		}
		// 同样地,对下拉做一下限制,避免出现跟上拉操作时一样的bug
		if (deltaY < 0 && mPullState == PULL_DOWN_STATE && Math.abs(params.topMargin) >= mHeaderViewHeight) {
			return params.topMargin;
		}
		params.topMargin = (int) newTopMargin;
		mHeaderView.setLayoutParams(params);
		invalidate();
		return params.topMargin;
	}

	/**
	 * header refreshing
	 * 
	 */
	private void headerRefreshing() { //需要显示全部的HeaderView 只有 topMargin > = 0 才会走入
		mHeaderState = REFRESHING;
		startScroll(0, HEADER_REFRESHING);
		if (isShowDefaultHeader) {
			mHeaderImageView.setVisibility(View.GONE);
			mHeaderImageView.clearAnimation();
			mHeaderImageView.setImageDrawable(null);
			mHeaderProgressBar.setVisibility(View.VISIBLE);
			mHeaderTextView.setText("加载中...");
		}
		fireEvent(mHeaderState, 0, PULL);
	}

	/**
	 * footer refreshing
	 * 
	 */
	private void footerRefreshing() {
		mFooterState = REFRESHING;
		int top = mHeaderViewHeight + mFooterViewHeight;
		startScroll(-top, HIDE_FOOTER);
		if (isShowDefaultFooter) {
			mFooterImageView.setVisibility(View.GONE);
			mFooterImageView.clearAnimation();
			mFooterImageView.setImageDrawable(null);
			mFooterProgressBar.setVisibility(View.VISIBLE);
			mFooterTextView.setText("加载中...");
		}
		fireEvent(mFooterState, 0, PUSH);
	}

	/**
	 * 设置header view 的topMargin的值
	 * 
	 * @param topMargin
	 *            ，为0时，说明header view 刚好完全显示出来； 为-mHeaderViewHeight时，说明完全隐藏了
	 */
	private void setHeaderTopMargin(int topMargin) {
		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
		params.topMargin = topMargin;
		mHeaderView.setLayoutParams(params);
		invalidate();
	}

	private void startScroll(int topMargin, int what) {
//		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
//		mScroller.startScroll(0, params.topMargin, 0, topMargin);
		MyHandler mHandler = new MyHandler(topMargin, 20);
		Message msg = mHandler.obtainMessage(what);
		mHandler.sendMessage(msg);
	}

	private void startScroll(int topMargin, int what, int interval) {
//		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
//		mScroller.startScroll(0, params.topMargin, 0, topMargin);
		MyHandler mHandler = new MyHandler(topMargin, interval);
		Message msg = mHandler.obtainMessage(what);
		mHandler.sendMessage(msg);
	}

	private static final int HIDE_HEADER = 1;
	private static final int SHOW_HEADER = 5;
	private static final int HEADER_REFRESHING = 6;
	private static final int HIDE_FOOTER = 2;
	private static final int HIDE_HEADER_RESET = 3;
	private static final int HIDE_FOOTER_RESET = 4;

	@SuppressLint("HandlerLeak")
	private class MyHandler extends Handler {
		private int targetTopMargin;
		private int currentTopMargin;
		private int interval = 20;

		public MyHandler(int _topMargin, int _interval) {
			this.targetTopMargin = _topMargin;
			this.interval = _interval;
			this.currentTopMargin = getHeaderTopMargin();
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HIDE_HEADER:
				currentTopMargin = currentTopMargin - interval;
				setHeaderTopMargin(currentTopMargin);
				if (currentTopMargin > targetTopMargin) {
					if (currentTopMargin - targetTopMargin < interval) {
						interval = currentTopMargin - targetTopMargin;
						if (mHeaderState == REFRESH_COMPLETE) {
							if (isShowDefaultHeader) {
								mHeaderImageView.setVisibility(View.VISIBLE);
								int pulltorefresh_arrow_id = DoResourcesHelper.getIdentifier("pulltorefresh_arrow", "drawable", new DoIModuleTypeID() {
									@Override
									public String getTypeID() {
										return TYPEID;
									}
								});
								mHeaderImageView.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), pulltorefresh_arrow_id));
								mHeaderTextView.setText("下拉刷新");
								mHeaderProgressBar.setVisibility(View.GONE);
							}
							mHeaderState = PULL_TO_REFRESH;
						}
					}
					msg = obtainMessage(HIDE_HEADER);
					sendMessageDelayed(msg, 1);
				}
				break;
			case HEADER_REFRESHING:
				if (currentTopMargin == 0) { //整好显示出HeaderView 就不需要继续下去
					return;
				}
				//表示HeaderView 已经拉出了，并且超过了HeaderView的高度
				if (currentTopMargin < interval) { //当前超过的高度小于interval的大小
					setHeaderTopMargin(0);
					return;
				}
				currentTopMargin = currentTopMargin - interval;
				setHeaderTopMargin(currentTopMargin);
				if (currentTopMargin > targetTopMargin) {
					if (currentTopMargin - targetTopMargin < interval) {
						interval = currentTopMargin - targetTopMargin;
						if (mHeaderState == REFRESH_COMPLETE) {
							if (isShowDefaultHeader) {
								mHeaderImageView.setVisibility(View.VISIBLE);
								int pulltorefresh_arrow_id = DoResourcesHelper.getIdentifier("pulltorefresh_arrow", "drawable", new DoIModuleTypeID() {
									@Override
									public String getTypeID() {
										return TYPEID;
									}
								});
								mHeaderImageView.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), pulltorefresh_arrow_id));
								mHeaderTextView.setText("下拉刷新");
								mHeaderProgressBar.setVisibility(View.GONE);
							}
							mHeaderState = PULL_TO_REFRESH;
						}
					}
					msg = obtainMessage(HEADER_REFRESHING);
					sendMessageDelayed(msg, 1);
				}
				break;
			case HIDE_FOOTER:
				currentTopMargin = currentTopMargin + interval;
				setHeaderTopMargin(currentTopMargin);
				if (currentTopMargin < targetTopMargin) {
					if (targetTopMargin - currentTopMargin < interval) {
						interval = targetTopMargin - currentTopMargin;
						if (mFooterState == REFRESH_COMPLETE) {
							if (isShowDefaultFooter) {
								mFooterImageView.setVisibility(View.VISIBLE);
								int pulltorefresh_arrow_up_id = DoResourcesHelper.getIdentifier("pulltorefresh_arrow_up", "drawable", new DoIModuleTypeID() {
									@Override
									public String getTypeID() {
										return TYPEID;
									}
								});
								mFooterImageView.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), pulltorefresh_arrow_up_id));
								mFooterTextView.setText("上拉加载更多");
								mFooterProgressBar.setVisibility(View.GONE);
							}
							mFooterState = PULL_TO_REFRESH;
						}
					}
					msg = obtainMessage(HIDE_FOOTER);
					sendMessageDelayed(msg, 1);
				}
				break;
			case HIDE_HEADER_RESET: //让头部归位
				setHeaderTopMargin(targetTopMargin);
				break;
			case HIDE_FOOTER_RESET: //让底部归位
				setHeaderTopMargin(targetTopMargin);
				break;
			case SHOW_HEADER:
				if (!supportHeaderRefresh) {
					return;
				}
				if (mPullState == PULL_DOWN_STATE) {// 执行下拉
					headerPrepareToRefresh(interval);
				}
				if (getHeaderTopMargin() < 0) {
					msg = obtainMessage(SHOW_HEADER);
					sendMessageDelayed(msg, 1);
				} else {
					headerRefreshing();
				}
				break;
			}
		}
	}

//	@Override
//	public void computeScroll() {
//		super.computeScroll();
//		if (mScroller.computeScrollOffset()) {
//			scrollTo(mScroller.getCurrX(), 0);
//			invalidate();
//		}
//	}

	/**
	 * header view 完成更新后恢复初始状态
	 * 
	 */
	public void onHeaderRefreshComplete() {
		if (mHeaderState == PULL_TO_REFRESH) {
			return;
		}
		mHeaderState = REFRESH_COMPLETE;
		startScroll(-mHeaderViewHeight, HIDE_HEADER);
	}

	/**
	 * Resets the list to a normal state after a refresh.
	 * 
	 * @param lastUpdated
	 *            Last updated at.
	 */
	public void onHeaderRefreshComplete(CharSequence lastUpdated) {
		setLastUpdated(lastUpdated);
	}

	/**
	 * footer view 完成更新后恢复初始状态
	 */
	public void onFooterRefreshComplete() {
		if (mFooterState == PULL_TO_REFRESH) {
			return;
		}
		mFooterState = REFRESH_COMPLETE;
		startScroll(-mHeaderViewHeight, HIDE_FOOTER);
	}

	/**
	 * Set a text to represent when the list was last updated.
	 * 
	 * @param lastUpdated
	 *            Last updated at.
	 */
	public void setLastUpdated(CharSequence lastUpdated) {
		if (isShowDefaultHeader) {
			if (lastUpdated != null) {
				mHeaderUpdateTextView.setVisibility(View.VISIBLE);
				mHeaderUpdateTextView.setText(lastUpdated);
			} else {
				mHeaderUpdateTextView.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * 获取当前header view 的topMargin
	 * 
	 */
	private int getHeaderTopMargin() {
		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
		return params.topMargin;
	}

	protected void fireEvent(int mHeaderState, int newTopMargin, String eventName) {
		// 由子类实现
	}

	protected void autoRefresh() {
		if (getHeaderTopMargin() == 0) { //当前已经显示了，就不用再显示
			return;
		}
		onHeaderRefreshComplete(mPullToRefreshTools.formatTime());
		mPullState = PULL_DOWN_STATE;
		startScroll(0, SHOW_HEADER, 20);
	}

	public void savaTime(long currentTime) {
		mPullToRefreshTools.savaTime(currentTime);
	}
}
