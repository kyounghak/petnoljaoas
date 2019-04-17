package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.chaigene.petnolja.BuildConfig;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.ConfigManager;
import com.chaigene.petnolja.model.ErrorCode;
import com.chaigene.petnolja.model.Order;
import com.chaigene.petnolja.ui.dialog.DialogAlertFragment;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.ShopUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.EXTRA_CARD_ID;
import static com.chaigene.petnolja.Constants.EXTRA_COVER_PHOTO;
import static com.chaigene.petnolja.Constants.EXTRA_INSTALLMENT;
import static com.chaigene.petnolja.Constants.EXTRA_PRODUCT_ID;
import static com.chaigene.petnolja.Constants.EXTRA_PRODUCT_PRICE;
import static com.chaigene.petnolja.Constants.EXTRA_PRODUCT_TITLE;
import static com.chaigene.petnolja.Constants.EXTRA_PRODUCT_TYPE;
import static com.chaigene.petnolja.Constants.EXTRA_QUANTITY;
import static com.chaigene.petnolja.Constants.EXTRA_SELLER_ID;
import static com.chaigene.petnolja.Constants.EXTRA_SELLER_NICKNAME;
import static com.chaigene.petnolja.Constants.EXTRA_SHIPPING_ADDRESS;
import static com.chaigene.petnolja.Constants.EXTRA_SHIPPING_MESSAGE;
import static com.chaigene.petnolja.Constants.EXTRA_SHIPPING_PRICE;
import static com.chaigene.petnolja.Constants.EXTRA_SHIPPING_RECEIVER;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_REGIONS;
import static com.chaigene.petnolja.model.Request.STATUS_SUCCESS;

public class SecurityPinActivity extends BaseActivity {
    public static final String TAG = "SecurityPinActivity";

    private static final String ACTION_STATUS = "action_status";
    public static final int ACTION_STATUS_REGISTER = 0;
    public static final int ACTION_STATUS_PAYMENT = 1;

    @BindView(R.id.label_message)
    TextView mTvLableMessage;

    @BindView(R.id.security_pin_input)
    EditText mEtSecurityPin;

    @BindView(R.id.forgot_security_pin_button)
    Button mBtnForgotSecurityPin;

    @BindView(R.id.digit_button_00)
    Button mBtnDigit00;
    @BindView(R.id.digit_button_01)
    Button mBtnDigit01;
    @BindView(R.id.digit_button_02)
    Button mBtnDigit02;
    @BindView(R.id.digit_button_03)
    Button mBtnDigit03;
    @BindView(R.id.digit_button_04)
    Button mBtnDigit04;
    @BindView(R.id.digit_button_05)
    Button mBtnDigit05;
    @BindView(R.id.digit_button_06)
    Button mBtnDigit06;
    @BindView(R.id.digit_button_07)
    Button mBtnDigit07;
    @BindView(R.id.digit_button_08)
    Button mBtnDigit08;
    @BindView(R.id.digit_button_09)
    Button mBtnDigit09;
    @BindView(R.id.digit_button_10)
    Button mBtnDigit10;

    private Button[] mDigitButtons;

    @BindView(R.id.delete_button)
    Button mBtnDelete;

    private int mActionStatus;

    private TextWatcher mTextWatcher;

    // 결제비밀번호 등록
    private String mPreviousInput;

    // 결제
    private String mProductId;
    private int mProductType;
    private String mProductTitle;
    private String mProductPrice;
    private String mCoverPhoto;
    private List<String> mRegions;
    private int mQuantity;
    private String mShippingPrice;
    private String mSellerId;
    private String mSellerNickname;
    private String mShippingReceiver;
    private String mShippingAddress;
    private String mShippingMessage;
    private String mCardId;
    private int mInstallment;

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

