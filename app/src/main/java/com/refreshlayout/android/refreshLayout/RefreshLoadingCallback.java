package com.refreshlayout.android.refreshLayout;

/**
 * Created by wangliang on 0026/2017/7/26.
 * 创建时间： 0026/2017/7/26 18:02
 * 创建人：王亮（Loren wang）
 * 功能作用：
 * 思路：
 * 修改人：
 * 修改时间：
 * 备注：
 */

public interface RefreshLoadingCallback {
    void startRefresh();
    void startLoadingMore();
    void finishRefresh();
    void finishLoadingMore();
    void startRefreshPullDownPercent(double percent);//下拉刷新下拉的进度，仅相当于所传递的刷新布局的高度（0-1之间的值）
    void startLoadingMorePullUpPercent(double percent);//上拉加载上拉的进度，仅相当于所传递的刷新布局的高度（0-1之间的值）
}
