package com.chaigene.petnolja.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.view.InfiniteScrollViewHolder;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;

public class ArticleGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = "ArticleGridAdapter";

    private final int VIEW_ITEM = 0;
    private final int VIEW_LOADING = 1;
    private static final int INFINITE_SCROLL_VISIBLE_THRESHOLD = 1;

    private Context mContext;
    private Context mAppContext;

    private int mArticleType;
    private List<Post> mPosts = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private GridLayoutManager mManager;

    public ArticleGridAdapter(Context context, int articleType, List<Post> posts, RecyclerView recyclerView) {
        // Log.i(TAG, "ArticleGridAdapter");

        this.mContext = context;
        this.mAppContext = context.getApplicationContext();
        this.mArticleType = articleType;
        this.mPosts = posts;
        this.mRecyclerView = recyclerView;
        this.mManager = (GridLayoutManager) recyclerView.getLayoutManager();

        setupInfiniteScroll();
    }

    private void setupInfiniteScroll() {
        mManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return getItemViewType(position) == VIEW_LOADING ? mManager.getSpanCount() : 1;
            }
        });
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            /**
             * Callback method to be invoked when the RecyclerView has been scrolled. This will be
             * called after the scroll has completed.
             * <p>
             * This callback will also be called if visible item range changes after a layout
             * calculation. In that case, dx and dy will be 0.
             *
             * @param recyclerView The RecyclerView which scrolled.
             * @param dx The amount of horizontal scroll.
             * @param dy The amount of vertical scroll.
             */
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // 마지막 데이타에 도달했다면 아에 아무런 작업도 하지 않는다.
                if (isLastDataReached()) return;

                int totalIndex = mManager.getItemCount() - 1;
                int lastItem = mManager.findLastVisibleItemPosition();
                boolean isDownScroll = dy > 0;

                Log.d(TAG, "onScrolled:totalIndex:" + totalIndex + "|lastItem:" + lastItem + "|dy:" + dy);

                // TODO: 일반적으로 10개의 아이템이 있다면 totalCount는 10, firstItem은 0, lastItem은 9가 된다. (모든 아이템이 보인다는 가정 하에)
                // 만약 lastItem이 totalIndex 보다 컸을 때라고 조건을 둔다면 lastItem이 보이기 시작하는 순간부터 콜백이 발생된다.

                // 스크롤을 아래로 내렸을 때
                if (lastItem >= totalIndex & isDownScroll) {
                    Log.d(TAG, "onScrolled:reached:isLoading:" + isLoading());
                    if (!isLoading()) {
                        Log.d(TAG, "onScrolled:reached:SUCCESS");

                        if (onLoadMoreListener != null) {
                            setLoading(true);
                            onLoadMoreListener.onLoadMore();
                        }
                    }
                }
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return mPosts.get(position) != null ? VIEW_ITEM : VIEW_LOADING;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Log.i(TAG, "onCreateViewHolder");
        RecyclerView.ViewHolder holder = null;
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid_explore, parent, false);
            holder = new ArticleViewHolder(v);
        } else if (viewType == VIEW_LOADING) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_infinite_scroll_loading, parent, false);
            holder = new InfiniteScrollViewHolder(v);
        }
        return holder;
    }

    // 이미지뷰가 재사용 되면서 이미지 사이즈가 자동 리사이징 안되는 버그가 있다.
    // Ref: https://github.com/bumptech/glide/issues/710
    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (!(holder instanceof ArticleViewHolder)) return;
        Log.i(TAG, "onViewRecycled:holder:ArticleViewHolder");
        ArticleViewHolder articleHolder = (ArticleViewHolder) holder;
        Glide.with(mContext).clear(articleHolder.ivMainPhoto);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        // Log.i(TAG, "onBindViewHolder");

        if (!(holder instanceof ArticleViewHolder)) return;

        ArticleViewHolder articleHolder = (ArticleViewHolder) holder;
        final Post post = mPosts.get(position);
        // Log.d(TAG, "onBindViewHolder:post:" + post.toMap().toString());

        Map<String, Boolean> regions = post.getRegions();
        String region = null;
        if (!regions.isEmpty()) {
            region = post.getRegions().keySet().iterator().next();
        }
        downloadPhoto(articleHolder.ivMainPhoto, post.getKey(), post.getPhotosList().get(0));

        if (mArticleType == ARTICLE_TYPE_ALL || mArticleType == ARTICLE_TYPE_FEED) {
            articleHolder.vgInformationContainer.setVisibility(View.GONE);
        }

        if (mArticleType == ARTICLE_TYPE_TALENT) {
            articleHolder.tvTitleText.setText(post.getProductTitle());
            // 가격 값이 삽입되어있지 않을 경우 0원으로 처리한다.
            int price = !TextUtils.isEmpty(post.getProductPrice()) ? Integer.parseInt(post.getProductPrice()) : 0;
            String priceText = CommonUtil.numberFormat(String.valueOf(price), "원");
            articleHolder.tvPriceText.setText(priceText);
            articleHolder.vgInformationContainer.setVisibility(View.VISIBLE);
        }
    }

    private void downloadPhoto(@NonNull ImageView view,
                               @NonNull String postId,
                               @NonNull String filename) {
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        GlideManager.loadImage(postsRef, view, GlideManager.SCALE_TYPE_FIT_CENTER);
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position, Post post);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    // Infinite scroll

    // TODO: 2번 호출되어서는 안된다.
    public void showLoading() {
        Log.i(TAG, "showLoading");
        // notifyItemInserted는 동일한 쓰레드에서 실행할 수 없다.
        Tasks.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mPosts.add(null);
                notifyItemInserted(getItemCount() - 1);
                return null;
            }
        });
    }

    public void hideLoading() {
        Log.i(TAG, "hideLoading");
        int loadingIndex = mPosts.indexOf(null);
        if (loadingIndex > 0) {
            mPosts.remove(loadingIndex);
            notifyItemRemoved(loadingIndex);
        }
    }

    private boolean mLoading;

    public synchronized boolean isLoading() {
        return mLoading;
    }

    public synchronized void setLoading(boolean isLoading) {
        Log.i(TAG, "setLoading:" + isLoading);
        mLoading = isLoading;
        if (isLoading) showLoading();
        else hideLoading();
    }

    private OnLoadMoreListener onLoadMoreListener;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private boolean mLastDataReached;

    public boolean isLastDataReached() {
        return mLastDataReached;
    }

    public void setLastDataReached(boolean lastDataReached) {
        this.mLastDataReached = lastDataReached;
    }

    public class ArticleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final String TAG = "ArticleViewHolder";

        @BindView(R.id.main_photo)
        ImageView ivMainPhoto;

        @BindView(R.id.container)
        ViewGroup vgContainer;

        @BindView(R.id.information_container)
        ViewGroup vgInformationContainer;

        @BindView(R.id.title_text)
        TextView tvTitleText;

        @BindView(R.id.price_text)
        TextView tvPriceText;

        public ArticleViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            vgContainer.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener == null) return;
            switch (v.getId()) {
                case R.id.container:
                    Log.i(TAG, "onClick:R.id.container");
                    mOnItemClickListener.onItemClick(getAdapterPosition(), mPosts.get(getAdapterPosition()));
                    break;
                case R.id.main_photo:
                    Log.i(TAG, "onClick:R.id.main_photo");
                    break;
            }
        }
    }

    // Empty view
    private View mEmptyView;
    private boolean mLoadingDataset;
    private RecyclerView.AdapterDataObserver mAdapterDataObserver;

    public View getEmptyView() {
        return mEmptyView;
    }

    public void setEmptyView(View emptyView) {
        this.mEmptyView = emptyView;
        if (mEmptyView != null) {
            setupEmptyView();
        } else {
            releaseEmptyView();
        }
    }

    public boolean isLoadingDataset() {
        return mLoadingDataset;
    }

    public void setLoadingDataset(boolean loadingDataset) {
        this.mLoadingDataset = loadingDataset;
    }

    private void setupEmptyView() {
        mAdapterDataObserver = new RecyclerView.AdapterDataObserver() {
            final String TAG = "AdapterDataObserver";

            @Override
            public void onChanged() {
                Log.i(TAG, "onChanged");
                super.onChanged();
                checkEmpty();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                Log.i(TAG, "onItemRangeInserted");
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                Log.i(TAG, "onItemRangeRemoved");
                checkEmpty();
            }

            void checkEmpty() {
                Log.i(TAG, "checkEmpty:" +
                        "isLoadingDataset:" + isLoadingDataset() + "|" +
                        "isLastDataReached:" + isLastDataReached() + "|" +
                        "itemCount:" + getItemCount());
                boolean isEmpty = !isLoadingDataset() && isLastDataReached() && getItemCount() == 0;
                mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        };
        registerAdapterDataObserver(mAdapterDataObserver);
    }

    private void releaseEmptyView() {
        unregisterAdapterDataObserver(mAdapterDataObserver);
    }
}