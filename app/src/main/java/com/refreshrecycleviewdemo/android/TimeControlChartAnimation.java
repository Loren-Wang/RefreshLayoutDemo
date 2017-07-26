package com.refreshrecycleviewdemo.android;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by lenovo on 2016/7/22.
 * 创建人：王亮
 * 功能作用：用来对所要制作的图表的动画的计时操作，避免使用匿名线程对于系统内存的消耗
 */
public class TimeControlChartAnimation extends Animation{
    View view;
    SetPercentCallbackListener setListener;

    public TimeControlChartAnimation(View view, SetPercentCallbackListener setListener) {
        this.view = view;
        this.setListener = setListener;
    }
    int i = 0;

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        view.postInvalidate();
        setListener.percent(interpolatedTime);
    }

    public interface SetPercentCallbackListener{
        void percent(float interpolatedTime);
    }
}