        // 스크린 캡쳐 방지
        // Souce: https://stackoverflow.com/a/9822607/4729203
        if (!BuildConfig.DEBUG) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        setContentView(R.layout.activity_security_pin);
        ButterKnife.bind(this);
        mDigitButtons = new Button[]{
                mBtnDigit00,
                mBtnDigit01,
                mBtnDigit02,
                mBtnDigit03,
                mBtnDigit04,
                mBtnDigit05,
                mBtnDigit06,
                mBtnDigit07,
                mBtnDigit08,
                mBtnDigit09,
                mBtnDigit10
        };
        initView();
    }

    @Override
    protected void readIntent() {
        super.readIntent();

        mActionStatus = getIntent().getIntExtra(ACTION_STATUS, 0);
        if (mActionStatus == ACTION_STATUS_REGISTER) return;

        mProductId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        mProductTitle = getIntent().getStringExtra(EXTRA_PRODUCT_TITLE);
        mProductPrice = getIntent().getStringExtra(EXTRA_PRODUCT_PRICE);
        mCoverPhoto = getIntent().getStringExtra(EXTRA_COVER_PHOTO);
        mRegions = getIntent().getStringArrayListExtra(EXTRA_TARGET_REGIONS);
        mQuantity = getIntent().getIntExtra(EXTRA_QUANTITY, 0);
        mShippingPrice = getIntent().getStringExtra(EXTRA_SHIPPING_PRICE);
        mSellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);
        mSellerNickname = getIntent().getStringExtra(EXTRA_SELLER_NICKNAME);
        mShippingReceiver = getIntent().getStringExtra(EXTRA_SHIPPING_RECEIVER);
        mShippingAddress = getIntent().getStringExtra(EXTRA_SHIPPING_ADDRESS);
        mShippingMessage = getIntent().getStringExtra(EXTRA_SHIPPING_MESSAGE);
        mCardId = getIntent().getStringExtra(EXTRA_CARD_ID);
        mInstallment = getIntent().getIntExtra(EXTRA_INSTALLMENT, 0);

        if (TextUtils.isEmpty(mProductId) ||
                TextUtils.isEmpty(mProductTitle) ||
                TextUtils.isEmpty(mProductPrice) ||
                TextUtils.isEmpty(mCoverPhoto) ||
                TextUtils.isEmpty(mSellerId) ||
                TextUtils.isEmpty(mSellerNickname) ||
                TextUtils.isEmpty(mCardId)) {
            Toast.makeText(getApplicationContext(), "주문정보에 오류가 있습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String toolbarTitle = null;
        if (mActionStatus == ACTION_STATUS_REGISTER) {
            toolbarTitle = "결제비밀번호 등록";
        }
        if (mActionStatus == ACTION_STATUS_PAYMENT) {
            toolbarTitle = "결제비밀번호";
        }
        setToolbarTitle(toolbarTitle);
    }

    @Override
    protected void initView() {
        super.initView();
        if (mActionStatus == ACTION_STATUS_REGISTER) {
            // 첫번째 시도
            if (mPreviousInput == null) {
                mTvLableMessage.setText("결제비밀번호 6자리를 입력해주세요.");
            }
            // 두번째 시도
            if (mPreviousInput != null) {
                mTvLableMessage.setText("다시 한번 결제비밀번호를 입력해주세요.");
            }
            mBtnForgotSecurityPin.setVisibility(View.GONE);
        }
        if (mActionStatus == ACTION_STATUS_PAYMENT) {
            mTvLableMessage.setText("결제비밀번호 6자리를 입력해주세요.");
            mBtnForgotSecurityPin.setVisibility(View.VISIBLE);
        }
        setupSecurityPinInput();
        scrambleDigitButtons();
    }

    // 6자리 숫자를 입력하면 다음 단계로 넘어가야 한다.
    private void setupSecurityPinInput() {
        if (mTextWatcher != null) {
            mEtSecurityPin.removeTextChangedListener(mTextWatcher);
        } else {
            mTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (editable.length() == 6) {
                        String securityPin = editable.toString().trim();
                        onInputDone(securityPin);
                    }
                }
            };
        }
        mEtSecurityPin.addTextChangedListener(mTextWatcher);
        mEtSecurityPin.setText(null);
        mEtSecurityPin.setEnabled(true);
    }

    @SuppressWarnings({"ConstantConditions", "UnnecessaryReturnStatement"})
    private void onInputDone(String inputValue) {
        mEtSecurityPin.setEnabled(false);
        if (mActionStatus == ACTION_STATUS_REGISTER) {
            // 첫번째 시도
            if (mPreviousInput == null) {
                this.mPreviousInput = inputValue;
                initView();
                return;
            }
            // 두번째 시도
            if (mPreviousInput != null) {
                // 동일한 비밀번호를 정확하게 입력했을 경우 => 결제비밀번호 삽입
                if (mPreviousInput.equals(inputValue)) {
                    showLoadingDialog();
                    setProgressing(true);
                    ShopUtil.updateSecurityPin(inputValue).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(@NonNull Task<Void> task) throws Exception {
                            dismissDialog();
                            setProgressing(false);
                            if (!task.isSuccessful()) {
                                // ERROR
                                CommonUtil.showSnackbar(SecurityPinActivity.this, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                                initView();
                                return null;
                            }
                            DialogAlertFragment alertDialog = DialogAlertFragment.newInstance(
                                    "결제비밀번호가 설정되었습니다.",
                                    new DialogAlertFragment.OnDoneListener() {
                                        @Override
                                        public void onDone() {
                                            super.onDone();
                                            onRegisterFinish();
                                        }
                                    },
                                    false
                            );
                            alertDialog.show(getSupportFragmentManager(), null);
                            return null;
                        }
                    });
                    return;
                }
                // 이전에 입력하는 비밀번호와 입력하지 않을 경우 => 처음부터 다시 시작한다.
                if (!mPreviousInput.equals(inputValue)) {
                    this.mPreviousInput = null;
                    CommonUtil.showSnackbar(this, "결제비밀번호가 일치하지 않습니다.");
                    initView();
                    return;
                }
            }
            return;
        }
        if (mActionStatus == ACTION_STATUS_PAYMENT) {
            showLoadingDialog();
            buy(inputValue).continueWith(new Continuation<Order, Void>() {
                @Override
                public Void then(@NonNull Task<Order> task) throws Exception {
                    dismissDialog();
                    setProgressing(false);
                    if (!task.isSuccessful()) {
                        // Log.w(TAG, "onInputDone:buy:ERROR:" + task.getException().getMessage());
                        Log.w(TAG, "onInputDone:buy:ERROR:", task.getException());
                        CommonUtil.showSnackbar(SecurityPinActivity.this, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                        initView();
                        return null;
                    }
                    Order resOrder = task.getResult();

                    // Deprecated
                    /*if (resOrder.getStatus() == Order.STATUS_PAYMENT_FAIL) {
                        switch (resOrder.getFailReason()) {
                            case FAIL_REASON_DEFAULT:
                                CommonUtil.showSnackbar(SecurityPinActivity.this, "결제에 실패하였습니다. 카드 정보를 확인하고 다시 시도해주세요.");
                                CommonUtil.delayCall(new Callable() {
                                    @Override
                                    public Object call() throws Exception {
                                        setResult(RESULT_CANCELED);
                                        finish();
                                        return null;
                                    }
                                }, 2000);
                                break;
                            case FAIL_REASON_WRONG_SECURITY_PIN:
                                CommonUtil.showSnackbar(SecurityPinActivity.this, "결제비밀번호를 잘못 입력하셨습니다. 다시 입력해주세요.");
                                initView();
                                break;
                        }
                        return null;
                    }*/

                    if (resOrder.getResponseStatus() != STATUS_SUCCESS) {
                        switch (resOrder.getResponseCode()) {
                            case ErrorCode.ORDER_INVALID_SECURITY_PIN:
                                // CommonUtil.showSnackbar(SecurityPinActivity.this, "결제비밀번호를 잘못 입력하셨습니다. 다시 입력해주세요.");
                                CommonUtil.showSnackbar(SecurityPinActivity.this, resOrder.getResponseMessage());
                                initView();
                                break;
                            case ErrorCode.ORDER_PIN_FAIL_QUOTA_EXCEEDED:
                                CommonUtil.showSnackbar(SecurityPinActivity.this, resOrder.getResponseMessage());
                                CommonUtil.delayCall(new Callable() {
                                    @Override
                                    public Object call() throws Exception {
                                        initView();
                                        forgotSecurityPin();
                                        return null;
                                    }
                                }, 2000);
                                break;
                            default:
                                // CommonUtil.showSnackbar(SecurityPinActivity.this, "결제에 실패하였습니다. 카드 정보를 확인하고 다시 시도해주세요.");
                                String errorMessage = resOrder.getResponseMessage() != null ?
                                        resOrder.getResponseMessage() :
                                        "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                                CommonUtil.showSnackbar(SecurityPinActivity.this, errorMessage);
                                CommonUtil.delayCall(new Callable() {
                                    @Override
                                    public Object call() throws Exception {
                                        setResult(RESULT_CANCELED);
                                        finish();
                                        return null;
                                    }
                                }, 2000);
                                break;
                        }
                        return null;
                    }

                    onPaymentFinish();
                    return null;
                }
            });
            return;
        }
    }

    private void onRegisterFinish() {
        Log.i(TAG, "onRegisterFinish");
        setResult(RESULT_OK);
        finish();
    }

    private void onPaymentFinish() {
        Log.i(TAG, "onPaymentFinish");
        // EventBus.getDefault().post(new OrderCompleteEvent());
        setResult(RESULT_OK);
        finish();
    }

    private void scrambleDigitButtons() {
        Log.i(TAG, "scrambleDigitButtons");
        List<Button> scrambledDigitButtons = Arrays.asList(mDigitButtons);
        Collections.shuffle(scrambledDigitButtons);
        for (int i = 0; i <= 10; i++) {
            TextView targetButton = scrambledDigitButtons.get(i);
            Log.i(TAG, "scrambleDigitButtons:loop:index:" + i + "|targetButton:" + CommonUtil.getResourceName(targetButton));
            targetButton.setOnClickListener(null);
            if (i == 10) {
                targetButton.setText("");
                continue;
            }
            targetButton.setText(String.valueOf(i));
            final int clickedDigit = i;
            targetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onDigitButtonClick(clickedDigit);
                }
            });
        }
    }

    private void onDigitButtonClick(int digit) {
        if (isProgressing()) return;
        mEtSecurityPin.append(String.valueOf(digit));
    }

    @OnClick(R.id.delete_button)
    void backspace() {
        if (isProgressing()) return;
        Editable content = mEtSecurityPin.getText();
        if (content.length() > 0) {
            content.delete(content.length() - 1, content.length());
        }
    }

    // TODO: 결제비밀번호를 잊어버렸을 때는 일단 현재 액티비티를 그대로 유지해둔다.
    @OnClick(R.id.forgot_security_pin_button)
    void forgotSecurityPin() {
        Intent in = IDVerificationActivity.createIntent(this, IDVerificationActivity.ACTION_STATUS_SECURITY_PIN_REGISTER);
        startActivity(in);
    }

    private Task<Order> buy(String securityPin) {
        showLoadingDialog();
        boolean isFakePaymentMode = ConfigManager.getInstance(this).isFakePaymentMode();
        if (!isFakePaymentMode) {
            return ShopUtil.buy(
                    mProductId,
                    mProductType,
                    mProductTitle,
                    mProductPrice,
                    mCoverPhoto,
                    mRegions,
                    mQuantity,
                    mShippingPrice,
                    mSellerId,
                    mSellerNickname,
                    mShippingReceiver,
                    mShippingAddress,
                    mShippingMessage,
                    mCardId,
                    securityPin,
                    mInstallment
            );
        } else {
            return ShopUtil.fakeBuy(
                    mProductId,
                    mProductType,
                    mProductTitle,
                    mProductPrice,
                    mCoverPhoto,
                    mRegions,
                    mQuantity,
                    mShippingPrice,
                    mSellerId,
                    mSellerNickname,
                    mShippingReceiver,
                    mShippingAddress,
                    mShippingMessage,
                    mCardId,
                    securityPin,
                    mInstallment
            );
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

    public static Intent createIntent(Context context, int actionStatus) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, SecurityPinActivity.class);
        intent.putExtra(ACTION_STATUS, actionStatus);
        return intent;
    }

    public static Intent createIntent(Context context,
                                      String productId,
                                      int productType,
                                      String productTitle,
                                      String productPrice,
                                      String coverPhoto,
                                      ArrayList<String> regions,
                                      int quantity,
                                      String shippingPrice,
                                      String sellerId,
                                      String sellerNickname,
                                      String shippingReceiver,
                                      String shippingAddress,
                                      String shippingMessage,
                                      String cardId,
                                      int installment) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, SecurityPinActivity.class);
        intent.putExtra(ACTION_STATUS, ACTION_STATUS_PAYMENT);
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        intent.putExtra(EXTRA_PRODUCT_TYPE, productType);
        intent.putExtra(EXTRA_PRODUCT_TITLE, productTitle);
        intent.putExtra(EXTRA_PRODUCT_PRICE, productPrice);
        intent.putExtra(EXTRA_COVER_PHOTO, coverPhoto);
        intent.putExtra(EXTRA_TARGET_REGIONS, regions);
        intent.putExtra(EXTRA_QUANTITY, quantity);
        intent.putExtra(EXTRA_SHIPPING_PRICE, shippingPrice);
        intent.putExtra(EXTRA_SELLER_ID, sellerId);
        intent.putExtra(EXTRA_SELLER_NICKNAME, sellerNickname);
        intent.putExtra(EXTRA_SHIPPING_RECEIVER, shippingReceiver);
        intent.putExtra(EXTRA_SHIPPING_ADDRESS, shippingAddress);
        intent.putExtra(EXTRA_SHIPPING_MESSAGE, shippingMessage);
        intent.putExtra(EXTRA_CARD_ID, cardId);
        intent.putExtra(EXTRA_INSTALLMENT, installment);
        return intent;
    }
}