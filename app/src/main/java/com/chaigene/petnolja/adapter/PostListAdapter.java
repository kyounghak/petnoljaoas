package com.chaigene.petnolja.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.chaigene.petnolja.model.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.image.glide.targets.ArticleProgressTarget;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.view.CircleProgressBar;
import com.chaigene.petnolja.ui.view.InfiniteScrollViewHolder;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;

import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PostListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = "PostListAdapter";

    private final int VIEW_ITEM = 0;
    private final int VIEW_LOADING = 1;
    private static final int INFINITE_SCROLL_VISIBLE_THRESHOLD = 1;

    private Context mContext;
    private List<Post> mPosts;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mManager;

    public PostListAdapter(Context context, List<Post> posts, RecyclerView recyclerView) {
        Log.i(TAG, "PostListAdapter");

        this.mContext = context;
        this.mPosts = posts;
        this.mRecyclerView = recyclerView;
        this.mManager = (LinearLayoutManager) recyclerView.getLayoutManager();

        setupInfiniteScroll();
    }

    private void setupInfiniteScroll() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (isLastDataReached()) return;

                int totalIndex = mManager.getItemCount() - 1;
                int lastItem = mManager.findLastVisibleItemPosition();
                boolean isDownScroll = dy > 0;

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

    private boolean mLastDataReached;

    public boolean isLastDataReached() {
        return mLastDataReached;
    }

    public void setLastDataReached(boolean lastDataReached) {
        this.mLastDataReached = lastDataReached;
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

    public void showLoading() {
        Log.i(TAG, "showLoading");
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

    private OnLoadMoreListener onLoadMoreListener;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    @Override
    public int getItemViewType(int position) {
        return mPosts.get(position) != null ? VIEW_ITEM : VIEW_LOADING;
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Log.i(TAG, "onCreateViewHolder");
        RecyclerView.ViewHolder holder = null;
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
            holder = new ArticleViewHolder(v);
        } else if (viewType == VIEW_LOADING) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_infinite_scroll_loading, parent, false);
            holder = new InfiniteScrollViewHolder(v);
        }
        return holder;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (!(holder instanceof ArticleViewHolder)) return;
        ArticleViewHolder articleHolder = (ArticleViewHolder) holder;
        Glide.with(mContext).clear(articleHolder.ivProfileImage);
        Glide.with(mContext).clear(articleHolder.ivMainPhoto);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        Log.i(TAG, "onBindViewHolder");

        if (!(holder instanceof ArticleViewHolder)) return;

        ArticleViewHolder articleHolder = (ArticleViewHolder) holder;
        Post post = mPosts.get(position);
        Log.d(TAG, "onBindViewHolder:post:" + post.toMap());

        final String targetUid = post.getUser().getId();

        User user = post.getUser();
        String signature = user.getSignature();
        UserUtil.downloadProfileImage(targetUid, signature, articleHolder.ivProfileImage);

        articleHolder.tvNickname.setText(post.getUser().getNickname());

        // 이미지 최소 높이 지정
        articleHolder.ivMainPhoto.setMinimumHeight(CommonUtil.getScreenWidth(mContext));

        downloadPhoto(
                post.getKey(),
                post.getPhotosList().get(0),
                articleHolder.ivMainPhoto,
                articleHolder.vProgressBar
        );

        // 글 내용 표시
        articleHolder.tvContentText.setText(post.getContent());

        // 시각 표시
        String date = CommonUtil.getTimeAgo(mContext, post.getTimestamp(true));
        articleHolder.tvDateText.setText(date);
    }

    private void downloadPhoto(@NonNull String postId,
                               @NonNull String filename,
                               @NonNull ImageView imageView,
                               @NonNull CircleProgressBar progressView) {
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        ArticleProgressTarget<Bitmap> target = new ArticleProgressTarget<>(
                mContext,
                new BitmapImageViewTarget(imageView),
                imageView,
                progressView
        );
        GlideManager.loadImageWithTarget(
                postsRef,
                null,
                imageView,
                GlideManager.SCALE_TYPE_FIT_CENTER,
                target
        );
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class ArticleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.article_container)
        ViewGroup vgArticleContainer;
        @BindView(R.id.profile_image)
        ImageView ivProfileImage;

        @BindView(R.id.nickname_text)
        TextView tvNickname;

        @BindView(R.id.main_photo)
        ImageView ivMainPhoto;

        @BindView(R.id.progress_bar)
        CircleProgressBar vProgressBar;

        @BindView(R.id.content_text)
        TextView tvContentText;

        @BindView(R.id.date_text)
        TextView tvDateText;

        public ArticleViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            vgArticleContainer.setOnClickListener(this);
            ivProfileImage.setOnClickListener(this);
            tvNickname.setOnClickListener(this);
            ivMainPhoto.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener == null) return;
            switch (v.getId()) {
                case R.id.article_container:
                    mOnItemClickListener.onItemClick(v, getAdapterPosition());
                    break;
                case R.id.profile_image:
                case R.id.nickname_text:
                    int position = getAdapterPosition();
                    Post post = mPosts.get(position);
                    String uid = post.getUser().getId();
                    mOnItemClickListener.onItemNicknameClick(uid, position);
                case R.id.main_photo:
                    mOnItemClickListener.onItemPhotoClick(v, getAdapterPosition());
                    break;
            }
        }
    }

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemNicknameClick(String userId, int position);

        void onItemPhotoClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
    }
}