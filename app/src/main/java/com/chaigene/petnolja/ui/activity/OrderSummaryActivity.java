package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.Card;
import com.chaigene.petnolja.model.PrivateInfo;
import com.chaigene.petnolja.ui.dialog.DialogConfirmFragment;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.ShopUtil;
import com.chaigene.petnolja.util.UserUtil;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.CARD_ACTIVITY;
import static com.chaigene.petnolja.Constants.DAUM_POSTCODE_ACTIVITY;
import static com.chaigene.petnolja.Constants.EXTRA_COVER_PHOTO;
import static com.chaigene.petnolja.Constants.EXTRA_PRODUCT_ID;
import static com.chaigene.petnolja.Constants.EXTRA_PRODUCT_PRICE;
import static com.chaigene.petnolja.Constants.EXTRA_PRODUCT_TITLE;
import static com.chaigene.petnolja.Constants.EXTRA_PRODUCT_TYPE;
import static com.chaigene.petnolja.Constants.EXTRA_SELLER_ID;
import static com.chaigene.petnolja.Constants.EXTRA_SELLER_NICKNAME;
import static com.chaigene.petnolja.Constants.EXTRA_SHIPPING_PRICE;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_CARD;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_REGIONS;
import static com.chaigene.petnolja.Constants.SECURITY_PIN_ACTIVITY;
import static com.chaigene.petnolja.ui.activity.CardActivity.ACTION_STATUS_SELECT;

public class OrderSummaryActivity extends BaseActivity {
    public static final String TAG = "OrderSummaryActivity";

    @BindView(R.id.product_photo)
    ImageView mIvProductPhoto;

    @BindView(R.id.product_title_text)
    TextView mTvProductTitle;

    @BindView(R.id.product_price_text)
    TextView mTvProductPrice;

    @BindView(R.id.quantity_input)
    EditText mEtQuantity;

    @BindView(R.id.shipping_price_text)
    TextView mTvShippingPrice;

    @BindView(R.id.total_price_text)
    TextView mTvTotalPrice;

    @BindView(R.id.shipping_receiver_input)
    EditText mEtShippingReceiver;

    @BindView(R.id.shipping_address_input)
    EditText mEtShippingAddress;

    @BindView(R.id.shipping_detail_address_container)
    ViewGroup mVgShippingDetailAddress;

    @BindView(R.id.shipping_detail_address_input)
    EditText mEtShippingDetailAddress;

    @BindView(R.id.shipping_message_input)
    EditText mEtShippingMessage;

    @BindView(R.id.card_name_text)
    TextView mTvCardName;

    @BindView(R.id.card_display_no_text)
    TextView mTvCardDisplayNo;

    @BindView(R.id.payment_button)
    Button mBtnPayment;

    private String mProductId;
    private int mProductType;
    private String mProductTitle;
    private String mProductPrice;
    private String mCoverPhoto;
    private ArrayList<String> mRegions;
    private String mShippingPrice;
    private String mSellerId;
    private String mSellerNickname;

    private PrivateInfo mBuyerPrivateInfo;
    private Card mPrimaryCard;

    private boolean mProgressing;

    public boolean isProgressing() {
        return mProgressing;
    }

    public void setProgressing(boolean progressing) {
        this.mProgressing = progressing;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);
        ButterKnife.bind(this);

