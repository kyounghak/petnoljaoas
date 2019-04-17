package com.chaigene.petnolja.adapter;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;

import java.util.List;

public abstract class InfiniteScrollAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    public static final String TAG = "InfiniteScrollRecyclerView";

    private static final int VIEW_ITEM = 1;
    private static final int VIEW_PROG = 0;

    private static final int VISIBLE_THRESHOLD = 1;

    private RecyclerView mRecyclerView;
    private List mItems;

    private boolean mLoading;
    private OnLoadMoreListener onLoadMoreListener;

    public InfiniteScrollAdapter(RecyclerView recyclerView, List items) {
        // mContext = recyclerView.getContext().getApplicationContext();
        mRecyclerView = recyclerView;
        mItems = items;

        if (!(recyclerView.getLayoutManager() instanceof LinearLayoutManager))
            throw new IllegalArgumentException("RecyclerView's layout manager is not LinearLayoutManager.");

        setupScrollListener();
    }

    private void setupScrollListener() {
        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            final String TAG = "OnScrollListener";

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int totalItemCount = linearLayoutManager.getItemCount();
                int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition() + 1;
                int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition() + 1;

                if (totalItemCount <= (lastVisibleItem + VISIBLE_THRESHOLD) && dy > 0) {
                    if (!isLoading()) {
                        if (onLoadMoreListener != null) onLoadMoreListener.onLoadMore();
                        setLoading(true);
                    }
                }

                if (firstVisibleItem <= VISIBLE_THRESHOLD && dy < 0) {
                    Log.d(TAG, "onScrolled:reached:isLoading:" + isLoading());
                    if (!isLoading()) {
                        // 최초 호출 되었을 때는 최초 로드된 데이타 라고 판단하고 무시해버린다.
                        /*if (!isLastDataReached()) {
                            setLastDataReached(true);
                            return;
                        }*/

                        // if (onLoadMoreListener != null) onLoadMoreListener.onLoadMore();
                        // setLoading(true);
                    }
                }
            }
        });
    }

    @Override
    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    public abstract void onBindViewHolder(VH holder, int position);

    @Override
    public abstract int getItemCount();

    // LOADING_PROGRESS
    public synchronized boolean isLoading() {
        return mLoading;
    }

    private synchronized void setLoading(boolean isLoading) {
        mLoading = isLoading;
    }
    // /LOADING_PROGRESS

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }
}
