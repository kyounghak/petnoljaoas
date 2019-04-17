package com.chaigene.petnolja.ui.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.chaigene.petnolja.BuildConfig;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.Order;
import com.chaigene.petnolja.model.PrivateInfo;
import com.chaigene.petnolja.model.Request;
import com.chaigene.petnolja.ui.activity.ChatActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.ShopUtil;
import com.chaigene.petnolja.util.UserUtil;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.CHAT_ACTIVITY;
import static com.chaigene.petnolja.Constants.EXTRA_SHOP_TYPE;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_BUY;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_SELL;
import static com.chaigene.petnolja.model.Order.ISSUE_CODE_ETC;
import static com.chaigene.petnolja.model.Order.STATUS_ISSUE_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_ISSUE_REQUEST;
import static com.chaigene.petnolja.model.Order.STATUS_ORDER_CANCEL_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_ORDER_READY;
import static com.chaigene.petnolja.model.Order.STATUS_ORDER_REJECT_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_PAYMENT_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_PAYMENT_REQUEST;
import static com.chaigene.petnolja.model.Order.STATUS_PURCHASE_COMPETE;
import static com.chaigene.petnolja.model.Order.STATUS_SERVICE_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_SERVICE_IN_PROGRESS;
import static com.chaigene.petnolja.model.Order.STATUS_SHIPPING_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_SHIPPING_IN_PROGRESS;
import static com.chaigene.petnolja.model.Order.STATUS_WORK_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_WORK_IN_PROGRESS;

public class DialogOrderDetailFragment extends AppCompatDialogFragment {
    public static final String TAG = "DialogOrderDetailFrag";

    private static String EXTRA_TARGET_ORDER = "extra_target_order";
    private static String EXTRA_LISTENER = "extra_listener";
    // private static String EXTRA_HIGHLIGHT_CONTENT = "extra_highlight_content";
    // private static String EXTRA_CONTENT = "extra_content";
    // private static String EXTRA_LISTENER = "extra_listener";

    @BindView(R.id.dialog_frame)
    FrameLayout mVgDialogFrame;

    @BindView(R.id.dialog_container)
    ViewGroup mVgDialogContainer;

    @BindView(R.id.loading_view)
    LottieAnimationView mLoadingView;

    @BindView(R.id.order_name_text)
    TextView mTvOrderName;

    @BindView(R.id.cover_photo)
    ImageView mIvCoverPhoto;

    @BindView(R.id.order_status_text)
    TextView mTvOrderStatus;

    // 구매자
    @BindView(R.id.order_cancel_button)
    Button mBtnOrderCancel;

    @BindView(R.id.order_finalize_button)
    Button mBtnOrderFinalize;

    @BindView(R.id.order_report_issue_button)
    Button mBtnOrderReportIssue;

    @BindView(R.id.order_cancel_issue_button)
    Button mBtnOrderCancelIssue;

    @BindView(R.id.order_resolve_issue_button)
    Button mBtnOrderResolveIssue;

    // 판매자
    @BindView(R.id.order_accept_button)
    Button mBtnOrderAccept;

    @BindView(R.id.order_reject_button)
    Button mBtnOrderReject;

    @BindView(R.id.order_chat_button)
    Button mBtnOrderChat;

    @BindView(R.id.shipping_start_button)
    Button mBtnShippingStart;

    // 디버그용
    /*@BindView(R.id.shipping_complete_button)
    Button mBtnShippingComplete;*/

    // Deprecated
    /*@BindView(R.id.shipping_edit_button)
    Button mBtnShippingEdit;

    @BindView(R.id.shipping_carrier_text)
    TextView mTvShippingCarrier;

    @BindView(R.id.shipping_tracking_no_text)
    TextView mTvShippingTrackingNo;*/

    @BindView(R.id.issue_container)
    ViewGroup mVgIssueContainer;

    @BindView(R.id.issue_message_text)
    TextView mTvIssueMessage;

    @BindView(R.id.info_order_date_text)
    TextView mTvInfoOrderDate;

    @BindView(R.id.info_order_no_text)
    TextView mTvInfoOrderNo;

    @BindView(R.id.info_seller_text)
    TextView mTvInfoSeller;

