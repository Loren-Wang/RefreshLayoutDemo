package com.refreshlayout.android.refreshLayout;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by wangliang on 0026/2017/7/26.
 * 创建时间： 0026/2017/7/26 10:11
 * 创建人：王亮（Loren wang）
 * 功能作用：刷新控件的最外层布局，可以做为通用刷新布局基类，只需要对于参数进行设置即可
 * 思路：1、由子类向基类传递三个view视图，分别是头部刷新、中间填充、底部加载视图，同时在传递进入之后需要对头部刷新以及
 *         底部加载视图的高度进行计算并记录，如果无法获得到视图高度那么代表着视图无效
 *      2、阻尼效果实现：实际滑动距离除以3.0可以出现类似于阻尼效果
 *      3、开始刷新、结束刷新、开始加载、结束加载的动画由TimeControlChartAnimation动画控制器控制，有控制器返回已
 *         消耗的时间比例，然后根据时间比例以及需要移动的总体移动距离来进行显示或隐藏的动画效果
 *      4、对于刷新加载状态的方法暴露出去的只有结束功能，开始功能由基类控制，否则的话可能会导致崩溃
 *      5、所有的布局效果都由layout进行子布局完成，视图的总高度总宽度由onMeasure方法获得
 *      6、使用dispatchTouchEvent拦截所有的触摸事件（未过滤多指问题），如果是正在刷新的状态则拦截所有事件，否则就要
 *         在第一次点击的时候记录按下的位置然后在滑动的时候判断中间填充视图是否到达了顶部或者底部，同时再进行刷新加载
 *         视图是否存在，高度是否存在等，如果符合条件则计算阻尼的距离然后传递给layou进行重新布局，最后在手指拿起的时
 *         候进行回弹至刷新刚好显示刷新或加载的位置，同时在回弹后返回开始刷新的回调，最后又调用者调用结束方法，结束刷
 *         新加载，同时回调结束状态，回调成功之后在调用相应的结束方法的抽象方法给子类，让子类更新当前的是否到顶部或者
 *         底部的状态
 *
 * 修改人：
 * 修改时间：
 * 备注：
 */

public abstract class BaseRefreshLayout extends LinearLayout {

    private Context context;
    private static final String TAG = "BaseRefreshLayout";

    private View headRefreshView;//头部刷新视图
    private View footLoadingMoreView;//底部加载更多视图
    private View centerFillView;//中间填充视图

    //布尔值状态判断相关
    private boolean isHeadRefresh = false;//是否正在刷新，默认是未刷新
    private boolean isFootLoadingMore = false;//是否正在加载更多，默认是未加载更多
    private boolean isAllowHeadRefresh = false;//是否允许刷新，默认是不允许
    private boolean isAllowFootLoadingMore = false;//是否允许加载更多，默认是不允许
    private boolean isNotRefreshOrLoadButNeedViewRestoration = false;//不需要刷新或者加载但是需要对view视图进行复位
    private boolean isFirstLayout = true;//是否是第一次布局，为了使界面刚开始显示的时候可以布局，除此之外只有本类中的布局更新才可以改变布局
    private boolean upDataLayout = false;//是否需要更新布局，在每次更新后都需要重置为false，在每次更新前（调用layout前）都需要设置为true

    //数据相关
    private int headRefreshViewHeight = 0;//头部刷新视图高度
    private int footLoadingMoreViewHeight = 0;//底部加载更多视图高度
    private int viewHeight = 0;//视图高度
    private int viewWidth = 0;//视图宽度
    private int headRefreshNowMoveDistance = 0;//刷新时当前滑动的垂直方向的距离
    private int footLoadingMoreMoveDistance = 0;//上拉加载的时候当前滑动的垂直方向的距离
    private RefreshLoadingCallback refreshLoadingCallback;//刷新加载的回调

    //动画相关
    private TimeControlChartAnimation startHeadRefreshAnim;//开始下拉刷新动画计时器
    private TimeControlChartAnimation startFootLoadingMoreAnim;//开始上拉加载更多动画计时器
    private TimeControlChartAnimation finishHeadRefreshAnim;//结束下拉刷新动画计时器
    private TimeControlChartAnimation finishFootLoadingMoreAnim;//结束底部上拉加载更多动画计时器
    private int ANIMATION_TIME = 500;//动画的显示时间

