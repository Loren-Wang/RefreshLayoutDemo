package com.refreshlayout.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.refreshlayout.android.refreshLayout.RefreshLoadingCallback;
import com.refreshlayout.android.refreshLayout.RefreshRecycleView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RefreshRecycleView test;
    private Context context;
    private LayoutInflater inflater;
    private List<String> list = new ArrayList<>();
    private RefreshRecycleViewAdapter refreshRecycleViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        test = (RefreshRecycleView) findViewById(R.id.test);
        inflater = LayoutInflater.from(context);

        for(int i = 0 ; i < 20 ; i++){
            list.add(String.valueOf(i));
        }
        refreshRecycleViewAdapter = new RefreshRecycleViewAdapter(list);
        test.setAdapter(refreshRecycleViewAdapter);

        test.setRefreshLoadingCallback(new RefreshLoadingCallback() {
            @Override
            public void startRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            test.finishHeadRefresh();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void startLoadingMore() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            test.finishFootLoadingMore();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void finishRefresh() {
                list.clear();
                for (int i = 0; i < 20; i++) {
                    list.add(String.valueOf(i));
                }
                refreshRecycleViewAdapter.setList(list);
            }

            @Override
            public void finishLoadingMore() {
                int size = list.size();
                for (int i = size; i < size + 20; i++) {
                    list.add(String.valueOf(i));
                }
                refreshRecycleViewAdapter.setList(list);
            }

            @Override
            public void startRefreshPullDownPercent(double percent) {

            }

            @Override
            public void startLoadingMorePullUpPercent(double percent) {

            }

        });

    }



    private class RefreshRecycleViewAdapter extends RecyclerView.Adapter<Holder>{

        private List<String> list = new ArrayList<>();

        public RefreshRecycleViewAdapter(List<String> list) {
            this.list = list;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(inflater.inflate(R.layout.item_list_recycleview,null));
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.textView.setText(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void setList(List<String> list) {
            this.list = list;
            notifyDataSetChanged();
        }
    }

    private class Holder extends RecyclerView.ViewHolder {
        private TextView textView;
        public Holder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.textView);
        }
    }
}
