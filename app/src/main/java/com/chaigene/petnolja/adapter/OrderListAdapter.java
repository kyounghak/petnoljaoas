package com.chaigene.petnolja.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.Order;
import com.chaigene.petnolja.ui.view.InfiniteScrollViewHolder;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.SHOP_TYPE_BUY;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_SELL;
import static com.chaigene.petnolja.model.Order.STATUS_ISSUE_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_ISSUE_REQUEST;
import static com.chaigene.petnolja.model.Order.STATUS_ORDER_CANCEL_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_ORDER_READY;
import static com.chaigene.petnolja.model.Order.STATUS_PAYMENT_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_PAYMENT_REQUEST;
import static com.chaigene.petnolja.model.Order.STATUS_PURCHASE_COMPETE;
import static com.chaigene.petnolja.model.Order.STATUS_SERVICE_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_SERVICE_IN_PROGRESS;
import static com.chaigene.petnolja.model.Order.STATUS_SHIPPING_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_SHIPPING_IN_PROGRESS;
import static com.chaigene.petnolja.model.Order.STATUS_WORK_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_WORK_IN_PROGRESS;

public class OrderListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = "OrderListAdapter";

    private final int VIEW_ITEM = 0;
    private final int VIEW_LOADING = 1;
    private static final int INFINITE_SCROLL_VISIBLE_THRESHOLD = 1;

    private Context mContext;
    private Context mAppContext;

    private int mShopType;
    private List<Order> mOrders;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mManager;

    public OrderListAdapter(Context context, int shopType, List<Order> orders, RecyclerView recyclerView) {
        Log.i(TAG, "NotificationListAdapter");
        this.mContext = context;
        this.mAppContext = context.getApplicationContext();
        this.mShopType = shopType;
        this.mOrders = orders;
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
        return mOrders.get(position) != null ? VIEW_ITEM : VIEW_LOADING;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Log.i(TAG, "onCreateViewHolder");
        RecyclerView.ViewHolder holder = null;
        if (viewType == VIEW_ITEM) {
            if (mShopType == SHOP_TYPE_BUY) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_buy, parent, false);
                holder = new OrderBuyViewHolder(v);
            }
            if (mShopType == SHOP_TYPE_SELL) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_sell, parent, false);
                holder = new OrderSellViewHolder(v);
            }
        } else if (viewType == VIEW_LOADING) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_infinite_scroll_loading, parent, false);
            holder = new InfiniteScrollViewHolder(v);
        }
        return holder;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof OrderBuyViewHolder) {
            OrderBuyViewHolder orderBuyHolder = (OrderBuyViewHolder) holder;
            Glide.with(mContext).clear(orderBuyHolder.ivCoverPhoto);
        }
    }

    // 타입에 따라서 다른 메세지를 출력해야 한다.
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        // Log.i(TAG, "onBindViewHolder");

        // if (!(holder instanceof OrderViewHolder)) return;

        // OrderViewHolder orderHolder = (OrderViewHolder) holder;

        Order order = mOrders.get(position);

        // 구매자
        if (holder instanceof OrderBuyViewHolder) {
            OrderBuyViewHolder orderBuyHolder = (OrderBuyViewHolder) holder;

            downloadPhoto(orderBuyHolder.ivCoverPhoto, order.getProductId(), order.getCoverPhoto());

            orderBuyHolder.tvOrderName.setText(order.getOrderName());

            String totalPrice = CommonUtil.numberFormat(order.getTotalPrice(), "원");
            orderBuyHolder.tvTotalPrice.setText(totalPrice);

            String date = CommonUtil.getFormattedTimeString(order.getOrderTimestamp(), "yyyy.MM.dd");
            orderBuyHolder.tvOrderDate.setText(date);

            String orderStatus = null;
            CommonUtil.hideViews(
                    orderBuyHolder.btnOrderCancel,
                    orderBuyHolder.btnOrderFinalize,
                    orderBuyHolder.btnOrderIssue
            );
            switch (order.getStatus()) {
                case STATUS_ORDER_READY:
                    orderStatus = "주문준비";
                    break;
                case STATUS_PAYMENT_REQUEST:
                    orderStatus = "결제요청";
                    break;
                case STATUS_PAYMENT_COMPLETE:
                    orderStatus = "결제완료";
                    CommonUtil.showViews(orderBuyHolder.btnOrderCancel);
                    break;
                case STATUS_WORK_IN_PROGRESS:
                    orderStatus = "작업중";
                    break;
                case STATUS_WORK_COMPLETE:
                    orderStatus = "작업완료";
                    break;
                case STATUS_SERVICE_IN_PROGRESS:
                    orderStatus = "진행중";
                    break;
                case STATUS_SERVICE_COMPLETE:
                    orderStatus = "전달완료";
                    break;
                case STATUS_SHIPPING_IN_PROGRESS:
                    orderStatus = "전달중";
                    CommonUtil.showViews(orderBuyHolder.btnOrderFinalize, orderBuyHolder.btnOrderIssue);
                    break;
                case STATUS_SHIPPING_COMPLETE:
                    orderStatus = "전달완료";
                    CommonUtil.showViews(orderBuyHolder.btnOrderFinalize, orderBuyHolder.btnOrderIssue);
                    break;
                case STATUS_PURCHASE_COMPETE:
                    orderStatus = "구매완료";
                    break;
                case STATUS_ORDER_CANCEL_COMPLETE:
                    orderStatus = "주문취소";
                    break;
                case STATUS_ISSUE_REQUEST:
                    orderStatus = "환불/교환요청";
                    break;
                case STATUS_ISSUE_COMPLETE:
                    orderStatus = "환불/교환완료";
                    break;
            }
            orderBuyHolder.tvOrderStatus.setText(orderStatus);
        }

        if (holder instanceof OrderSellViewHolder) {
            OrderSellViewHolder orderSellHolder = (OrderSellViewHolder) holder;

            orderSellHolder.tvOrderNo.setText(String.valueOf(order.getOrderNo()));
            orderSellHolder.tvBuyerNickname.setText(order.getBuyerNickname());
            orderSellHolder.tvOrderName.setText(order.getOrderName());

            String orderStatus = null;
            CommonUtil.hideViews(
                    orderSellHolder.btnOrderAccept,
                    orderSellHolder.btnOrderReject,
                    orderSellHolder.btnOrderChat
            );
            switch (order.getStatus()) {
                case STATUS_ORDER_READY:
                    orderStatus = "주문준비";
                    break;
                case STATUS_PAYMENT_REQUEST:
                    orderStatus = "결제요청";
                    break;
                case STATUS_PAYMENT_COMPLETE:
                    orderStatus = "승인대기";
                    CommonUtil.showViews(orderSellHolder.btnOrderAccept, orderSellHolder.btnOrderReject);
                    break;
                case STATUS_WORK_IN_PROGRESS:
                    orderStatus = "작업중";
                    CommonUtil.showViews(orderSellHolder.btnShippingStart);
                    break;
                case STATUS_WORK_COMPLETE:
                    orderStatus = "작업완료";
                    break;
                case STATUS_SERVICE_IN_PROGRESS:
                    orderStatus = "진행중";
                    break;
                case STATUS_SERVICE_COMPLETE:
                    orderStatus = "전달완료";
                    break;
                case STATUS_SHIPPING_IN_PROGRESS:
                    orderStatus = "전달중";
                    break;
                case STATUS_SHIPPING_COMPLETE:
                    orderStatus = "전달완료";
                    break;
                case STATUS_PURCHASE_COMPETE:
                    orderStatus = "구매완료";
                    CommonUtil.showViews(orderSellHolder.btnOrderChat);
                    break;
                case STATUS_ORDER_CANCEL_COMPLETE:
                    orderStatus = "주문취소";
                    break;
                case STATUS_ISSUE_REQUEST:
                    orderStatus = "환불/교환요청";
                    CommonUtil.showViews(orderSellHolder.btnOrderChat);
                    break;
                case STATUS_ISSUE_COMPLETE:
                    orderStatus = "환불/교환완료";
                    break;
            }
            orderSellHolder.tvOrderStatus.setText(orderStatus);

            // 판매자가 판매승인 하기 전까지는 구매취소 가능
            // orderBuyHolder.btnOrderCancel.setVisibility(View.VISIBLE);
        }
    }

    private void downloadPhoto(@NonNull ImageView view,
                               @NonNull String postId,
                               @NonNull String filename) {
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        GlideManager.loadImage(postsRef, view);
    }

    @Override
    public int getItemCount() {
        return mOrders.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class OrderBuyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.item_container)
        ViewGroup vgItemContainer;

        @BindView(R.id.cover_photo)
        ImageView ivCoverPhoto;

        @BindView(R.id.order_name_text)
        TextView tvOrderName;

        @BindView(R.id.total_price_text)
        TextView tvTotalPrice;

        @BindView(R.id.order_date_text)
        TextView tvOrderDate;

        @BindView(R.id.order_status_text)
        TextView tvOrderStatus;

        // Buttons
        @BindView(R.id.order_cancel_button)
        Button btnOrderCancel;

        @BindView(R.id.order_finalize_button)
        Button btnOrderFinalize;

        @BindView(R.id.order_issue_button)
        Button btnOrderIssue;

        @BindView(R.id.chat_button)
        ImageButton btnChat;

        public OrderBuyViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
            btnOrderCancel.setOnClickListener(this);
            btnOrderFinalize.setOnClickListener(this);
            btnOrderIssue.setOnClickListener(this);
            btnChat.setOnClickListener(this);
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
                case R.id.item_container:
                    mOnItemClickListener.onItemClick(mOrders.get(getAdapterPosition()));
                    break;
                case R.id.order_cancel_button:
                    mOnItemClickListener.onItemOrderCancelButtonClick(mOrders.get(getAdapterPosition()));
                    break;
                case R.id.order_finalize_button:
                    mOnItemClickListener.onItemOrderFinalizeButtonClick(mOrders.get(getAdapterPosition()));
                    break;
                case R.id.order_issue_button:
                    mOnItemClickListener.onItemOrderIssueButtonClick(mOrders.get(getAdapterPosition()));
                    break;
                case R.id.chat_button:
                    mOnItemClickListener.onItemChatButtonClick(mOrders.get(getAdapterPosition()));
            }
        }
    }

    public class OrderSellViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.item_container)
        ViewGroup vgItemContainer;

        @BindView(R.id.order_no_text)
        TextView tvOrderNo;

        @BindView(R.id.buyer_nickname_text)
        TextView tvBuyerNickname;

        @BindView(R.id.order_name_text)
        TextView tvOrderName;

        @BindView(R.id.order_status_text)
        TextView tvOrderStatus;

        // Buttons
        @BindView(R.id.order_accept_button)
        Button btnOrderAccept;

        @BindView(R.id.order_reject_button)
        Button btnOrderReject;

        @BindView(R.id.shipping_start_button)
        Button btnShippingStart;

        @BindView(R.id.order_chat_button)
        Button btnOrderChat;

        public OrderSellViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
            btnOrderAccept.setOnClickListener(this);
            btnOrderReject.setOnClickListener(this);
            btnShippingStart.setOnClickListener(this);
            btnOrderChat.setOnClickListener(this);
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
                case R.id.item_container:
                    mOnItemClickListener.onItemClick(mOrders.get(getAdapterPosition()));
                    break;
                case R.id.order_accept_button:
                    mOnItemClickListener.onItemOrderAcceptButtonClick(mOrders.get(getAdapterPosition()));
                    break;
                case R.id.order_reject_button:
                    mOnItemClickListener.onItemOrderRejectButtonClick(mOrders.get(getAdapterPosition()));
                    break;
                case R.id.shipping_start_button:
                    mOnItemClickListener.onItemShippingStartButtonClick(mOrders.get(getAdapterPosition()));
                    break;
                case R.id.order_chat_button:
                    mOnItemClickListener.onItemChatButtonClick(mOrders.get(getAdapterPosition()));
                    break;
            }

        }
    }

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Order order);

        void onItemOrderCancelButtonClick(Order order);

        void onItemOrderAcceptButtonClick(Order order);

        void onItemOrderRejectButtonClick(Order order);

        void onItemShippingStartButtonClick(Order order);

        void onItemOrderFinalizeButtonClick(Order order);

        void onItemOrderIssueButtonClick(Order order);

        void onItemChatButtonClick(Order order);
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
                mOrders.add(null);
                notifyItemInserted(getItemCount() - 1);
                return null;
            }
        });
    }

    public void hideLoading() {
        Log.i(TAG, "hideLoading");
        int loadingIndex = mOrders.indexOf(null);
        if (loadingIndex > 0) {
            mOrders.remove(loadingIndex);
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