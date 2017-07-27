package com.refreshrecycleviewdemo.android.refreshLayout;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.refreshrecycleviewdemo.android.R;
import com.refreshrecycleviewdemo.android.refreshLayout.BaseRefreshLayout;

/**
 * Created by wangliang on 0019/2017/6/19.
 * 创建时间： 0019/2017/6/19 11:21
 * 创建人：王亮（Loren wang）
 * 功能作用：
 * 思路：
 * 修改人：
 * 修改时间：
 * 备注：
 */

public class RefreshRecycleView extends BaseRefreshLayout {
    private RecyclerView recyclerView;
    private boolean isAtTheTop = false;
    private boolean isAtTheBottom = false;
    private LinearLayoutManager linearLayoutManager;

    public RefreshRecycleView(Context context) {
        super(context);
        init(context);
    }

    public RefreshRecycleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RefreshRecycleView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context){
        linearLayoutManager = new LinearLayoutManager(context);
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,500));
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setBackgroundColor(Color.RED);
        setCenterFillView(recyclerView);



        View inflate = LayoutInflater.from(context).inflate(R.layout.head_refresh_layout, null);
        inflate.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,100));
        setHeadRefreshView(inflate);
        setAllowHeadRefresh(true);


        inflate = LayoutInflater.from(context).inflate(R.layout.head_refresh_layout, null);
        inflate.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,100));
        setFootLoadingMoreView(inflate);
        setAllowFootLoadingMore(true);



        //RecyclerView.canScrollVertically(1)的值表示是否能向上滚动，false表示已经滚动到底部
        //RecyclerView.canScrollVertically(-1)的值表示是否能向下滚动，false表示已经滚动到顶部
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
               if(!recyclerView.canScrollVertically(-1) && linearLayoutManager.findFirstVisibleItemPosition() == 0){
                   isAtTheTop = true;
               }else {
                   isAtTheTop = false;
               }
               if(!recyclerView.canScrollVertically(1) && linearLayoutManager.findLastVisibleItemPosition() == recyclerView.getAdapter().getItemCount() - 1){
                   isAtTheBottom = true;
               }else {
                   isAtTheBottom = false;
               }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(!recyclerView.canScrollVertically(-1) && linearLayoutManager.findFirstVisibleItemPosition() == 0){
                    isAtTheTop = true;
                }else {
                    isAtTheTop = false;
                }
                if(!recyclerView.canScrollVertically(1) && linearLayoutManager.findLastVisibleItemPosition() == recyclerView.getAdapter().getItemCount() - 1){
                    isAtTheBottom = true;
                }else {
                    isAtTheBottom = false;
                }
            }
        });

    }

    private void refreshTopBottomStates(LinearLayoutManager linearLayoutManager) {
        if(linearLayoutManager != null) {
            if (!recyclerView.canScrollVertically(-1) && linearLayoutManager.findFirstVisibleItemPosition() == 0) {
                isAtTheTop = true;
            } else {
                isAtTheTop = false;
            }
            if (!recyclerView.canScrollVertically(1) && linearLayoutManager.findLastVisibleItemPosition() == recyclerView.getAdapter().getItemCount() - 1) {
                isAtTheBottom = true;
            } else {
                isAtTheBottom = false;
            }
        }
    }

    public void setAdapter(RecyclerView.Adapter adapter){
        if(recyclerView != null && adapter != null){
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public boolean isAtTheBottom() {
        return isAtTheBottom;
    }

    @Override
    public boolean isAtTheTop() {
        return isAtTheTop;
    }

    @Override
    public void finishHeadRefreshAnim() {
        refreshTopBottomStates(linearLayoutManager);
    }

    @Override
    public void finishFootLoadingMoreAnim() {
        refreshTopBottomStates(linearLayoutManager);
    }


}
