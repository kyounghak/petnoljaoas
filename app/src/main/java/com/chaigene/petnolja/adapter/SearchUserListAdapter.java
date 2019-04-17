package com.chaigene.petnolja.adapter;

import android.content.Context;
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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.ALGUser;
import com.chaigene.petnolja.ui.view.InfiniteScrollViewHolder;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchUserListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = "SearchUserListAdapter";

    private final int VIEW_ITEM = 0;
    private final int VIEW_LOADING = 1;
    private static final int INFINITE_SCROLL_VISIBLE_THRESHOLD = 1;

    private Context mContext;
    private Context mAppContext;

    private List<ALGUser> mSearchUsers;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mManager;

    public SearchUserListAdapter(Context context, List<ALGUser> searchUsers, RecyclerView recyclerView) {
        Log.i(TAG, "NotificationListAdapter");

        this.mContext = context;
        this.mAppContext = context.getApplicationContext();
        this.mSearchUsers = searchUsers;
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

                Log.d(TAG, "onScrolled:totalIndex:" + totalIndex + "/lastItem:" + lastItem + "/isDownScroll:" + isDownScroll);

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
        return mSearchUsers.get(position) != null ? VIEW_ITEM : VIEW_LOADING;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Log.i(TAG, "onCreateViewHolder");

        RecyclerView.ViewHolder holder = null;
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_user, parent, false);
            holder = new SearchUserViewHolder(v);
        } else if (viewType == VIEW_LOADING) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_infinite_scroll_loading, parent, false);
            holder = new InfiniteScrollViewHolder(v);
        }
        return holder;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (!(holder instanceof SearchUserViewHolder)) return;
        SearchUserViewHolder searchHolder = (SearchUserViewHolder) holder;
        Glide.with(mContext).clear(searchHolder.ivProfileImage);
    }

    // 타입에 따라서 다른 메세지를 출력해야 한다.
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        // Log.i(TAG, "onBindViewHolder");

        if (!(holder instanceof SearchUserViewHolder)) return;

        SearchUserViewHolder searchHolder = (SearchUserViewHolder) holder;
        ALGUser algUser = mSearchUsers.get(position);

        downloadProfileImage(searchHolder.ivProfileImage, algUser.getObjectID());

        // 닉네임이 null 일 경우 NullPointerException이 발생한다.
        String nickname = algUser.getNickname();
        searchHolder.tvNicknameText.setText(nickname);

        String description = algUser.getDescription();
        searchHolder.tvDescriptionText.setText(description);
    }

    // TODO: 추후 실제 리전 사용
    private void downloadProfileImage(@NonNull ImageView view, @NonNull String uid) {
        StorageReference postsRef = StorageManager.getUsersRef().child(uid).child(Constants.PROFILE_IMAGE_FILENAME);
        GlideManager.loadImage(postsRef, view);
    }

    @Override
    public int getItemCount() {
        return mSearchUsers.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class SearchUserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.search_user_item_container)
        ViewGroup vgSearchUserItemContainer;

        @BindView(R.id.profile_image)
        ImageView ivProfileImage;

        @BindView(R.id.nickname_text)
        TextView tvNicknameText;

        @BindView(R.id.description_text)
        TextView tvDescriptionText;

        public SearchUserViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
            tvNicknameText.setOnClickListener(this);
            tvDescriptionText.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener == null) {
                Log.d(TAG, "onClick:listener_is_null");
                return;
            }

            String viewId = CommonUtil.getResourceName(v);
            Log.d(TAG, "onClick:viewId:" + viewId);

            mOnItemClickListener.onItemClick(getAdapterPosition(), mSearchUsers.get(getAdapterPosition()));
        }
    }

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position, ALGUser algUser);
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
                mSearchUsers.add(null);
                notifyItemInserted(getItemCount() - 1);
                return null;
            }
        });
    }

    public void hideLoading() {
        Log.i(TAG, "hideLoading");
        int loadingIndex = mSearchUsers.indexOf(null);
        if (loadingIndex > 0) {
            mSearchUsers.remove(loadingIndex);
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
}