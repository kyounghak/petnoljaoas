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
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.Notification;
import com.chaigene.petnolja.ui.view.InfiniteScrollViewHolder;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;

import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.SHOP_TYPE_BUY;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_SELL;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_COMMENT;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_FOLLOW;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_FOLLOW_ACCEPT;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_FOLLOW_REQUEST;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_LIKE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_MENTION_ARTICLE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_MENTION_COMMENT;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_SHOP;
import static com.chaigene.petnolja.model.Order.STATUS_ISSUE_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_ISSUE_REQUEST;
import static com.chaigene.petnolja.model.Order.STATUS_ORDER_CANCEL_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_ORDER_REJECT_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_PAYMENT_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_PURCHASE_COMPETE;
import static com.chaigene.petnolja.model.Order.STATUS_SHIPPING_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_SHIPPING_IN_PROGRESS;
import static com.chaigene.petnolja.model.Order.STATUS_WORK_IN_PROGRESS;

public class NotificationListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = "NotificationListAdapter";

    private final int VIEW_ITEM = 0;
    private final int VIEW_LOADING = 1;
    private static final int INFINITE_SCROLL_VISIBLE_THRESHOLD = 1;

    private Context mContext;
    private Context mAppContext;

    private List<Notification> mNotifications;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mManager;

    public NotificationListAdapter(Context context, List<Notification> posts, RecyclerView recyclerView) {
        Log.i(TAG, "NotificationListAdapter");

        this.mContext = context;
        this.mAppContext = context.getApplicationContext();
        this.mNotifications = posts;
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

                Log.d(TAG, "onScrolled:totalIndex:" + totalIndex + "/lastItem:" + lastItem + "/dy:" + dy);

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
        return mNotifications.get(position) != null ? VIEW_ITEM : VIEW_LOADING;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Log.i(TAG, "onCreateViewHolder");

        RecyclerView.ViewHolder holder = null;
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            holder = new NotificationViewHolder(v);
        } else if (viewType == VIEW_LOADING) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_infinite_scroll_loading, parent, false);
            holder = new InfiniteScrollViewHolder(v);
        }
        return holder;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (!(holder instanceof NotificationViewHolder)) return;
        NotificationViewHolder notiHolder = (NotificationViewHolder) holder;
        Glide.with(mContext).clear(notiHolder.ivProfileImage);
        Glide.with(mContext).clear(notiHolder.ivPhoto);
        notiHolder.ivPhoto.setVisibility(View.GONE);
    }

    // 타입에 따라서 다른 메세지를 출력해야 한다.
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        // Log.i(TAG, "onBindViewHolder");

        if (!(holder instanceof NotificationViewHolder)) return;

        NotificationViewHolder notiHolder = (NotificationViewHolder) holder;
        Notification notification = mNotifications.get(position);

        Log.d(TAG, "onBindViewHolder:notification:" + notification.toMap());

        // 닉네임이 null 일 경우 NullPointerException이 발생한다.
        String nickname = notification.getTargetNickname();

        String label;

        UserUtil.downloadProfileImage(notification.getTargetUid(), null, notiHolder.ivProfileImage);

        switch (notification.getType()) {
            case TYPE_LIKE:
                label = mAppContext.getString(R.string.label_notification_like, nickname);
                notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                downloadPhoto(notiHolder.ivPhoto, notification.getPostId(), notification.getPhotoName());
                break;
            case TYPE_COMMENT:
                String comment = CommonUtil.ellipsize(notification.getComment(), 20);
                label = mAppContext.getString(R.string.label_notification_comment, nickname, comment);
                notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                downloadPhoto(notiHolder.ivPhoto, notification.getPostId(), notification.getPhotoName());
                break;
            case TYPE_FOLLOW:
                label = mAppContext.getString(R.string.label_notification_follow, nickname);
                notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                break;
            case TYPE_FOLLOW_REQUEST:
                break;
            case TYPE_FOLLOW_ACCEPT:
                break;
            case TYPE_MENTION_ARTICLE:
                // String mentionContent = notification.getContent().replaceAll(System.getProperty("line.separator"), "");
                String mentionContent = notification.getContent().replaceAll("\\r\\n|\\r|\\n", " ");
                mentionContent = CommonUtil.ellipsize(mentionContent, 20);
                label = CommonUtil.format("%s님이 게시글에서 회원님을 언급했습니다. \"%s\"", nickname, mentionContent);
                notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                downloadPhoto(notiHolder.ivPhoto, notification.getPostId(), notification.getPhotoName());
                break;
            case TYPE_MENTION_COMMENT:
                String mentionComment = CommonUtil.ellipsize(notification.getComment(), 20);
                label = CommonUtil.format("%s님이 댓글에서 회원님을 언급했습니다. \"%s\"", nickname, mentionComment);
                notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                downloadPhoto(notiHolder.ivPhoto, notification.getPostId(), notification.getPhotoName());
                break;
            case TYPE_SHOP:
                Log.d(TAG, "sendNotification:type:TYPE_SHOP|shopType:" + notification.getShopType());

                // 구매자
                if (notification.getShopType() == SHOP_TYPE_BUY) {

                    switch (notification.getOrderStatus()) {
                        case STATUS_WORK_IN_PROGRESS:
                            // orderStatus = "작업중";
                            label = "구매요청이 승인되었습니다.";
                            notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                            downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            break;
                        case STATUS_SHIPPING_IN_PROGRESS:
                            // orderStatus = "전달중";
                            label = "배송이 시작되었습니다.";
                            notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                            downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            break;
                        case STATUS_SHIPPING_COMPLETE:
                            // orderStatus = "전달완료";
                            label = "작품이 전달완료 되었습니다. 작품에 대한 후기를 공유해주세요 :)";
                            notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                            downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            break;
                        case STATUS_ORDER_REJECT_COMPLETE:
                            // orderStatus = "주문거절";
                            label = "구매요청이 거절되었습니다.";
                            notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                            downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            break;
                        case STATUS_ISSUE_COMPLETE:
                            // orderStatus = "환불/교환완료";
                            label = "환불/교환이 완료되었습니다. 확인 후 요청을 철회해주세요 :)";
                            notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                            downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            break;
                        default:
                            // isPhotoExists = false;
                            label = notification.getMessage();
                            break;
                    }
                    notiHolder.tvContentText.setText(label);
                }

                // 판매자
                if (notification.getShopType() == SHOP_TYPE_SELL) {

                    switch (notification.getOrderStatus()) {
                        case STATUS_PAYMENT_COMPLETE:
                            // orderStatus = "결제완료";
                            label = CommonUtil.format(
                                    "%s님이 %s 작품을 1개 구매하셨습니다. 구매를 승인해주세요 :)",
                                    notification.getTargetNickname(),
                                    notification.getProductTitle()
                            );
                            notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                            CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                            notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                            downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            break;
                        case STATUS_SHIPPING_COMPLETE:
                            // orderStatus = "전달완료";
                            label = CommonUtil.format(
                                    "%s님에게 %s 작품이 전달완료 되었습니다.",
                                    notification.getTargetNickname(),
                                    notification.getProductTitle()
                            );
                            notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                            CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                            notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                            downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            break;
                        case STATUS_PURCHASE_COMPETE:
                            // orderStatus = "구매완료";
                            if (!notification.isAutoFinalized()) {
                                label = CommonUtil.format(
                                        "%s님이 %s 작품을 구매결정 하셨습니다.",
                                        notification.getTargetNickname(),
                                        notification.getProductTitle()
                                );
                                notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                                CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                                notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                                downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            } else {
                                label = CommonUtil.format(
                                        "%s님의 %s 작품에 대한 주문이 자동구매결정 되었습니다.",
                                        notification.getTargetNickname(),
                                        notification.getProductTitle()
                                );
                                notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                                CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                                notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                                downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            }
                            break;
                        case STATUS_ORDER_CANCEL_COMPLETE:
                            // orderStatus = "주문취소";
                            label = CommonUtil.format(
                                    "%s님이 %s 작품에 대한 주문을 취소하셨습니다.",
                                    notification.getTargetNickname(),
                                    notification.getProductTitle()
                            );
                            notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                            CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                            notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                            downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            break;
                        case STATUS_ISSUE_REQUEST:
                            // orderStatus = "환불/교환요청";
                            label = CommonUtil.format(
                                    "%s님이 %s 작품을 환불/교환 요청하셨습니다.",
                                    notification.getTargetNickname(),
                                    notification.getProductTitle()
                            );
                            notiHolder.tvContentText.setText(label, TextView.BufferType.SPANNABLE);
                            CommonUtil.appendHighlightText(notiHolder.tvContentText, nickname, null);
                            notiHolder.ivPhoto.setVisibility(View.VISIBLE);
                            downloadPhoto(notiHolder.ivPhoto, notification.getProductId(), notification.getPhotoName());
                            break;
                        default:
                            // isPhotoExists = false;
                            label = notification.getMessage();
                            break;
                    }
                }
                break;
        }

        String date = CommonUtil.getTimeAgo(mContext, notification.getTimestamp(true));
        // Log.d(TAG, "onBindViewHolder:timestamp:" + notification.getTimestamp(true) + "|date:" + date);
        notiHolder.tvDateText.setText(date);
    }

    private void downloadPhoto(@NonNull ImageView view,
                               @NonNull String postId,
                               @NonNull String filename) {
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        GlideManager.loadImage(postsRef, view);
    }

    @Override
    public int getItemCount() {
        return mNotifications.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        @BindView(R.id.notification_item_container)
        ViewGroup vgNotificationItemContainer;

        @BindView(R.id.profile_image)
        ImageView ivProfileImage;

        @BindView(R.id.content_text)
        TextView tvContentText;

        @BindView(R.id.date_text)
        TextView tvDateText;

        @BindView(R.id.photo)
        ImageView ivPhoto;

        public NotificationViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
            tvContentText.setOnClickListener(this);
            tvDateText.setOnClickListener(this);
            ivProfileImage.setOnClickListener(this);
            ivPhoto.setOnClickListener(this);
            // vgNotificationItemContainer.setOnClickListener(this);

            vgNotificationItemContainer.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener == null) {
                Log.d(TAG, "onClick:listener_is_null");
                return;
            }

            String viewId = CommonUtil.getResourceName(v);
            Log.d(TAG, "onClick:viewId:" + viewId);

            switch (v.getId()) {
                case R.id.notification_item_container:
                case R.id.content_text:
                case R.id.date_text:
                    mOnItemClickListener.onItemClick(getAdapterPosition(), mNotifications.get(getAdapterPosition()));
                    break;
                case R.id.profile_image:
                    mOnItemClickListener.onItemProfileImageClick(mNotifications.get(getAdapterPosition()));
                    break;
                case R.id.photo:
                    mOnItemClickListener.onItemPhotoClick(mNotifications.get(getAdapterPosition()));
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mOnItemLongClickListener == null) return false;
            switch (v.getId()) {
                case R.id.notification_item_container:
                    mOnItemLongClickListener.onItemLongClick(getAdapterPosition());
                    return true;
            }
            return false;
        }
    }

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position, Notification notification);

        void onItemProfileImageClick(Notification notification);

        void onItemPhotoClick(Notification notification);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
    }

    private OnItemLongClickListener mOnItemLongClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener l) {
        mOnItemLongClickListener = l;
    }

    // Infinite scroll

    // TODO: 2번 호출되어서는 안된다.
    public void showLoading() {
        Log.i(TAG, "showLoading");
        // notifyItemInserted는 동일한 쓰레드에서 실행할 수 없다.
        Tasks.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mNotifications.add(null);
                notifyItemInserted(getItemCount() - 1);
                return null;
            }
        });
    }

    public void hideLoading() {
        Log.i(TAG, "hideLoading");
        int loadingIndex = mNotifications.indexOf(null);
        if (loadingIndex > 0) {
            mNotifications.remove(loadingIndex);
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