    private Handler handlerToMainThread = new Handler(Looper.getMainLooper());

    public BaseRefreshLayout(Context context) {
        super(context);
        init(context);
    }

    public BaseRefreshLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BaseRefreshLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    /*************************************数据内容初始化模块*****************************************/

    private void init(Context context){
        this.context = context;
        setOrientation(VERTICAL);


        //开始下拉刷新动画计时器
        startHeadRefreshAnim = new TimeControlChartAnimation(this, new TimeControlChartAnimation.SetPercentCallbackListener() {
            @Override
            public void percent(float interpolatedTime) {//interpolatedTime：根据动画的显示时间返回0-1之间的值，未显示为0，显示完全为1
                Log.e(TAG,"startHeadRefreshAnim interpolatedTime :::" + interpolatedTime);
                if(interpolatedTime <= 1 && interpolatedTime >= 0){
                    headRefreshNowMoveDistance = headRefreshViewHeight +  (int) ((headRefreshNowMoveDistance - headRefreshViewHeight) * (1 - interpolatedTime));
                    upDataLayout = true;
                    onLayout(true, getLeft(), headRefreshNowMoveDistance, viewWidth, viewHeight);
                }
                if(interpolatedTime == 1){
                    if(refreshLoadingCallback != null){
                        refreshLoadingCallback.startRefresh();
                    }
                }
            }
        });
        //开始上拉加载更多动画计时器
        startFootLoadingMoreAnim = new TimeControlChartAnimation(this, new TimeControlChartAnimation.SetPercentCallbackListener() {
            @Override
            public void percent(float interpolatedTime) {//interpolatedTime：根据动画的显示时间返回0-1之间的值，未显示为0，显示完全为1
                Log.e(TAG,"startFootLoadingMoreAnim interpolatedTime :::" + interpolatedTime);
                if(interpolatedTime <= 1 && interpolatedTime >= 0){
                    footLoadingMoreMoveDistance = footLoadingMoreViewHeight +  (int) ((footLoadingMoreMoveDistance - footLoadingMoreViewHeight) * (1 - interpolatedTime));
                    upDataLayout = true;
                    onLayout(true, getLeft(), getTop() - footLoadingMoreMoveDistance, viewWidth, viewHeight - footLoadingMoreMoveDistance);
                }
                if(interpolatedTime == 1){
                    if(refreshLoadingCallback != null){
                        refreshLoadingCallback.startLoadingMore();
                    }
                }
            }
        });
        //结束下拉刷新动画计时器
        finishHeadRefreshAnim = new TimeControlChartAnimation(this, new TimeControlChartAnimation.SetPercentCallbackListener() {
            @Override
            public void percent(float interpolatedTime) {//interpolatedTime：根据动画的显示时间返回0-1之间的值，未显示为0，显示完全为1
                Log.e(TAG,"finishHeadRefreshAnim interpolatedTime :::" + interpolatedTime);
                if(interpolatedTime <= 1 && interpolatedTime >= 0 && headRefreshNowMoveDistance != 0){
                    headRefreshNowMoveDistance = (int) (headRefreshNowMoveDistance * (1 - interpolatedTime));
                    upDataLayout = true;
                    onLayout(true, getLeft(), headRefreshNowMoveDistance, viewWidth, viewHeight);
                }
                if(interpolatedTime == 1){
                    headRefreshNowMoveDistance = 0;
                    if(refreshLoadingCallback != null && isHeadRefresh){
                        refreshLoadingCallback.finishRefresh();
                    }
                    isHeadRefresh = false;
                    isNotRefreshOrLoadButNeedViewRestoration = false;
                    finishHeadRefreshAnim();
                }
            }
        });
        //结束底部上拉加载更多动画计时器
        finishFootLoadingMoreAnim = new TimeControlChartAnimation(this, new TimeControlChartAnimation.SetPercentCallbackListener() {
            @Override
            public void percent(float interpolatedTime) {//interpolatedTime：根据动画的显示时间返回0-1之间的值，未显示为0，显示完全为1
                Log.e(TAG,"finishFootLoadingMoreAnim interpolatedTime :::" + interpolatedTime);
                if(interpolatedTime <= 1 && interpolatedTime >= 0){
                    footLoadingMoreMoveDistance = (int) (footLoadingMoreMoveDistance * (1 - interpolatedTime));
                    upDataLayout = true;
                    onLayout(true, getLeft(), getTop() - footLoadingMoreMoveDistance, viewWidth, viewHeight - footLoadingMoreMoveDistance);
                }
                if(interpolatedTime == 1){
                    footLoadingMoreMoveDistance = 0;
                    if(refreshLoadingCallback != null && isFootLoadingMore){
                        refreshLoadingCallback.finishLoadingMore();
                    }
                    isFootLoadingMore = false;
                    isNotRefreshOrLoadButNeedViewRestoration = false;
                    finishFootLoadingMoreAnim();
                }
            }
        });

        startHeadRefreshAnim.setDuration(ANIMATION_TIME);//开始下拉刷新动画计时器
        startFootLoadingMoreAnim.setDuration(ANIMATION_TIME);//开始上拉加载更多动画计时器
        finishHeadRefreshAnim.setDuration(ANIMATION_TIME);//结束下拉刷新动画计时器
        finishFootLoadingMoreAnim.setDuration(ANIMATION_TIME);//结束底部上拉加载更多动画计时器
    }
    //设置头部刷新视图
    public void setHeadRefreshView(View view){
        if(view != null){
            if(headRefreshView != null && getChildAt(0) != null && getChildAt(0).equals(headRefreshView)){
                Log.i(TAG,"remove headRefreshView");
                removeView(headRefreshView);
                headRefreshViewHeight = 0;
            }
            Log.i(TAG,"add headRefreshView");
            headRefreshView = view;
            headRefreshView.measure(0,0);
            int measuredHeight = headRefreshView.getMeasuredHeight();
            int height = headRefreshView.getHeight();
            if(measuredHeight != 0 && height != 0){//两者都不为0取最小值为高度
                headRefreshViewHeight = Math.min(measuredHeight,height);
            }else if(height != 0){
                headRefreshViewHeight = height;
            }else if(measuredHeight != 0){
                headRefreshViewHeight = measuredHeight;
            }else {
                headRefreshViewHeight = 0;
            }
            Log.i(TAG,"headRefreshViewHeight=" + headRefreshViewHeight);
            addView(headRefreshView,0,new LayoutParams(LayoutParams.MATCH_PARENT, headRefreshViewHeight));
        }
    }
    //设置底部加载更多视图
    public void setFootLoadingMoreView(View view){
        if(view != null){
            if(footLoadingMoreView != null && getChildAt(2) != null
                    && getChildAt(2).equals(footLoadingMoreView)){
                Log.i(TAG,"remove footLoadingMoreView");
                removeView(footLoadingMoreView);
                footLoadingMoreViewHeight = 0;
            }
            Log.i(TAG,"add footLoadingMoreView");
            footLoadingMoreView = (view);
            footLoadingMoreView.measure(0,0);
            int measuredHeight = footLoadingMoreView.getMeasuredHeight();
            int height = footLoadingMoreView.getHeight();
            if(measuredHeight != 0 && height != 0){//两者都不为0取最小值为高度
                footLoadingMoreViewHeight = Math.min(measuredHeight,height);
            }else if(height != 0){
                footLoadingMoreViewHeight = height;
            }else if(measuredHeight != 0){
                footLoadingMoreViewHeight = measuredHeight;
            }else {
                footLoadingMoreViewHeight = 0;
            }
            Log.i(TAG,"footLoadingMoreViewHeight=" + footLoadingMoreViewHeight);
            addView(footLoadingMoreView,new LayoutParams(LayoutParams.MATCH_PARENT,footLoadingMoreViewHeight));
        }
    }
    //设置中间填充视图
    protected void setCenterFillView(View view){
        if(view != null){
            if(centerFillView != null && getChildAt(1) != null
                    && getChildAt(1).equals(centerFillView)){
                Log.i(TAG,"remove centerFillView");
                removeView(centerFillView);
            }
            Log.i(TAG,"add centerFillView");
            centerFillView = view;
            if(getChildCount() == 2) {
                addView(centerFillView, 1);
            }else if(getChildCount() == 1 && getChildAt(0) != null && headRefreshView != null && getChildAt(0).equals(headRefreshView)){
                addView(centerFillView);
            }else if(getChildCount() == 1 && getChildAt(0) != null && footLoadingMoreView != null && getChildAt(0).equals(footLoadingMoreView)){
                addView(centerFillView,0);
            }else {
                addView(centerFillView);
            }
        }
    }
    //添加回调
    public void setRefreshLoadingCallback(RefreshLoadingCallback refreshLoadingCallback) {
        this.refreshLoadingCallback = refreshLoadingCallback;
    }

