package com.refreshrecycleviewdemo.android.refreshLayout;

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
 * 思路：
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
                    onLayout(true, getLeft(), headRefreshNowMoveDistance, viewWidth, viewHeight);
                }
                if(interpolatedTime == 1){
                    headRefreshNowMoveDistance = 0;
                    isHeadRefresh = false;
                    if(refreshLoadingCallback != null){
                        refreshLoadingCallback.finishRefresh();
                    }
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
                    onLayout(true, getLeft(), getTop() - footLoadingMoreMoveDistance, viewWidth, viewHeight - footLoadingMoreMoveDistance);
                }
                if(interpolatedTime == 1){
                    footLoadingMoreMoveDistance = 0;
                    isFootLoadingMore = false;
                    if(refreshLoadingCallback != null){
                        refreshLoadingCallback.finishLoadingMore();
                    }
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
    protected void setHeadRefreshView(View view){
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
            addView(headRefreshView,0,new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
    }
    //设置底部加载更多视图
    protected void setFootLoadingMoreView(View view){
        if(view != null){
            if(footLoadingMoreView != null && getChildAt(2) != null
                    && getChildAt(2).equals(footLoadingMoreView)){
                Log.i(TAG,"remove footLoadingMoreView");
                removeView(footLoadingMoreView);
                footLoadingMoreViewHeight = 0;
            }
            Log.i(TAG,"add footLoadingMoreView");
            footLoadingMoreView = view;
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
            addView(footLoadingMoreView,new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
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
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if(isAtTheTop()){
                    if((headRefreshNowMoveDistance == 0 && ev.getY() > downY) || headRefreshNowMoveDistance !=0) {
                        headRefreshNowMoveDistance = (int) ((ev.getY() - downY) * 1 / 3.0);//模仿阻尼效果
                        if(headRefreshViewHeight != 0 && refreshLoadingCallback != null
                                && headRefreshNowMoveDistance <= headRefreshViewHeight) {
                            refreshLoadingCallback.startRefreshPullDownPercent(headRefreshNowMoveDistance * 1.0 / headRefreshViewHeight);
                        }
                        onLayout(true, getLeft(), headRefreshNowMoveDistance, viewWidth, viewHeight);
                        return true;
                    }
                }
                if(isAtTheBottom()){
                    if((footLoadingMoreMoveDistance == 0 && downY > ev.getY()) || footLoadingMoreMoveDistance !=0) {
                        footLoadingMoreMoveDistance = (int) ((downY - ev.getY()) * 1 / 3.0);//模仿阻尼效果
                        if(footLoadingMoreViewHeight != 0 && refreshLoadingCallback != null
                                && footLoadingMoreMoveDistance <= footLoadingMoreViewHeight) {
                            refreshLoadingCallback.startRefreshPullDownPercent(footLoadingMoreMoveDistance * 1.0 / footLoadingMoreViewHeight);
                        }
                        onLayout(true, getLeft(), getTop() - footLoadingMoreMoveDistance, viewWidth, viewHeight - footLoadingMoreMoveDistance);
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if(isAtTheTop() && headRefreshNowMoveDistance >= headRefreshViewHeight){
                    startHeadRefresh();
                    return true;
                }
                if(isAtTheBottom() && footLoadingMoreMoveDistance >= footLoadingMoreViewHeight){
                    startFootLoadingMore();
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
        if(centerFillView != null){
            centerFillView.layout(l,t,r,b);
        }
        if(headRefreshView != null && isAllowHeadRefresh && isAtTheTop() && headRefreshViewHeight != 0){
            //t - headRefreshViewHeight:会让刷新界面图像从上向下，headRefreshViewHeight - t :会让刷新界面图像从下向上
            if(t <= headRefreshViewHeight){
                headRefreshView.layout(l,t - headRefreshViewHeight,r,headRefreshNowMoveDistance);
            }else {
                headRefreshView.layout(l,headRefreshNowMoveDistance - headRefreshViewHeight,r,headRefreshNowMoveDistance);
            }
        }
        if(footLoadingMoreView != null && isAllowFootLoadingMore && isAtTheBottom() && footLoadingMoreViewHeight != 0 ) {
            if (footLoadingMoreMoveDistance <= footLoadingMoreViewHeight) {
                footLoadingMoreView.layout(l, b, r, b + footLoadingMoreMoveDistance);
            } else {
                footLoadingMoreView.layout(l, b, r, b + footLoadingMoreViewHeight);
            }
        }
    }





    /********************************子类重载获取动态变量信息模块*************************************/

    //是否到达中间填充视图的内容最底部
    public abstract boolean isAtTheBottom();
    //是否到达中间填充视图的内容最顶部
    public abstract boolean isAtTheTop();
    //结束下拉刷新动画
    public abstract void finishHeadRefreshAnim();
    //结束底部上拉加载动画
    public abstract void finishFootLoadingMoreAnim();




    /*************************************刷新加载控制模块*****************************************/

    //结束头部刷新
    public void finishHeadRefresh(){
        if(isAtTheTop() && headRefreshNowMoveDistance != 0) {
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
        if(isAtTheBottom() && footLoadingMoreMoveDistance != 0) {
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
        if(isAtTheTop() && headRefreshNowMoveDistance >= headRefreshViewHeight) {
            isHeadRefresh = true;//记录已经开始进行下拉刷新
            startAnimation(startHeadRefreshAnim);
        }
    }
    //开始底部加载更多(需要先判断中间填充视图是否在器内容的最底部,开启加载的接口不能开放)
    private void startFootLoadingMore(){
        if(isAtTheBottom() && footLoadingMoreMoveDistance >= footLoadingMoreViewHeight) {
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
