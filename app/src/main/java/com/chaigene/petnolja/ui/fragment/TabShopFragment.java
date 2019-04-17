package com.chaigene.petnolja.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.chaigene.petnolja.ui.dialog.DialogConfirmFragment;
import com.chaigene.petnolja.ui.dialog.DialogInputFragment;
import com.chaigene.petnolja.ui.dialog.DialogOrderDetailFragment;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.OrderListAdapter;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.model.Order;
import com.chaigene.petnolja.model.Request;
import com.chaigene.petnolja.ui.activity.ChatActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.ShopUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.CHAT_ACTIVITY;
import static com.chaigene.petnolja.Constants.COUNT_SHOP_ORDER;
import static com.chaigene.petnolja.Constants.EXTRA_ORDER_ID;
import static com.chaigene.petnolja.Constants.EXTRA_SHOP_TYPE;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_BUY;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_SELL;
import static com.chaigene.petnolja.model.Order.ISSUE_CODE_ETC;
import static com.chaigene.petnolja.model.Order.STATUS_ISSUE_REQUEST;
import static com.chaigene.petnolja.model.Order.STATUS_SHIPPING_IN_PROGRESS;

public class TabShopFragment extends TabFragment {
    public static final String TAG = "TabShopFragment";

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private int mShopType;
    private String mTargetOrderId;
    private Order mTargetOrder;
    private List<Order> mOrders;
    private OrderListAdapter mAdapter;
    private LinearLayoutManager mManager;

    private boolean mClickLoading;

    private void setClickLoading(boolean clickLoading) {
        this.mClickLoading = clickLoading;
    }

    private boolean isClickLoading() {
        return this.mClickLoading;
    }

    public static TabShopFragment newInstance(@IntRange(from = SHOP_TYPE_BUY, to = SHOP_TYPE_SELL) int shopType) {
        return newInstance(shopType, null);
    }