    @BindView(R.id.info_quantity_text)
    TextView mTvInfoQuantity;

    @BindView(R.id.info_subtotal_price_text)
    TextView mTvInfoSubtotalPrice;

    @BindView(R.id.info_shipping_price)
    TextView mTvInfoShippingPrice;

    @BindView(R.id.payment_method_text)
    TextView mTvPaymentMethod;

    @BindView(R.id.payment_total_price_text)
    TextView mTvPaymentTotalPrice;

    @BindView(R.id.shipping_username_text)
    TextView mTvShippingUsername;

    @BindView(R.id.shipping_phone_text)
    TextView mTvShippingPhone;

    @BindView(R.id.shipping_address_text)
    TextView mTvShippingAddress;

    @BindView(R.id.shipping_message_text)
    TextView mTvShippingMessage;

    @BindView(R.id.orderer_nickname_text)
    TextView mTvOrdererNickname;

    @BindView(R.id.orderer_phone_text)
    TextView mTvOrdererPhone;

    /*@BindView(R.id.shipping_seller_container)
    ViewGroup mVgShippingSellerContainer;*/

    @BindView(R.id.shipping_edit_button)
    Button mBtnShippingEdit;

    @BindView(R.id.shipping_carrier_text)
    TextView mTvShippingCarrier;

    @BindView(R.id.shipping_tracking_no_text)
    TextView mTvShippingTrackingNo;

    private View mView;

    private int mShopType;
    private Order mTargetOrder;
    // private String mHighlightContent;
    // private String mContent;
    // private OnButtonSelectListener mOnButtonSelectListener;

    private OnUpdateListener mOnUpdateListener;

    private PrivateInfo mBuyerPrivateInfo;
    private PrivateInfo mSellerPrivateInfo;

    public static DialogOrderDetailFragment newInstance(int shopType, @NonNull Order order, OnOrderUpdateListener l) {
        Log.i(TAG, "newInstance");
        DialogOrderDetailFragment dialogConfirmFragment = new DialogOrderDetailFragment();

        Bundle args = new Bundle();
        args.putInt(EXTRA_SHOP_TYPE, shopType);
        args.putSerializable(EXTRA_TARGET_ORDER, order);
        args.putParcelable(EXTRA_LISTENER, l);
        dialogConfirmFragment.setArguments(args);

        // 백그라운드 커스터마이징
        // confirmDialogFragment.setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Panel);

        return dialogConfirmFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readBundle(getArguments());


    }

