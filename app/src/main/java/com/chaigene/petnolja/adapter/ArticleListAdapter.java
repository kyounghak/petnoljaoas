package com.chaigene.petnolja.adapter;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.chaigene.petnolja.model.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.hashtag.AtSignHelper;
import com.chaigene.petnolja.hashtag.HashTagHelper;
import com.chaigene.petnolja.image.glide.targets.ArticleProgressTarget;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.Comment;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.view.CircleProgressBar;
import com.chaigene.petnolja.ui.view.InfiniteScrollViewHolder;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ArticleListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements HashTagHelper.OnHashTagClickListener, AtSignHelper.OnAtSignClickListener {
    public static final String TAG = "ArticleListAdapter";

    private final int VIEW_ITEM = 0;
    private final int VIEW_LOADING = 1;
    private static final int INFINITE_SCROLL_VISIBLE_THRESHOLD = 1;

    private Context mContext;
    private Context mAppContext;

    private List<Post> mPosts;
    private List<Boolean> loadingComments = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mManager;

    public ArticleListAdapter(Context context, List<Post> posts, RecyclerView recyclerView) {
        Log.i(TAG, "ArticleListAdapter");

        this.mContext = context;
        this.mAppContext = context.getApplicationContext();
        this.mPosts = posts;
        this.mRecyclerView = recyclerView;
        this.mManager = (LinearLayoutManager) recyclerView.getLayoutManager();

        setupInfiniteScroll();
    }

    private void setupInfiniteScroll() {
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

                // Log.d(TAG, "onScrolled:totalIndex:" + totalIndex + "/lastItem:" + lastItem + "/dy:" + dy);

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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
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
        ArticleViewHolder articleHolder = (ArticleViewHolder) holder;
        Glide.with(mContext).clear(articleHolder.ivProfileImage);
        Glide.with(mContext).clear(articleHolder.ivMainPhoto);
        articleHolder.ivFollowBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        Log.i(TAG, "onBindViewHolder");

        if (!(holder instanceof ArticleViewHolder)) return;

        ArticleViewHolder articleHolder = (ArticleViewHolder) holder;
        final Post post = mPosts.get(position);
        Log.d(TAG, "onBindViewHolder:post:" + post.toMap());

        String myUid = AuthManager.getUserId();
        final String targetUid = post.getUser().getId();

        final boolean isMyArticle = targetUid.equals(myUid);

        User user = post.getUser();
        String signature = user.getSignature();
        UserUtil.downloadProfileImage(targetUid, signature, articleHolder.ivProfileImage);

        articleHolder.tvNickname.setText(post.getUser().getNickname());

        /*ViewGroup.LayoutParams params = holder.ivMainPhoto.getLayoutParams();
        params. = customHeight;
        layout.requestLayout();*/

        // 닉네임이 null 일 경우 NullPointerException이 발생한다.
        final String nickname = post.getUser().getNickname();

        // 팔로우 버튼 처리
        if (!targetUid.equals(myUid)) {
            // FIXME
            /*if (post.getUser().isFollowing()) {
                articleHolder.ivFollowBtn.setActivated(true);
            } else {
                articleHolder.ivFollowBtn.setActivated(false);
            }*/
        } else {
            articleHolder.ivFollowBtn.setVisibility(View.GONE);
        }

        // 이미지 최소 높이 지정
        articleHolder.ivMainPhoto.setMinimumHeight(CommonUtil.getScreenWidth(mContext));

        downloadPhoto(
                post.getKey(),
                post.getPhotosList().get(0),
                articleHolder.ivMainPhoto,
                articleHolder.vProgressBar
        );

        // 글 내용 표시
        articleHolder.tvContentText.setText(nickname + " " + post.getContent());
        HashTagHelper hashTagHelper = CommonUtil.createDefaultHashTagHelper(mContext, this);
        hashTagHelper.handle(articleHolder.tvContentText);
        AtSignHelper atSignHelper = CommonUtil.createDefaultAtSignHelper(mContext, this);
        atSignHelper.handle(articleHolder.tvContentText);
        CommonUtil.appendHighlightText(articleHolder.tvContentText, nickname, new ClickableSpan() {
            final String TAG = "ClickableSpan";

            @Override
            public void onClick(View widget) {
                Log.d(TAG, "onClick");
                // TODO: 콜백 이벤트 리스너 구현해야함.
                if (mOnItemClickListener != null)
                    mOnItemClickListener.onItemNicknameClick(targetUid, position);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setUnderlineText(false);
            }
        });

        // 좋아요 버튼
        if (post.getLikes().containsKey(myUid)) {
            articleHolder.ivLikeBtn.setActivated(true);
        } else {
            articleHolder.ivLikeBtn.setActivated(false);
        }
        // 좋아요 갯수
        articleHolder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));

        // 댓글 갯수
        articleHolder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));

        // 세이브 버튼
        if (post.getSaves().containsKey(myUid)) {
            articleHolder.ivSaveBtn.setActivated(true);
        } else {
            articleHolder.ivSaveBtn.setActivated(false);
        }
        // 세이브 갯수
        articleHolder.tvSaveCount.setText(String.valueOf(post.getSaveCount()));

        // 시각 표시
        String date = CommonUtil.getTimeAgo(mContext, post.getTimestamp(true));
        articleHolder.tvDateText.setText(date);

        /*if (isLoadingComments(position)) {
            holder.mLoadingView.setVisibility(View.VISIBLE);
        } else {
            holder.mLoadingView.setVisibility(View.GONE);
        }*/

        List<Comment> comments = post.getLatestComments();
        Log.d(TAG, "onBindViewHolder:comments:" + comments.toString());
        Log.d(TAG, "onBindViewHolder:comments_size:" + comments.size());
        if (post.getCommentCount() > 3) {
            Log.d(TAG, "onBindViewHolder:show_all_comments_is_visible");
            articleHolder.tvShowAllComments.setText(CommonUtil.format(mContext, R.string.show_all_comments, post.getCommentCount()));
            articleHolder.tvShowAllComments.setVisibility(View.VISIBLE);
        } else {
            articleHolder.tvShowAllComments.setText("");
            articleHolder.tvShowAllComments.setVisibility(View.GONE);
        }

        // TODO: 카카오로 로그인 했을 때 댓글 nickname 값이 null이 발생하는 이슈가 있다.
        articleHolder.clearCommentView();
        // TODO: 무조건 가장 마지막 3개의 댓글만 보여줘야 한다.
        // 5개의 댓글이 존재한다면 1, 2번째 댓글은 무시하고 3, 4, 5번째 댓글만 보여줘야 한다.
        int index = 0;
        int visibleIndex = comments.size() - 3;
        for (Comment comment : comments) {
            if (index >= visibleIndex) {
                articleHolder.addCommentView(comment);
            }
            index++;
        }

        final int pos = position;
        articleHolder.ivOptionBtn.setOnClickListener(new View.OnClickListener() {
            final String TAG = "OnItemClickListener";

            @Override
            public void onClick(final View view) {
                Log.i(TAG, "onClick");
                PopupMenu popupMenu = new PopupMenu(mContext, view);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    final String TAG = "OnMenuItemClickListener";

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Log.i(TAG, "onMenuItemClick:position:" + pos);
                        switch (item.getItemId()) {
                            case R.id.action_report:
                                if (mOnItemClickListener != null)
                                    mOnItemClickListener.onItemReportButtonClick(post);
                                break;
                            case R.id.action_message:
                                if (mOnItemClickListener != null)
                                    mOnItemClickListener.onItemMessageButtonClick(holder.itemView, pos);
                                break;
                            case R.id.action_modify:
                                if (mOnItemClickListener != null)
                                    mOnItemClickListener.onItemModifyButtonClick(holder.itemView, pos);
                                break;
                            case R.id.action_delete:
                                if (mOnItemClickListener != null)
                                    mOnItemClickListener.onItemDeleteButtonClick(holder.itemView, pos);
                                break;
                        }
                        return true;
                    }
                });
                // TODO: 나의 글이냐 타인의 글이냐에 따라서 메뉴가 달라져야 함.
                if (isMyArticle) {
                    popupMenu.inflate(R.menu.menu_feed_popup_mine);
                } else {
                    popupMenu.inflate(R.menu.menu_feed_popup_other);
                }

                MenuItem reportMenu = popupMenu.getMenu().findItem(R.id.action_report);
                if (reportMenu != null) {
                    SpannableString s = new SpannableString(reportMenu.getTitle());
                    s.setSpan(new ForegroundColorSpan(CommonUtil.getColor(mContext, R.color.material_red_500)), 0, s.length(), 0);
                    reportMenu.setTitle(s);
                }

                popupMenu.show();
            }
        });
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

    // 문제는 다운로드가 되기 전에는 이미지의 높이 값을 알 수 없다는 점이다.
    /*private void downloadPhoto(@NonNull ImageView view,
                               @NonNull String postId,
                               @Nullable String region,
                               @NonNull String filename) {
        StorageReference postsRef = StorageManager.getArticlePostsRef(region).child(postId).child(filename);
        GlideManager.loadImage(postsRef, view, GlideManager.SCALE_TYPE_FIT_CENTER);
    }*/

    @Deprecated
    // 문제는 다운로드가 되기 전에는 이미지의 높이 값을 알 수 없다는 점이다.
    private void downloadPhoto(@NonNull ImageView view, @NonNull String postId, @NonNull String filename) {
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        GlideManager.loadImage(postsRef, view, GlideManager.SCALE_TYPE_FIT_CENTER);
    }

    @Deprecated
    public synchronized Boolean isLoadingComments(int position) {
        Log.i(TAG, "isLoadingComments:loadingComments:" + this.loadingComments.get(position) + "/position:" + position);
        return loadingComments.get(position);
    }

    @Deprecated
    public synchronized void setLoadingComments(boolean loadingComments, int position) {
        Log.i(TAG, "setLoadingComments:loadingComments:" + loadingComments + "/position:" + position);
        while (this.loadingComments.size() < position) {
            this.loadingComments.add(null);
        }
        this.loadingComments.add(position, loadingComments);
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onHashTagClicked(String hashTag) {
        Log.i(TAG, "onHashTagClicked:hashTag:" + hashTag);
        if (mOnItemClickListener != null) mOnItemClickListener.onItemHashtagClick(hashTag);
    }

    @Override
    public void onAtSignClicked(String atSign) {
        Log.i(TAG, "onAtSignClicked:atSign:" + atSign);
        if (mOnItemClickListener != null) mOnItemClickListener.onItemAtSignClick(atSign);
    }

    public class ArticleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, HashTagHelper.OnHashTagClickListener, AtSignHelper.OnAtSignClickListener {

        @BindView(R.id.article_container)
        ViewGroup vgArticleContainer;

        /*@BindView(R.id.user_profile_image)
        ImageView ivProfileImage;*/

        @BindView(R.id.profile_image)
        ImageView ivProfileImage;

        @BindView(R.id.nickname_text)
        TextView tvNickname;

        @BindView(R.id.follow_button)
        ImageView ivFollowBtn;

        @BindView(R.id.option_button)
        ImageView ivOptionBtn;

        @BindView(R.id.main_photo)
        ImageView ivMainPhoto;

        @BindView(R.id.progress_bar)
        CircleProgressBar vProgressBar;

        @BindView(R.id.like_button_container)
        ViewGroup vgLikeContainer;

        @BindView(R.id.like_button)
        public ImageView ivLikeBtn;

        @BindView(R.id.like_count_text)
        public TextView tvLikeCount;

        @BindView(R.id.comment_button_container)
        ViewGroup vgCommentContainer;

        @BindView(R.id.comment_button)
        ImageView ivCommentBtn;

        @BindView(R.id.comment_count_text)
        TextView tvCommentCount;

        @BindView(R.id.save_button_container)
        ViewGroup vgSaveContainer;

        @BindView(R.id.save_button)
        public ImageView ivSaveBtn;

        @BindView(R.id.save_count_text)
        public TextView tvSaveCount;

        @BindView(R.id.content_text)
        TextView tvContentText;

        @BindView(R.id.date_text)
        TextView tvDateText;

        /*@BindView(R.id.loading_view)
        LoadingView mLoadingView;*/

        @BindView(R.id.show_all_comments)
        TextView tvShowAllComments;

        @BindView(R.id.comment_list_container)
        ViewGroup vgCommentListContainer;

        public ArticleViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            vgArticleContainer.setOnClickListener(this);
            ivProfileImage.setOnClickListener(this);
            tvNickname.setOnClickListener(this);
            ivFollowBtn.setOnClickListener(this);
            ivMainPhoto.setOnClickListener(this);
            vgLikeContainer.setOnClickListener(this);
            ivLikeBtn.setOnClickListener(this);
            vgCommentContainer.setOnClickListener(this);
            ivCommentBtn.setOnClickListener(this);
            vgSaveContainer.setOnClickListener(this);
            ivSaveBtn.setOnClickListener(this);
            tvShowAllComments.setOnClickListener(this);
        }

        private void clearCommentView() {
            vgCommentListContainer.removeAllViews();
        }

        private void addCommentView(final Comment comment) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View commentView = inflater.inflate(R.layout.view_comment, null);
            // ViewGroup vgCommentContainer = ButterKnife.findById(commentView, R.id.comment_container);
            // CircleImageView civUserProfileImage = ButterKnife.findById(commentView, R.id.user_profile_image);
            TextView tvContent = commentView.findViewById(R.id.comment_text);
            // TextView tvDate = ButterKnife.findById(commentView, R.id.date_text);

            HashTagHelper hashTagHelper = CommonUtil.createDefaultHashTagHelper(mContext, this);
            hashTagHelper.handle(tvContent);
            AtSignHelper atSignHelper = CommonUtil.createDefaultAtSignHelper(mContext, this);
            atSignHelper.handle(tvContent);

            CommonUtil.appendHighlightText(tvContent, comment.getNickname(), comment.getContent(), new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    CommonUtil.showSnackbar(widget, comment.getNickname());
                    Log.d(TAG, "click");
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemNicknameClick(comment.getUid(), getAdapterPosition());
                    }
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setUnderlineText(false);
                }
            });
            // tvDate.setVisibility(View.GONE);
            // tvDate.setText(CommonUtil.getTimeAgo(mContext, comment.getTimestamp(true)));
            vgCommentListContainer.addView(commentView);
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
                case R.id.follow_button:
                    mOnItemClickListener.onItemFollowButtonClick(v, getAdapterPosition());
                    break;
                case R.id.main_photo:
                    mOnItemClickListener.onItemPhotoClick(v, getAdapterPosition());
                    break;
                case R.id.like_button_container:
                case R.id.like_button:
                    mOnItemClickListener.onItemLikeButtonClick(ArticleViewHolder.this, mPosts.get(getAdapterPosition()));
                    break;
                case R.id.comment_button_container:
                case R.id.comment_button:
                case R.id.show_all_comments:
                    mOnItemClickListener.onItemCommentButtonClick(v, getAdapterPosition());
                    break;
                case R.id.save_button_container:
                case R.id.save_button:
                    mOnItemClickListener.onItemSaveButtonClick(ArticleViewHolder.this, mPosts.get(getAdapterPosition()));
                    break;
            }
        }

        @Override
        public void onHashTagClicked(String hashTag) {
            Log.i(TAG, "onHashTagClicked:hashTag:" + hashTag);
            if (mOnItemClickListener != null) mOnItemClickListener.onItemHashtagClick(hashTag);
        }

        @Override
        public void onAtSignClicked(String atSign) {
            Log.i(TAG, "onAtSignClicked:atSign:" + atSign);
            if (mOnItemClickListener != null) mOnItemClickListener.onItemAtSignClick(atSign);
        }
    }

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemNicknameClick(String userId, int position);

        void onItemFollowButtonClick(View view, int position);

        void onItemPhotoClick(View view, int position);

        void onItemCommentButtonClick(View view, int position);

        // void onItemLikeButtonClick(View likeIconView, TextView likeCountView, int position);
        void onItemLikeButtonClick(ArticleViewHolder viewHolder, Post post);

        void onItemSaveButtonClick(ArticleViewHolder viewHolder, Post post);

        // void onItemShareButtonClick(View view, int position);

        void onItemReportButtonClick(Post post);

        void onItemMessageButtonClick(View view, int position);

        void onItemModifyButtonClick(View view, int position);

        void onItemDeleteButtonClick(View view, int position);

        void onItemHashtagClick(String hashtag);

        void onItemAtSignClick(String atSign);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
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