    public static TabShopFragment newInstance(@IntRange(from = SHOP_TYPE_BUY, to = SHOP_TYPE_SELL) int shopType, String orderId) {
        Log.i(TAG, "newInstance");

        if (shopType < SHOP_TYPE_BUY || shopType > SHOP_TYPE_SELL)
            throw new IllegalArgumentException("Unexpected article type retrived.");

        TabShopFragment fragment = new TabShopFragment();

        Bundle args = new Bundle();
        args.putInt(EXTRA_SHOP_TYPE, shopType);
        args.putString(EXTRA_ORDER_ID, orderId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        // Log.i(TAG, "readBundle");
        mShopType = bundle.getInt(EXTRA_SHOP_TYPE);
        mTargetOrderId = bundle.getString(EXTRA_ORDER_ID);
        Log.i(TAG, "readBundle:shopType:" + mShopType + "|targetOrderId:" + (mTargetOrderId != null ? mTargetOrderId : "null"));
        /*switch (mShopType) {
            case SHOP_TYPE_BUY:
                break;
            case SHOP_TYPE_SELL:
                break;
        }*/
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
    }

    // onCreate ->

    // onCreateView ->

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        // initOnMessageReceiveListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        // releaseOnMessageReceiveListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_tab_shop, container, false);
        ButterKnife.bind(this, mView);

        initView();

        showLoadingDialog();
        asyncTask().continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "asyncTask:ERROR:" + task.getException().getMessage());
                    return null;
                }
                Log.d(TAG, "asyncTask:SUCCESS");
                return null;
            }
        });
        /*getOrders(true).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "ERROR:");
                    return null;
                }
                initView();
                return null;
            }
        });*/

        return mView;
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        showLoadingDialog();
        getOrders(true).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "ERROR:");
                    return null;
                }
                return null;
            }
        });
    }

    private Task<Void> asyncTask() {
        Log.i(TAG, "asyncTask");
        final Executor executor = Executors.newSingleThreadExecutor();
        return Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (mTargetOrderId != null) {
                    Task<Order> getOrderTask = ShopUtil.getOrder(mTargetOrderId);
                    mTargetOrder = Tasks.await(getOrderTask);
                    Log.d(TAG, "asyncTask:getOrderTask:order:" + mTargetOrder);
                }
                return null;
            }
        }).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                if (mTargetOrderId != null && mTargetOrder == null) {
                    Toast.makeText(getContext(), "해당 주문이 존재하지 않습니다.", Toast.LENGTH_LONG).show();
                    getActivity().finish();
                    return null;
                }
                if (mTargetOrder != null) {
                    DialogOrderDetailFragment orderDetailFragment = DialogOrderDetailFragment.newInstance(
                            mShopType,
                            mTargetOrder,
                            new DialogOrderDetailFragment.OnOrderUpdateListener() {
                                @Override
                                public void onUpdate(Order updatedOrder) {
                                    super.onUpdate(updatedOrder);
                                    for (Order order : mOrders) {
                                        if (order.getId().equals(mTargetOrderId)) {
                                            int index = mOrders.indexOf(order);
                                            mOrders.set(index, updatedOrder);
                                            mAdapter.notifyItemChanged(index);
                                            break;
                                        }
                                    }
                                }
                            }
                    );
                    orderDetailFragment.show(getFragmentManager(), null);
                    mTargetOrderId = null;
                }
                return getOrders(true);
            }
        });
    }

    @Override
    protected void initView() {
        super.initView();
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        Log.i(TAG, "setupRecyclerView");

        mManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mManager);

        // Source: http://stackoverflow.com/a/28828749/4729203
        mRecyclerView.setHasFixedSize(true);
        // false로 하게 되면 하단 부분이 짤리게 된다.
        mRecyclerView.setNestedScrollingEnabled(true);

        // Divider
        DividerItemDecoration divider = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(CommonUtil.getDrawable(getContext(), R.drawable.shape_divider_notification));
        mRecyclerView.addItemDecoration(divider);

        mOrders = new ArrayList<>();
        mAdapter = new OrderListAdapter(getContext(), mShopType, mOrders, mRecyclerView);
        mAdapter.setOnItemClickListener(new OrderListAdapter.OnItemClickListener() {
            final String TAG = "OnItemClickListener";

            @Override
            public void onItemClick(final Order order) {
                Log.i(TAG, "onItemClick");
                DialogOrderDetailFragment orderDetailFragment = DialogOrderDetailFragment.newInstance(
                        mShopType,
                        order,
                        new DialogOrderDetailFragment.OnOrderUpdateListener() {
                            @Override
                            public void onUpdate(Order updatedOrder) {
                                super.onUpdate(updatedOrder);
                                int index = mOrders.indexOf(order);
                                mOrders.set(index, updatedOrder);
                                mAdapter.notifyItemChanged(index);
                            }
                        }
                );
                orderDetailFragment.show(getFragmentManager(), null);
            }

            @Override
            public void onItemOrderCancelButtonClick(final Order order) {
                DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                        "주문을 취소하시겠습니까?",
                        new DialogConfirmFragment.OnSelectListener() {
                            @Override
                            public void onConfirm() {
                                super.onConfirm();
                                showLoadingDialog();
                                ShopUtil.cancelOrder(order).continueWith(new Continuation<Order, Void>() {
                                    @Override
                                    public Void then(@NonNull Task<Order> task) throws Exception {
                                        dismissDialog();
                                        if (!task.isSuccessful()) {
                                            Exception e = task.getException();
                                            Log.w(TAG, "changeStatus:ERROR:" + e.getMessage());
                                            throw e;
                                        }
                                        Order resOrder = task.getResult();
                                        if (resOrder.getResponseStatus() != Request.STATUS_SUCCESS) {
                                            switch (resOrder.getResponseCode()) {
                                                default:
                                                    String errorMessage = resOrder.getResponseMessage() != null ?
                                                            resOrder.getResponseMessage() :
                                                            "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                                                    CommonUtil.showSnackbar(getActivity(), errorMessage);
                                                    break;
                                            }
                                            return null;
                                        }
                                        int itemIndex = mOrders.indexOf(order);
                                        mOrders.set(itemIndex, resOrder);
                                        mAdapter.notifyItemChanged(itemIndex);
                                        return null;
                                    }
                                });
                            }

                            @Override
                            public void onDeny() {
                                super.onDeny();
                            }
                        }
                );
                confirmDialog.show(getFragmentManager(), null);
            }

            @Override
            public void onItemOrderAcceptButtonClick(final Order order) {
                DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                        "구매를 승인하시겠습니까?",
                        new DialogConfirmFragment.OnSelectListener() {
                            @Override
                            public void onConfirm() {
                                super.onConfirm();
                                showLoadingDialog();
                                ShopUtil.changeStatus(Order.STATUS_WORK_IN_PROGRESS, order).continueWith(new Continuation<Order, Void>() {
                                    @Override
                                    public Void then(@NonNull Task<Order> task) throws Exception {
                                        Log.i(TAG, "changeStatus:COMPLETE");
                                        dismissDialog();
                                        if (!task.isSuccessful()) {
                                            Exception e = task.getException();
                                            Log.w(TAG, "changeStatus:ERROR:" + e.getMessage());
                                            throw e;
                                        }
                                        Log.d(TAG, "changeStatus:SUCCESS");
                                        Order newOrder = task.getResult();
                                        Log.w(TAG, "changeStatus:newOrder:" + newOrder.toMap());
                                        int itemIndex = mOrders.indexOf(order);
                                        Log.w(TAG, "changeStatus:itemIndex:" + itemIndex);
                                        mOrders.set(itemIndex, newOrder);
                                        mAdapter.notifyItemChanged(itemIndex);
                                        return null;
                                    }
                                });
                            }

                            @Override
                            public void onDeny() {
                                super.onDeny();
                            }
                        }
                );
                confirmDialog.show(getFragmentManager(), null);
            }

            @Override
            public void onItemOrderRejectButtonClick(final Order order) {
                DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                        "구매를 거절하시겠습니까?",
                        new DialogConfirmFragment.OnSelectListener() {
                            @Override
                            public void onConfirm() {
                                super.onConfirm();
                                showLoadingDialog();
                                ShopUtil.rejectOrder(order).continueWith(new Continuation<Order, Void>() {
                                    @Override
                                    public Void then(@NonNull Task<Order> task) throws Exception {
                                        dismissDialog();
                                        if (!task.isSuccessful()) {
                                            Exception e = task.getException();
                                            Log.w(TAG, "rejectOrder:ERROR:" + e.getMessage());
                                            throw e;
                                        }
                                        Order resOrder = task.getResult();
                                        if (resOrder.getResponseStatus() != Request.STATUS_SUCCESS) {
                                            switch (resOrder.getResponseCode()) {
                                                default:
                                                    String errorMessage = resOrder.getResponseMessage() != null ?
                                                            resOrder.getResponseMessage() :
                                                            "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                                                    CommonUtil.showSnackbar(getActivity(), errorMessage);
                                                    break;
                                            }
                                            return null;
                                        }
                                        int itemIndex = mOrders.indexOf(order);
                                        mOrders.set(itemIndex, resOrder);
                                        mAdapter.notifyItemChanged(itemIndex);
                                        return null;
                                    }
                                });
                            }

                            @Override
                            public void onDeny() {
                                super.onDeny();
                            }
                        }
                );
                confirmDialog.show(getFragmentManager(), null);
            }

            @Override
            public void onItemShippingStartButtonClick(final Order order) {
                DialogInputFragment inputDialog = DialogInputFragment.newInstance(
                        "전달하기",
                        "택배사와 운송장 번호를 입력해주세요.",
                        getResources().getTextArray(R.array.shipping_tracking_carrier_names),
                        new DialogInputFragment.OnSpinnerListener() {
                            @Override
                            public void onItemSelected(int position, CharSequence selected) {
                                super.onItemSelected(position, selected);
                            }
                        },
                        new DialogInputFragment.OnSelectListener() {
                            @Override
                            public void onConfirm(final DialogInputFragment inputDialog) {
                                int shippingCarrierIndex = inputDialog.getSpinnerIndex();
                                final CharSequence[] codes = getResources().getTextArray(R.array.shipping_tracking_carrier_codes);
                                final String shippingCarrier = codes[shippingCarrierIndex].toString();
                                final String shippingTrackingNo = inputDialog.getInputText().toString();
                                Log.i(TAG, "onConfirm:shippingCarrier:" + shippingCarrier + "|shippingTrackingNo:" + shippingTrackingNo);

                                if (shippingCarrierIndex == 0) {
                                    Toast.makeText(getContext(), "택배사를 선택해주세요.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (TextUtils.isEmpty(shippingTrackingNo)) {
                                    Toast.makeText(getContext(), "운송장 번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                inputDialog.getDialog().hide();
                                showLoadingDialog();
                                ShopUtil.ship(order.getId(), shippingCarrier, shippingTrackingNo).continueWith(new Continuation<Void, Void>() {
                                    @Override
                                    public Void then(@NonNull Task<Void> task) throws Exception {
                                        dismissDialog();
                                        if (!task.isSuccessful()) {
                                            Toast.makeText(getContext(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                            inputDialog.getDialog().show();
                                            return null;
                                        }
                                        order.setStatus(STATUS_SHIPPING_IN_PROGRESS);
                                        order.setShippingCarrier(shippingCarrier);
                                        order.setShippingTrackingNo(shippingTrackingNo);
                                        int itemIndex = mOrders.indexOf(order);
                                        mOrders.set(itemIndex, order);
                                        mAdapter.notifyItemChanged(itemIndex);
                                        inputDialog.dismiss();
                                        return null;
                                    }
                                });
                            }

                            @Override
                            public void onDeny() {
                                super.onDeny();
                            }
                        }
                );
                inputDialog.setInputHint("운송장 번호 입력");
                inputDialog.setInputTypeAlphanumeric();
                inputDialog.setInputTypeUppercase();
                inputDialog.setInputTypeSingleline();
                inputDialog.show(getFragmentManager(), null);
            }

            @Override
            public void onItemOrderFinalizeButtonClick(final Order order) {
                DialogInputFragment inputDialog = DialogInputFragment.newInstance(
                        "구매를 결정하시겠습니까?",
                        null,
                        null,
                        new DialogInputFragment.OnSelectListener() {
                            @Override
                            public void onConfirm(final DialogInputFragment inputDialog) {
                                final String review = inputDialog.getInputText();
                                Log.i(TAG, "onConfirm:review:" + review);

                                if (TextUtils.isEmpty(review)) {
                                    Toast.makeText(getContext(), "후기를 입력하셔야 구매 결정을 하실 수 있습니다.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                inputDialog.getDialog().hide();
                                showLoadingDialog();
                                ShopUtil.finalize(order, review).continueWith(new Continuation<Order, Void>() {
                                    @Override
                                    public Void then(@NonNull Task<Order> task) throws Exception {
                                        dismissDialog();
                                        if (!task.isSuccessful()) {
                                            Toast.makeText(getContext(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                            inputDialog.getDialog().show();
                                            return null;
                                        }
                                        Order newOrder = task.getResult();
                                        int itemIndex = mOrders.indexOf(order);
                                        mOrders.set(itemIndex, newOrder);
                                        mAdapter.notifyItemChanged(itemIndex);
                                        inputDialog.dismiss();
                                        return null;
                                    }
                                });
                            }

                            @Override
                            public void onDeny() {
                                super.onDeny();
                            }
                        }
                );
                inputDialog.setInputHint("솔직한 거래후기를 보여주세요.");
                inputDialog.setInputTypeMultiline();
                inputDialog.show(getFragmentManager(), null);
            }

            @Override
            public void onItemOrderIssueButtonClick(final Order order) {
                DialogInputFragment inputDialog = DialogInputFragment.newInstance(
                        "정말로 환불/교환 하시겠습니까?",
                        null,
                        null,
                        new DialogInputFragment.OnSelectListener() {
                            @Override
                            public void onConfirm(final DialogInputFragment inputDialog) {
                                final String message = inputDialog.getInputText();
                                Log.i(TAG, "onConfirm:message:" + message);

                                if (TextUtils.isEmpty(message)) {
                                    Toast.makeText(getContext(), "사유를 입력하셔야 환불/교환 요청을 하실 수 있습니다.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                inputDialog.getDialog().hide();
                                showLoadingDialog();
                                ShopUtil.reportIssue(order.getId(), ISSUE_CODE_ETC, message).continueWith(new Continuation<Void, Void>() {
                                    @Override
                                    public Void then(@NonNull Task<Void> task) throws Exception {
                                        dismissDialog();
                                        if (!task.isSuccessful()) {
                                            Toast.makeText(getContext(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                            inputDialog.getDialog().show();
                                            return null;
                                        }
                                        order.setStatus(STATUS_ISSUE_REQUEST);
                                        order.setIssueCode(ISSUE_CODE_ETC);
                                        order.setIssueMessage(message);
                                        int itemIndex = mOrders.indexOf(order);
                                        mOrders.set(itemIndex, order);
                                        mAdapter.notifyItemChanged(itemIndex);
                                        inputDialog.dismiss();
                                        return null;
                                    }
                                });
                            }

                            @Override
                            public void onDeny() {
                                super.onDeny();
                            }
                        }
                );
                inputDialog.setInputHint("환불 사유를 입력해주세요.");
                inputDialog.setInputTypeMultiline();
                inputDialog.show(getFragmentManager(), null);
            }

            @Override
            public void onItemChatButtonClick(Order order) {
                Log.i(TAG, "onItemChatButtonClick");
                Intent intent = null;
                if (mShopType == SHOP_TYPE_BUY) {
                    intent = ChatActivity.createIntent(getContext(), order.getSellerId());
                }
                if (mShopType == SHOP_TYPE_SELL) {
                    intent = ChatActivity.createIntent(getContext(), order.getBuyerId());
                }
                startActivityForResult(intent, CHAT_ACTIVITY);
            }

            /*@Override
            public void onItemClick(int position, Notification notification) {
                Log.i(TAG, "onItemClick");

                if (isClickLoading()) return;
                setClickLoading(true);

                String notificationId = notification.getKey();
                int notificationType = notification.getType();

                if (!notification.isChecked()) {
                    NotificationUtil.check(notificationId, mArticleType).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(@NonNull Task<Void> task) throws Exception {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "onItemClick:check:ERROR", task.getException());
                                return null;
                            }
                            Log.d(TAG, "onItemClick:check:SUCCESS");
                            return null;
                        }
                    });
                }

                // TODO: 무조건 postId만을 통해서 ArticleFragment를 불러온다.
                switch (notificationType) {
                    case TYPE_LIKE:
                        OldArticleUtil.getPost(notification.getPostId()).continueWith(new Continuation<Post, Void>() {
                            @Override
                            public Void then(@NonNull Task<Post> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "onItemClick:TYPE_LIKE:get_article:ERROR", task.getException());
                                    setClickLoading(false);
                                    return null;
                                }
                                Log.d(TAG, "onItemClick:TYPE_LIKE:get_article:SUCCESS");
                                Post post = task.getResult();
                                startArticleFragment(post);
                                setClickLoading(false);
                                return null;
                            }
                        });
                        break;
                    case TYPE_COMMENT:
                        OldArticleUtil.getPost(notification.getPostId()).continueWith(new Continuation<Post, Void>() {
                            @Override
                            public Void then(@NonNull Task<Post> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "onItemClick:TYPE_COMMENT:get_article:ERROR", task.getException());
                                    setClickLoading(false);
                                    return null;
                                }
                                Log.d(TAG, "onItemClick:TYPE_COMMENT:get_article:SUCCESS");
                                Post post = task.getResult();
                                startArticleFragment(post);
                                setClickLoading(false);
                                return null;
                            }
                        });
                        break;
                    case TYPE_FOLLOW:
                        setClickLoading(false);
                        break;
                    case TYPE_TRADE:
                        setClickLoading(false);
                        break;
                }

            }*/
        });
        mAdapter.setOnLoadMoreListener(new OrderListAdapter.OnLoadMoreListener()

        {
            @Override
            public void onLoadMore() {
                Log.i(TAG, "onLoadMore");

                // 로딩을 보여준다.
                getOrders(false).continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(@NonNull Task<Void> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "onLoadMore:getOrders:ERROR");
                        }
                        mAdapter.setLoading(false);
                        return null;
                    }
                });
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    /*private void startArticleFragment(Post post) {
        ArticleFragment articleFragment = ArticleFragment.newInstance(post);
        ((PageNotificationFragment) getParentFragment()).getRootFragment().add(articleFragment);
    }*/

    private Task<Void> getOrders(boolean isRefresh) {
        Log.i(TAG, "getOrders");

        if (isRefresh) {
            mOrders.clear();
            mAdapter.notifyDataSetChanged();
        }

        DocumentSnapshot cursor = null;
        if (!mOrders.isEmpty()) cursor = mOrders.get(mOrders.size() - 1).getDocumentSnapshot();
        Task<List<Order>> getOrderListTask = ShopUtil.getOrderList(
                mShopType,
                AuthManager.getUserId(),
                COUNT_SHOP_ORDER,
                cursor
        );
        return getOrderListTask.continueWith(new Continuation<List<Order>, Void>() {
            @Override
            public Void then(@NonNull Task<List<Order>> task) throws Exception {
                mAdapter.hideLoading();

                if (!task.isSuccessful()) {
                    // ERROR
                    Exception e = task.getException();
                    Log.w(TAG, "getOrders:ERROR:" + e.getMessage());
                    throw e;
                }

                List<Order> orders = task.getResult();

                if (orders.size() < COUNT_SHOP_ORDER) mAdapter.setLastDataReached(true);

                for (Order order : orders) {
                    Log.d(TAG, "getOrders:order:" +
                            order.getDocumentSnapshot().getId() +
                            "=>" +
                            order.getDocumentSnapshot().getData()
                    );
                    mOrders.add(order);
                    int currentIndex = mOrders.indexOf(order);
                    mAdapter.notifyItemInserted(currentIndex);
                }
                return null;
            }
        });
    }
}