        showLoadingDialog();
        asyncTask().continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "asyncTask:ERROR:" + task.getException().getMessage());
                    Log.w(TAG, "asyncTask:ERROR:", task.getException());
                    return null;
                }
                initView();
                return null;
            }
        });
    }

    @Override
    protected void readIntent() {
        super.readIntent();

        mProductId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        mProductType = getIntent().getIntExtra(EXTRA_PRODUCT_TYPE, 0);
        mProductTitle = getIntent().getStringExtra(EXTRA_PRODUCT_TITLE);
        mProductPrice = getIntent().getStringExtra(EXTRA_PRODUCT_PRICE);
        mCoverPhoto = getIntent().getStringExtra(EXTRA_COVER_PHOTO);
        mRegions = getIntent().getStringArrayListExtra(EXTRA_TARGET_REGIONS);
        mShippingPrice = getIntent().getStringExtra(EXTRA_SHIPPING_PRICE);
        mSellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);
        mSellerNickname = getIntent().getStringExtra(EXTRA_SELLER_NICKNAME);

        if (TextUtils.isEmpty(mProductId) ||
                TextUtils.isEmpty(mProductTitle) ||
                TextUtils.isEmpty(mProductPrice) ||
                TextUtils.isEmpty(mCoverPhoto) ||
                TextUtils.isEmpty(mSellerId) ||
                TextUtils.isEmpty(mSellerNickname)) {
            Toast.makeText(getApplicationContext(), "주문정보에 오류가 있습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle("작품 구매 정보");
    }

    private Task<Void> asyncTask() {
        mBuyerPrivateInfo = null;
        // mSellerPrivateInfo = null;

        /*DocumentReference ref = FirebaseFirestore.getInstance().collection("user-private-infos").document(AuthManager.getUserId());
        ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "get:ERROR:" + e.getMessage());
                    return;
                }
                DocumentSnapshot docSnapshot = task.getResult();
                Log.d(TAG, "get:SUCCESS:" + (docSnapshot.exists() ? docSnapshot.getId() + " => " + docSnapshot.getData() : null));
            }
        });

        return Tasks.forResult(null);*/

        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Task<PrivateInfo> getBuyerPrivateInfoTask = UserUtil.getPrivateInfo(AuthManager.getUserId());
                mBuyerPrivateInfo = Tasks.await(getBuyerPrivateInfoTask);

                if (!getBuyerPrivateInfoTask.isSuccessful()) {
                    Exception getBuyerPrivateInfoError = getBuyerPrivateInfoTask.getException();
                    Log.w(TAG, "asyncTask:getBuyerPrivateInfoError" + getBuyerPrivateInfoError.getMessage());
                    throw getBuyerPrivateInfoError;
                }

                Log.d(TAG, "asyncTask:getBuyerPrivateInfoTask:buyerPrivateInfo:" +
                        (mBuyerPrivateInfo != null ? mBuyerPrivateInfo.toMap() : "null")
                );

               /* Task<PrivateInfo> getSellerPrivateInfoTask = UserUtil.getPrivateInfo(mTargetOrder.getSellerId());
                mSellerPrivateInfo = Tasks.await(getSellerPrivateInfoTask);

                if (!getSellerPrivateInfoTask.isSuccessful()) {
                    Exception getSellerPrivateInfoError = getSellerPrivateInfoTask.getException();
                    throw getSellerPrivateInfoError;
                }*/

                Task<Card> getPrimaryCardTask = ShopUtil.getPrimaryCard();

                mPrimaryCard = Tasks.await(getPrimaryCardTask);
                if (!getPrimaryCardTask.isSuccessful()) {
                    Exception getPrimaryCardError = getPrimaryCardTask.getException();
                    Log.w(TAG, "asyncTask:getPrimaryCardError" + getPrimaryCardError.getMessage());
                    throw getPrimaryCardError;
                }

                Log.d(TAG, "asyncTask:getPrimaryCardTask:primaryCard:" +
                        (mPrimaryCard != null ? mPrimaryCard.toMap() : "null")
                );

                return null;
            }
        });
    }

    @Override
    protected void initView() {
        Log.i(TAG, "initView");
        super.initView();
        // setupSecurityPinInput();
        // scrambleDigitButtons();

        downloadPhoto(
                mIvProductPhoto,
                mProductId,
                mCoverPhoto
        );

        // 제품명
        mTvProductTitle.setText(mProductTitle);

        // 제품 가격
        mTvProductPrice.setText(CommonUtil.numberFormat(mProductPrice, "원"));

        // 수량
        setupQuantityInput();
        mEtQuantity.setText("1");
        mEtQuantity.setSelection(mEtQuantity.length());

        // 배송비
        String shippingPriceText;
        if (CommonUtil.toInt(mShippingPrice) > 0) {
            shippingPriceText = CommonUtil.numberFormat(mShippingPrice, "원");
        } else {
            shippingPriceText = "무료";
        }
        mTvShippingPrice.setText(shippingPriceText);

        // 받으시는 분
        setupShippingReceiverInput();
        mEtShippingReceiver.setText(mBuyerPrivateInfo.getUsername());

        // 배송주소
        setupShippingAddressInput();
        mEtShippingAddress.setText(mBuyerPrivateInfo.getAddress());

        // 결제카드 (주결제카드가 null인 상황은 절대 일어나서는 안된다.)
        updateCardView();

        updatePaymentBtn();
    }

    private void updateCardView() {
        if (mPrimaryCard != null) {
            mTvCardName.setText(mPrimaryCard.getName());
            String prettyCardNo = CommonUtil.getPrettyCardNo(mPrimaryCard.getDisplayNo());
            mTvCardDisplayNo.setText(prettyCardNo);
        }
    }

    private void downloadPhoto(@NonNull ImageView view,
                               @NonNull String postId,
                               @NonNull String filename) {
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        GlideManager.loadImage(postsRef, view);
    }

    private void setupQuantityInput() {
        mEtQuantity.setFilters(new InputFilter[]{new InputFilter() {
            final int MIN_VALUE = 1;
            final int MAX_VALUE = 999;

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                try {
                    // Remove the string out of destination that is to be replaced
                    String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
                    // Add the new string in
                    newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
                    int input = Integer.parseInt(newVal);
                    if (isInRange(MIN_VALUE, MAX_VALUE, input))
                        return null;
                } catch (NumberFormatException nfe) {
                }
                return "";
            }

            private boolean isInRange(int a, int b, int c) {
                return b > a ? c >= a && c <= b : c >= b && c <= a;
            }
        }});
        mEtQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                updatePaymentBtn();
                if (!TextUtils.isEmpty(editable.toString().trim())) calculate();
            }
        });
    }

    private void setupShippingReceiverInput() {
        mEtShippingReceiver.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                updatePaymentBtn();
            }
        });
    }

    private void setupShippingAddressInput() {
        mEtShippingAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                updatePaymentBtn();
            }
        });
    }

    private void calculate() {
        int quantity = CommonUtil.toInt(mEtQuantity.getText().toString().trim());
        int unitPrice = CommonUtil.toInt(mProductPrice);
        int subtotalPrice = unitPrice * quantity;
        int shippingPrice = CommonUtil.toInt(mShippingPrice);
        int totalPrice = subtotalPrice + shippingPrice;

        mTvTotalPrice.setText(CommonUtil.numberFormat(totalPrice, "원"));
    }

    private void updatePaymentBtn() {
        Log.i(TAG, "updatePaymentBtn");
        boolean isEnabled = true;
        if (TextUtils.isEmpty(mEtQuantity.getText().toString().trim())) {
            isEnabled = false;
        }
        if (TextUtils.isEmpty(mEtShippingReceiver.getText().toString().trim())) {
            isEnabled = false;
        }
        if (TextUtils.isEmpty(mEtShippingAddress.getText().toString().trim())) {
            isEnabled = false;
        }
        mBtnPayment.setEnabled(isEnabled);
    }

    @OnClick({R.id.shipping_address_container, R.id.shipping_address_input})
    void startDaumPostcodeActivity() {
        Intent intent = createIntent(DaumPostcodeActivity.class);
        startActivityForResult(intent, DAUM_POSTCODE_ACTIVITY);
    }

    @OnClick(R.id.shipping_detail_address_confirm_button)
    void makeCompleteAddress() {
        String detailAddress = mEtShippingDetailAddress.getText().toString().trim();
        /*if (TextUtils.isEmpty(detailAddress)) {
            CommonUtil.showSnackbar(getActivity(), "상세주소를 입력해주세요.");
            mEtDetailAddress.requestFocus();
            return;
        }*/
        String address = mEtShippingAddress.getText().toString().trim();
        final String completeAddress;
        if (!TextUtils.isEmpty(detailAddress)) {
            completeAddress = address + " " + detailAddress;
        } else {
            completeAddress = address;
        }
        DialogConfirmFragment dialogConfirmFragment = DialogConfirmFragment.newInstance(
                "다음 주소가 확실합니까?",
                "\"" + completeAddress + "\"",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        mEtShippingAddress.setText(completeAddress);
                        // mVgDetailAddressTab.setVisibility(View.GONE);
                        CommonUtil.setVisibilityWithFade(mVgShippingDetailAddress, View.GONE);
                        mEtShippingDetailAddress.setText(null);
                        mBtnPayment.setEnabled(true);
                    }
                }
        );
        dialogConfirmFragment.show(getSupportFragmentManager(), null);
    }

    @OnClick(R.id.card_change_button)
    void cardChange() {
        Intent in = CardActivity.createIntent(this, ACTION_STATUS_SELECT);
        startActivityForResult(in, CARD_ACTIVITY);
    }

    @OnClick(R.id.payment_button)
    void pay() {
        final int quantity = CommonUtil.toInt(mEtQuantity.getText().toString().trim());
        if (quantity <= 0) {
            CommonUtil.showSnackbar(OrderSummaryActivity.this, "수량을 1개 이상 입력해주세요.");
            mEtQuantity.requestFocus();
            return;
        }
        DialogConfirmFragment dialogFragment = DialogConfirmFragment.newInstance(
                "결제 하시겠습니까?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        String address = mEtShippingAddress.getText().toString().trim();
                        PrivateInfo privateInfo = new PrivateInfo();
                        privateInfo.setAddress(address);
                        showLoadingDialog();
                        UserUtil.updatePrivateInfo(privateInfo).continueWith(new Continuation<Void, Void>() {
                            @Override
                            public Void then(@NonNull Task<Void> task) throws Exception {
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    Log.w(TAG, "pay:updatePrivateInfo:ERROR:" + e.getMessage());
                                    // CommonUtil.showSnackbar(OrderSummaryActivity.this, "일시적인 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.");
                                }
                                String shippingReceiver = mEtShippingReceiver.getText().toString().trim();
                                String shippingAddress = mEtShippingAddress.getText().toString().trim();
                                String shippingMessage = mEtShippingMessage.getText().toString().trim();
                                Intent in = SecurityPinActivity.createIntent(
                                        getApplicationContext(),
                                        mProductId,
                                        mProductType,
                                        mProductTitle,
                                        mProductPrice,
                                        mCoverPhoto,
                                        mRegions,
                                        quantity,
                                        mShippingPrice,
                                        mSellerId,
                                        mSellerNickname,
                                        shippingReceiver,
                                        shippingAddress,
                                        shippingMessage,
                                        mPrimaryCard.getId(),
                                        0
                                );
                                startActivityForResult(in, SECURITY_PIN_ACTIVITY);
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
        dialogFragment.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));

        switch (requestCode) {
            case DAUM_POSTCODE_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    String address = data.getStringExtra("address");
                    if (address != null) {
                        mEtShippingAddress.setText(address.trim());
                        // mVgDetailAddressTab.setVisibility(View.VISIBLE);
                        CommonUtil.setVisibilityWithFade(mVgShippingDetailAddress, View.VISIBLE);
                        mEtShippingDetailAddress.setText(null);
                        mEtShippingDetailAddress.requestFocus();
                        mBtnPayment.setEnabled(false);
                    }
                }

                if (resultCode == RESULT_CANCELED) {
                }
                break;
            case CARD_ACTIVITY:
                // 변경된 카드 정보를 넘겨받아야 한다.
                if (resultCode == RESULT_OK) {
                    mPrimaryCard = (Card) data.getSerializableExtra(EXTRA_TARGET_CARD);
                    updateCardView();
                }

                if (resultCode == RESULT_CANCELED) {

                }
                break;
            case SECURITY_PIN_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }

                if (resultCode == RESULT_CANCELED) {

                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static Intent createIntent(Context context,
                                      String productId,
                                      int productType,
                                      String productTitle,
                                      String productPrice,
                                      String coverPhoto,
                                      ArrayList<String> regions,
                                      String shippingPrice,
                                      String sellerId,
                                      String sellerNickname) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, OrderSummaryActivity.class);
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        intent.putExtra(EXTRA_PRODUCT_TYPE, productType);
        intent.putExtra(EXTRA_PRODUCT_TITLE, productTitle);
        intent.putExtra(EXTRA_PRODUCT_PRICE, productPrice);
        intent.putExtra(EXTRA_COVER_PHOTO, coverPhoto);
        intent.putExtra(EXTRA_TARGET_REGIONS, regions);
        intent.putExtra(EXTRA_SHIPPING_PRICE, shippingPrice);
        intent.putExtra(EXTRA_SELLER_ID, sellerId);
        intent.putExtra(EXTRA_SELLER_NICKNAME, sellerNickname);
        return intent;
    }
}