    protected void readBundle(@Nullable Bundle bundle) {
        Log.i(TAG, "readBundle");
        mShopType = bundle.getInt(EXTRA_SHOP_TYPE);
        mTargetOrder = (Order) bundle.getSerializable(EXTRA_TARGET_ORDER);
        mOnUpdateListener = bundle.getParcelable(EXTRA_LISTENER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_dialog_order_detail, container, false);
        ButterKnife.bind(this, mView);

        showLoadingDialog();
        asyncTask().continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    // ERROR
                    return null;
                }
                initView();
                return null;
            }
        });

        return mView;
    }

    private Task<Void> asyncTask() {
        // TODO: UserUtil.getPrivateInfo()를 통해 seller의 정보를 가져온다.
        mBuyerPrivateInfo = null;
        mSellerPrivateInfo = null;

        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Task<PrivateInfo> getBuyerPrivateInfoTask = UserUtil.getPrivateInfo(mTargetOrder.getBuyerId());
                mBuyerPrivateInfo = Tasks.await(getBuyerPrivateInfoTask);

                if (!getBuyerPrivateInfoTask.isSuccessful()) {
                    Exception getBuyerPrivateInfoError = getBuyerPrivateInfoTask.getException();
                    throw getBuyerPrivateInfoError;
                }

                Task<PrivateInfo> getSellerPrivateInfoTask = UserUtil.getPrivateInfo(mTargetOrder.getSellerId());
                mSellerPrivateInfo = Tasks.await(getSellerPrivateInfoTask);

                if (!getSellerPrivateInfoTask.isSuccessful()) {
                    Exception getSellerPrivateInfoError = getSellerPrivateInfoTask.getException();
                    throw getSellerPrivateInfoError;
                }

                return null;
            }
        });
    }

    private void initView() {
        Log.i(TAG, "initView");

        mTvOrderName.setText(mTargetOrder.getOrderName());

        downloadPhoto(
                mIvCoverPhoto,
                mTargetOrder.getProductId(),
                mTargetOrder.getCoverPhoto()
        );

        String orderStatus = null;

        // 구매자
        if (mShopType == SHOP_TYPE_BUY) {
            // 초기화
            CommonUtil.hideViews(
                    mBtnOrderCancel,
                    mBtnOrderAccept,
                    mBtnOrderReject,
                    mBtnShippingStart,
                    mBtnOrderFinalize,
                    mBtnOrderReportIssue,
                    mBtnOrderCancelIssue
            );
            switch (mTargetOrder.getStatus()) {
                case STATUS_ORDER_READY:
                    orderStatus = "주문준비";
                    break;
                case STATUS_PAYMENT_REQUEST:
                    orderStatus = "결제요청";
                    break;
                case STATUS_PAYMENT_COMPLETE:
                    orderStatus = "결제완료";
                    CommonUtil.showViews(mBtnOrderCancel);
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
                    CommonUtil.showViews(mBtnOrderFinalize, mBtnOrderReportIssue);
                    break;
                case STATUS_SHIPPING_COMPLETE:
                    orderStatus = "전달완료";
                    CommonUtil.showViews(mBtnOrderFinalize, mBtnOrderReportIssue);
                    break;
                case STATUS_PURCHASE_COMPETE:
                    orderStatus = "구매완료";
                    break;
                case STATUS_ORDER_CANCEL_COMPLETE:
                    orderStatus = "주문취소";
                    break;
                case STATUS_ORDER_REJECT_COMPLETE:
                    orderStatus = "주문거절";
                    break;
                case STATUS_ISSUE_REQUEST:
                    orderStatus = "환불/교환요청";
                    CommonUtil.showViews(mBtnOrderCancelIssue);
                    break;
                case STATUS_ISSUE_COMPLETE:
                    orderStatus = "환불/교환완료";
                    CommonUtil.showViews(mBtnOrderCancelIssue);
                    break;
            }
        }

        if (mShopType == SHOP_TYPE_SELL) {
            CommonUtil.hideViews(
                    mBtnOrderCancel,
                    mBtnOrderAccept,
                    mBtnOrderReject,
                    mBtnShippingStart,
                    mBtnOrderCancelIssue,
                    mBtnOrderResolveIssue
            );
            switch (mTargetOrder.getStatus()) {
                case STATUS_ORDER_READY:
                    orderStatus = "주문준비";
                    break;
                case STATUS_PAYMENT_REQUEST:
                    orderStatus = "결제요청";
                    break;
                case STATUS_PAYMENT_COMPLETE:
                    orderStatus = "승인대기";
                    mBtnOrderCancel.setVisibility(View.GONE);
                    mBtnOrderAccept.setVisibility(View.VISIBLE);
                    mBtnOrderReject.setVisibility(View.VISIBLE);
                    break;
                case STATUS_WORK_IN_PROGRESS:
                    orderStatus = "작업중";
                    CommonUtil.showViews(mBtnShippingStart);
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
                    // CommonUtil.showViews(mBtnShippingEdit);
                    if (BuildConfig.DEBUG) {
                        // CommonUtil.showViews(mBtnShippingComplete);
                    }
                    break;
                case STATUS_SHIPPING_COMPLETE:
                    orderStatus = "전달완료";
                    if (BuildConfig.DEBUG) {
                        // CommonUtil.hideViews(mBtnShippingComplete);
                    }
                    break;
                case STATUS_PURCHASE_COMPETE:
                    orderStatus = "구매완료";
                    CommonUtil.showViews(mBtnOrderChat);
                    break;
                case STATUS_ORDER_CANCEL_COMPLETE:
                    orderStatus = "주문취소";
                    break;
                case STATUS_ORDER_REJECT_COMPLETE:
                    orderStatus = "주문거절";
                    break;
                case STATUS_ISSUE_REQUEST:
                    orderStatus = "환불/교환요청";
                    CommonUtil.showViews(mBtnOrderResolveIssue);
                    break;
                case STATUS_ISSUE_COMPLETE:
                    orderStatus = "환불/교환완료";
                    break;
            }
        }

        if (BuildConfig.DEBUG) {
            if (mShopType == SHOP_TYPE_SELL) {
                switch (mTargetOrder.getStatus()) {
                    case STATUS_SHIPPING_IN_PROGRESS:
                        // CommonUtil.showViews(mBtnShippingComplete);
                        break;
                    case STATUS_SHIPPING_COMPLETE:
                        // CommonUtil.hideViews(mBtnShippingComplete);
                        break;
                }
            }
        }

        mTvOrderStatus.setText(orderStatus);

        // 배송정보 버튼
        /*String shippingEditBtnText;
        if (TextUtils.isEmpty(mTvShippingCarrier.getText()) && TextUtils.isEmpty(mTvShippingTrackingNo.getText())) {
            shippingEditBtnText = "배송정보입력";
        } else {
            shippingEditBtnText = "배송정보수정";
        }
        mBtnShippingEdit.setText(shippingEditBtnText);*/

        // 주문일자
        String date = CommonUtil.getFormattedTimeString(mTargetOrder.getOrderTimestamp(), "yyyy.MM.dd");
        CharSequence orderDateLabel = CommonUtil.getBoldText("주문일자: ");
        mTvInfoOrderDate.setText(TextUtils.concat(orderDateLabel, date));

        // 상품주문번호
        CharSequence orderNoLabel = CommonUtil.getBoldText("상품주문번호: ");
        mTvInfoOrderNo.setText(TextUtils.concat(orderNoLabel, String.valueOf(mTargetOrder.getOrderNo())));

        // 판매자
        CharSequence sellerLabel = CommonUtil.getBoldText("판매자: ");
        /*String sellerPhone = CommonUtil.phoneFormat(mSellerPrivateInfo.getPhone());
        String sellerText = CommonUtil.format("%s (%s)", mTargetOrder.getSellerNickname(), sellerPhone);*/
        mTvInfoSeller.setText(TextUtils.concat(sellerLabel, mTargetOrder.getSellerNickname()));

        // 수량
        CharSequence quantityLabel = CommonUtil.getBoldText("수량: ");
        String quantity = CommonUtil.numberFormat(mTargetOrder.getQuantity(), "개");
        mTvInfoQuantity.setText(TextUtils.concat(quantityLabel, quantity));

        // 소계
        CharSequence subtotalLabel = CommonUtil.getBoldText("소계: ");
        String subtotalPrice = CommonUtil.numberFormat(mTargetOrder.getSubtotalPrice(), "원");
        mTvInfoSubtotalPrice.setText(TextUtils.concat(subtotalLabel, subtotalPrice));

        // 배송비
        CharSequence shippingPriceLabel = CommonUtil.getBoldText("배송비: ");
        String shippingPrice = CommonUtil.numberFormat(mTargetOrder.getShippingPrice(), "원");
        mTvInfoShippingPrice.setText(TextUtils.concat(shippingPriceLabel, shippingPrice));

        // 결제방식
        CharSequence paymentMethodLabel = CommonUtil.getBoldText("결제방식: ");
        mTvPaymentMethod.setText(TextUtils.concat(paymentMethodLabel, "신용카드"));

        // 결제금액
        CharSequence paymentTotalPriceLabel = CommonUtil.getBoldText("결제금액: ");
        String totalPrice = CommonUtil.numberFormat(mTargetOrder.getTotalPrice(), "원");
        mTvPaymentTotalPrice.setText(TextUtils.concat(paymentTotalPriceLabel, totalPrice));

        // 수령인
        CharSequence shippingUsernameLabel = CommonUtil.getBoldText("수령인: ");
        String shippingUsername = mTargetOrder.getShippingReceiver();
        mTvShippingUsername.setText(TextUtils.concat(shippingUsernameLabel, shippingUsername));

        // 연락처
        CharSequence shippingPhoneLabel = CommonUtil.getBoldText("연락처: ");
        String shippingPhone = CommonUtil.phoneFormat(mBuyerPrivateInfo.getPhone());
        mTvShippingPhone.setText(TextUtils.concat(shippingPhoneLabel, shippingPhone));

        // 배송지
        CharSequence shippingAddressLabel = CommonUtil.getBoldText("배송지: ");
        String shippingAddress = mTargetOrder.getShippingAddress() != null ? mTargetOrder.getShippingAddress() : "";
        mTvShippingAddress.setText(TextUtils.concat(shippingAddressLabel, shippingAddress));

        // 배송메모
        CharSequence shippingMessageLabel = CommonUtil.getBoldText("배송메모: ");
        String shippingMessage = mTargetOrder.getShippingMessage() != null ? mTargetOrder.getShippingMessage() : "";
        mTvShippingMessage.setText(TextUtils.concat(shippingMessageLabel, shippingMessage));

        // 주문인ID
        CharSequence ordererNicknameLabel = CommonUtil.getBoldText("주문인ID: ");
        mTvOrdererNickname.setText(TextUtils.concat(ordererNicknameLabel, mTargetOrder.getBuyerNickname()));

        // 주문인 연락처
        CharSequence ordererPhoneLabel = CommonUtil.getBoldText("연락처: ");
        String ordererPhone = CommonUtil.phoneFormat(mBuyerPrivateInfo.getPhone());
        mTvOrdererPhone.setText(TextUtils.concat(ordererPhoneLabel, ordererPhone));

        // 환불/교환
        if (mTargetOrder.getStatus() == STATUS_ISSUE_REQUEST || mTargetOrder.getStatus() == STATUS_ISSUE_COMPLETE) {
            CommonUtil.showViews(mVgIssueContainer);
        } else {
            CommonUtil.hideViews(mVgIssueContainer);
        }

        CharSequence issueMessageLabel = CommonUtil.getBoldText("사유: ");
        mTvIssueMessage.setText(TextUtils.concat(issueMessageLabel, mTargetOrder.getIssueMessage()));

        // 발송 정보
        if (mShopType == SHOP_TYPE_BUY) {
            CommonUtil.hideViews(mBtnShippingEdit);
        }
        if (mShopType == SHOP_TYPE_SELL) {
            if (mTargetOrder.getStatus() == STATUS_SHIPPING_IN_PROGRESS) {
                CommonUtil.showViews(mBtnShippingEdit);
            } else {
                CommonUtil.hideViews(mBtnShippingEdit);
            }
        }
        if (!TextUtils.isEmpty(mTargetOrder.getShippingCarrier()) && !TextUtils.isEmpty(mTargetOrder.getShippingTrackingNo())) {
            // CommonUtil.showViews(mVgShippingSellerContainer);
            CommonUtil.showViews(mTvShippingCarrier, mTvShippingTrackingNo);
            CharSequence shippingCarrierLabel = CommonUtil.getBoldText("택배사: ");
            String carrierName = ShopUtil.getShippingCarrierName(getContext(), mTargetOrder.getShippingCarrier());
            mTvShippingCarrier.setText(TextUtils.concat(shippingCarrierLabel, carrierName));

            CharSequence shippingTrackingNoLabel = CommonUtil.getBoldText("운송장번호: ");
            mTvShippingTrackingNo.setText(TextUtils.concat(shippingTrackingNoLabel, mTargetOrder.getShippingTrackingNo()));
        } else {
            // CommonUtil.hideViews(mVgShippingSellerContainer);
            CommonUtil.hideViews(mTvShippingCarrier, mTvShippingTrackingNo);
        }
    }

    private void downloadPhoto(@NonNull ImageView view,
                               @NonNull String postId,
                               @NonNull String filename) {
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        GlideManager.loadImage(postsRef, view);
    }

    // 구매자
    @OnClick(R.id.order_cancel_button)
    void orderCancel() {
        DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "구매를 취소하시겠습니까?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        showLoadingDialog();
                        ShopUtil.cancelOrder(mTargetOrder).continueWith(new Continuation<Order, Void>() {
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
                                mTargetOrder = resOrder;
                                initView();
                                if (mOnUpdateListener != null) mOnUpdateListener.onUpdate(resOrder);
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

    @OnClick(R.id.order_finalize_button)
    void orderFinalize() {
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
                        ShopUtil.finalize(mTargetOrder, review).continueWith(new Continuation<Order, Void>() {
                            @Override
                            public Void then(@NonNull Task<Order> task) throws Exception {
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Toast.makeText(getContext(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                    inputDialog.getDialog().show();
                                    return null;
                                }
                                Order newOrder = task.getResult();
                                mTargetOrder = newOrder;
                                initView();
                                if (mOnUpdateListener != null)
                                    mOnUpdateListener.onUpdate(mTargetOrder);
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

    // 판매자
    @OnClick(R.id.order_accept_button)
    void orderAccept() {
        DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "구매를 승인하시겠습니까?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        showLoadingDialog();
                        ShopUtil.changeStatus(Order.STATUS_WORK_IN_PROGRESS, mTargetOrder).continueWith(new Continuation<Order, Void>() {
                            @Override
                            public Void then(@NonNull Task<Order> task) throws Exception {
                                Log.i(TAG, "changeStatus:COMPLETE");
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    Log.w(TAG, "changeStatus:ERROR:" + e.getMessage());
                                    throw e;
                                }
                                Order newOrder = task.getResult();
                                Log.d(TAG, "changeStatus:newOrder:" + newOrder.toMap());
                                mTargetOrder = newOrder;
                                initView();
                                if (mOnUpdateListener != null) mOnUpdateListener.onUpdate(newOrder);
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

    // 판매자
    @OnClick(R.id.order_reject_button)
    void orderReject() {
        DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "구매를 거절하시겠습니까?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        showLoadingDialog();
                        ShopUtil.rejectOrder(mTargetOrder).continueWith(new Continuation<Order, Void>() {
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
                                mTargetOrder = resOrder;
                                initView();
                                if (mOnUpdateListener != null) mOnUpdateListener.onUpdate(resOrder);
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

    // 판매자
    @OnClick(R.id.shipping_start_button)
    void shippingStart() {
        DialogInputFragment inputDialog = DialogInputFragment.newInstance(
                "전달하기",
                "택배사와 운송장 번호를 입력해주세요.",
                getResources().getTextArray(R.array.shipping_tracking_carrier_names),
                new DialogInputFragment.OnSpinnerListener() {
                    @Override
                    public void onItemSelected(int position, CharSequence selected) {
                        super.onItemSelected(position, selected);
                        /*SHIPPING_CARRIER_건영택배 = "18";
                        SHIPPING_CARRIER_경동택배 = "23";
                        SHIPPING_CARRIER_고려택배 = "19";
                        SHIPPING_CARRIER_굿투럭 = "40";
                        SHIPPING_CARRIER_대신택배 = "22";
                        SHIPPING_CARRIER_로젠택배 = "06";
                        SHIPPING_CARRIER_롯데택배 = "08";
                        SHIPPING_CARRIER_애니트랙 = "43";
                        SHIPPING_CARRIER_우체국택배 = "01";
                        SHIPPING_CARRIER_일양로지스 = "11";
                        SHIPPING_CARRIER_천일택배 = "17";
                        SHIPPING_CARRIER_쿠팡로켓배송 = "36";
                        SHIPPING_CARRIER_한덱스 = "20";
                        SHIPPING_CARRIER_한의사랑택배 = "16";
                        SHIPPING_CARRIER_한진택배 = "05";
                        SHIPPING_CARRIER_합동택배 = "32";
                        SHIPPING_CARRIER_호남택배 = "45";
                        SHIPPING_CARRIER_CJ대한통운 = "04";
                        SHIPPING_CARRIER_CU편의점택배 = "46";
                        SHIPPING_CARRIER_CVSnet편의점택배 = "24";
                        SHIPPING_CARRIER_KGB택배 = "10";
                        SHIPPING_CARRIER_KGL네트웍스 = "30";
                        SHIPPING_CARRIER_KG로지스 = "39";
                        SHIPPING_CARRIER_SLX = "44";*/

                    }
                },
                new DialogInputFragment.OnSelectListener() {
                    @Override
                    public void onConfirm(final DialogInputFragment inputDialog) {
                        // super.onConfirm(inputDialog);
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
                        ShopUtil.ship(mTargetOrder.getId(), shippingCarrier, shippingTrackingNo).continueWith(new Continuation<Void, Void>() {
                            @Override
                            public Void then(@NonNull Task<Void> task) throws Exception {
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Toast.makeText(getContext(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                    inputDialog.getDialog().show();
                                    return null;
                                }
                                mTargetOrder.setStatus(STATUS_SHIPPING_IN_PROGRESS);
                                mTargetOrder.setShippingCarrier(shippingCarrier);
                                mTargetOrder.setShippingTrackingNo(shippingTrackingNo);
                                initView();
                                if (mOnUpdateListener != null)
                                    mOnUpdateListener.onUpdate(mTargetOrder);
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
        /*DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "작업을 완료하셨나요?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        showLoadingDialog();
                        ShopUtil.changeStatus(Order.STATUS_SHIPPING_IN_PROGRESS, mTargetOrder).continueWith(new Continuation<Order, Void>() {
                            @Override
                            public Void then(@NonNull Task<Order> task) throws Exception {
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    Log.w(TAG, "changeStatus:ERROR:" + e.getMessage());
                                    throw e;
                                }
                                Order newOrder = task.getResult();
                                mTargetOrder = newOrder;
                                initView();
                                if (mOnUpdateListener != null) mOnUpdateListener.onUpdate(newOrder);
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
        confirmDialog.show(getFragmentManager(), null);*/
    }

    /*@OnClick(R.id.shipping_complete_button)
    void shippingComplete() {
        DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "배송을 완료하셨나요?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        showLoadingDialog();
                        ShopUtil.changeStatus(Order.STATUS_SHIPPING_COMPLETE, mTargetOrder).continueWith(new Continuation<Order, Void>() {
                            @Override
                            public Void then(@NonNull Task<Order> task) throws Exception {
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    Log.w(TAG, "changeStatus:ERROR:" + e.getMessage());
                                    throw e;
                                }
                                Order newOrder = task.getResult();
                                mTargetOrder = newOrder;
                                initView();
                                if (mOnUpdateListener != null) mOnUpdateListener.onUpdate(newOrder);
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
    }*/

    @OnClick(R.id.shipping_edit_button)
    void shippingEdit() {
        if (mShopType == SHOP_TYPE_BUY) {

        }
        if (mShopType == SHOP_TYPE_SELL) {
            // TODO: 전달중일때만 변경이 가능하다.
            if (mTargetOrder.getStatus() != STATUS_SHIPPING_IN_PROGRESS) {
                Toast.makeText(getContext(), "주문상태가 전달중일 때만 변경이 가능합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            DialogInputFragment inputDialog = DialogInputFragment.newInstance(
                    "배송정보 변경",
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
                            ShopUtil.updateShippingInfo(mTargetOrder.getId(), shippingCarrier, shippingTrackingNo).continueWith(new Continuation<Void, Void>() {
                                @Override
                                public Void then(@NonNull Task<Void> task) throws Exception {
                                    dismissDialog();
                                    if (!task.isSuccessful()) {
                                        Toast.makeText(getContext(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                        inputDialog.getDialog().show();
                                        return null;
                                    }
                                    mTargetOrder.setStatus(STATUS_SHIPPING_IN_PROGRESS);
                                    mTargetOrder.setShippingCarrier(shippingCarrier);
                                    mTargetOrder.setShippingTrackingNo(shippingTrackingNo);
                                    initView();
                                    if (mOnUpdateListener != null)
                                        mOnUpdateListener.onUpdate(mTargetOrder);
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
            String carrierName = ShopUtil.getShippingCarrierName(getContext(), mTargetOrder.getShippingCarrier());
            inputDialog.setSpinnerText(carrierName);
            inputDialog.setInputHint("운송장 번호 입력");
            inputDialog.setInputText(mTargetOrder.getShippingTrackingNo());
            inputDialog.setInputTypeSingleline();
            inputDialog.show(getFragmentManager(), null);
        }
    }

    @OnClick(R.id.order_chat_button)
    void chat() {
        Intent intent = ChatActivity.createIntent(getContext(), mTargetOrder.getBuyerId());
        startActivityForResult(intent, CHAT_ACTIVITY);
    }

    @OnClick(R.id.order_report_issue_button)
    void reportIssue() {
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
                        ShopUtil.reportIssue(mTargetOrder.getId(), ISSUE_CODE_ETC, message).continueWith(new Continuation<Void, Void>() {
                            @Override
                            public Void then(@NonNull Task<Void> task) throws Exception {
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Toast.makeText(getContext(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                    inputDialog.getDialog().show();
                                    return null;
                                }
                                mTargetOrder.setStatus(STATUS_ISSUE_REQUEST);
                                mTargetOrder.setIssueCode(ISSUE_CODE_ETC);
                                mTargetOrder.setIssueMessage(message);
                                initView();
                                if (mOnUpdateListener != null)
                                    mOnUpdateListener.onUpdate(mTargetOrder);
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

    @OnClick(R.id.order_cancel_issue_button)
    void cancelIssue() {
        DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "환불/교환 요청을 철회하시겠습니까?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        showLoadingDialog();
                        ShopUtil.changeStatus(STATUS_SHIPPING_COMPLETE, mTargetOrder).continueWith(new Continuation<Order, Void>() {
                            @Override
                            public Void then(@NonNull Task<Order> task) throws Exception {
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    Log.w(TAG, "changeStatus:ERROR:" + e.getMessage());
                                    throw e;
                                }
                                Order newOrder = task.getResult();
                                mTargetOrder = newOrder;
                                initView();
                                if (mOnUpdateListener != null)
                                    mOnUpdateListener.onUpdate(mTargetOrder);
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

    @OnClick(R.id.order_resolve_issue_button)
    void resolveIssue() {
        DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "구매자로부터 물건을 받고 환불/교환 과정을 모두 완료 하셨나요?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        showLoadingDialog();
                        ShopUtil.resolveIssue(mTargetOrder.getId()).continueWith(new Continuation<Void, Void>() {
                            @Override
                            public Void then(@NonNull Task<Void> task) throws Exception {
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    Log.w(TAG, "changeStatus:ERROR:" + e.getMessage());
                                    throw e;
                                }
                                mTargetOrder.setStatus(STATUS_ISSUE_COMPLETE);
                                initView();
                                if (mOnUpdateListener != null)
                                    mOnUpdateListener.onUpdate(mTargetOrder);
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

    /*@OnClick(R.id.shipping_edit_button)
    void shippingEdit() {
        DialogInputFragment inputDialog = DialogInputFragment.newInstance(
                "택배사와 운송장 번호를 입력해주세요.",
                new DialogInputFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                    }

                    @Override
                    public void onDeny() {
                        super.onDeny();
                    }
                }
        );
        inputDialog.show(getFragmentManager(), null);
    }*/

    /*@OnClick(R.id.confirm_button)
    void confirm() {
        // Log.i(TAG, "confirm:listener:" + mOnButtonSelectListener);
        // if (mOnButtonSelectListener == null) return;
        // mOnButtonSelectListener.onConfirm();
        dismiss();
    }*/

    /*@OnClick(R.id.deny_button)
    void deny() {
        // Log.i(TAG, "deny:listener:" + mOnButtonSelectListener);
        // if (mOnButtonSelectListener == null) return;
        // mOnButtonSelectListener.onDeny();
        dismiss();
    }*/

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    // ProgressDialog

    public void showLoadingDialog() {
        Log.i(TAG, "showLoadingDialog");
        dismissDialog();
        mLoadingView.setVisibility(View.VISIBLE);
        getActivity().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        );
    }

    public void dismissDialog() {
        Log.i(TAG, "dismissDialog");
        mLoadingView.setVisibility(View.GONE);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        /*CommonUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingView.setVisibility(View.GONE);
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });*/
    }
    // /ProgressDialog

    private interface OnUpdateListener extends Parcelable {
        void onUpdate(Order updatedOrder);
    }

    public static class OnOrderUpdateListener implements OnUpdateListener {
        @Override
        public void onUpdate(Order updatedOrder) {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
        }
    }
}