    public boolean isHeadRefresh() {
        return isHeadRefresh;
    }

    public boolean isFootLoadingMore() {
        return isFootLoadingMore;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(getMeasuredHeight() != 0 && getHeight() != 0){
            viewHeight = Math.min(getMeasuredHeight(),getHeight());
        }else if(getMeasuredHeight() != 0){
            viewHeight = getMeasuredHeight();
        }else if(getHeight() != 0){
            viewHeight = getHeight();
        }else {
            viewHeight = 0;
        }
        if(getMeasuredWidth() != 0 && getWidth() != 0){
            viewWidth = Math.min(getMeasuredWidth(),getWidth());
        }else if(getMeasuredHeight() != 0){
            viewWidth = getMeasuredWidth();
        }else if(getHeight() != 0){
            viewWidth = getWidth();
        }else {
            viewWidth = 0;
        }
    }




    /**********************************滑动逻辑以及布局改变控制模块************************************/

    private float downY;
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //正在刷新的时候拦截所有的触摸事件
        if(isHeadRefresh || isFootLoadingMore){
            return true;
        }
        //需要复位
        if(isNotRefreshOrLoadButNeedViewRestoration){
            return true;
        }
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if(isAtTheTop() && isAllowHeadRefresh){
                    if((headRefreshNowMoveDistance == 0 && ev.getY() > downY) || headRefreshNowMoveDistance !=0) {
                        headRefreshNowMoveDistance = (int) ((ev.getY() - downY) * 1 / 3.0);//模仿阻尼效果
                        if(headRefreshViewHeight != 0 && refreshLoadingCallback != null
                                && headRefreshNowMoveDistance <= headRefreshViewHeight) {
                            refreshLoadingCallback.startRefreshPullDownPercent(headRefreshNowMoveDistance * 1.0 / headRefreshViewHeight);
                        }
                        upDataLayout = true;
                        onLayout(true, getLeft(), headRefreshNowMoveDistance, viewWidth, viewHeight);
                        return true;
                    }
                }
                if(isAtTheBottom() && isAllowFootLoadingMore){
                    if((footLoadingMoreMoveDistance == 0 && downY > ev.getY()) || footLoadingMoreMoveDistance !=0) {
                        footLoadingMoreMoveDistance = (int) ((downY - ev.getY()) * 1 / 3.0);//模仿阻尼效果
                        if(footLoadingMoreViewHeight != 0 && refreshLoadingCallback != null
                                && footLoadingMoreMoveDistance <= footLoadingMoreViewHeight) {
                            refreshLoadingCallback.startLoadingMorePullUpPercent(footLoadingMoreMoveDistance * 1.0 / footLoadingMoreViewHeight);
                        }
                        upDataLayout = true;
                        onLayout(true, getLeft(), getTop() - footLoadingMoreMoveDistance, viewWidth, viewHeight - footLoadingMoreMoveDistance);
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                //判断是否要开始刷新
                if(isAtTheTop() && isAllowHeadRefresh && headRefreshNowMoveDistance >= headRefreshViewHeight){
                    startHeadRefresh();
                    return true;
                }
                //判断是否要底部加载更多
                if(isAtTheBottom() && isAllowFootLoadingMore && footLoadingMoreMoveDistance >= footLoadingMoreViewHeight){
                    startFootLoadingMore();
                    return true;
                }
                //判断是否要对顶部刷新复位
                if(headRefreshNowMoveDistance != 0 && !isHeadRefresh){
                    isNotRefreshOrLoadButNeedViewRestoration = true;
                    finishHeadRefresh();
                    return true;
                }
                //判断是否要对底部加载更多复位
                if(footLoadingMoreMoveDistance != 0 && !isFootLoadingMore){
                    isNotRefreshOrLoadButNeedViewRestoration = true;
                    finishFootLoadingMore();
                    return true;
                }

                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (isFirstLayout){
            resetLayout(l,t,r,b);
        }
        if(upDataLayout) {
            isFirstLayout = false;//只有在第一次主动更新的时候才会重置
            upDataLayout = false;
            resetLayout(l,t,r,b);
        }
    }
    //重置布局
    private void resetLayout(int l, int t, int r, int b){
        if (centerFillView != null) {
            centerFillView.layout(l, t, r, b);
        }
        if (headRefreshView != null && isAllowHeadRefresh && isAtTheTop() && headRefreshViewHeight != 0) {
            //t - headRefreshViewHeight:会让刷新界面图像从上向下，headRefreshViewHeight - t :会让刷新界面图像从下向上
            if (t <= headRefreshViewHeight) {
                headRefreshView.layout(l, t - headRefreshViewHeight, r, headRefreshNowMoveDistance);
            } else {
                headRefreshView.layout(l, headRefreshNowMoveDistance - headRefreshViewHeight, r, headRefreshNowMoveDistance);
            }
        }
        if (footLoadingMoreView != null && isAllowFootLoadingMore && isAtTheBottom() && footLoadingMoreViewHeight != 0) {
            if (footLoadingMoreMoveDistance <= footLoadingMoreViewHeight) {
                footLoadingMoreView.layout(l, b, r, b + footLoadingMoreMoveDistance);
            } else {
                footLoadingMoreView.layout(l, b, r, b + footLoadingMoreViewHeight);
            }
        }
    }
    





    /********************************子类重载获取动态变量信息模块*************************************/

    //是否到达中间填充视图的内容最底部
    protected abstract boolean isAtTheBottom();
    //是否到达中间填充视图的内容最顶部
    protected abstract boolean isAtTheTop();
    //结束下拉刷新动画
    protected abstract void finishHeadRefreshAnim();
    //结束底部上拉加载动画
    protected abstract void finishFootLoadingMoreAnim();




    /*************************************刷新加载控制模块*****************************************/

    //结束头部刷新
    public void finishHeadRefresh(){
        if(isAtTheTop() && isAllowHeadRefresh && headRefreshNowMoveDistance != 0) {
            handlerToMainThread.post(new Runnable() {
                @Override
                public void run() {
                    startAnimation(finishHeadRefreshAnim);
                }
            });
        }
    }
    //结束底部加载更多
    public void finishFootLoadingMore(){
        if(isAtTheBottom() && isAllowFootLoadingMore && footLoadingMoreMoveDistance != 0) {
            handlerToMainThread.post(new Runnable() {
                @Override
                public void run() {
                    startAnimation(finishFootLoadingMoreAnim);
                }
            });
        }
    }
    //开始头部刷新(需要先判断中间填充视图是否在器内容的最底部,开启刷新的接口不能开放)
    private void startHeadRefresh(){
        if(isAtTheTop() && isAllowHeadRefresh && headRefreshNowMoveDistance >= headRefreshViewHeight) {
            isHeadRefresh = true;//记录已经开始进行下拉刷新
            startAnimation(startHeadRefreshAnim);
        }
    }
    //开始底部加载更多(需要先判断中间填充视图是否在器内容的最底部,开启加载的接口不能开放)
    private void startFootLoadingMore(){
        if(isAtTheBottom() && isAllowFootLoadingMore && footLoadingMoreMoveDistance >= footLoadingMoreViewHeight) {
            isFootLoadingMore = true;//记录已经开始进行上拉加载
            startAnimation(startFootLoadingMoreAnim);
        }
    }
    //设置是否允许刷新
    public void setAllowHeadRefresh(boolean isAllowHeadRefresh) {
        this.isAllowHeadRefresh = isAllowHeadRefresh;
    }
    //设置是否允许加载更多
    public void setAllowFootLoadingMore(boolean isAllowFootLoadingMore){
        this.isAllowFootLoadingMore = isAllowFootLoadingMore;
    }






}
