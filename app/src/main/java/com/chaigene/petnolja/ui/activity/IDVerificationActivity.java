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
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.chaigene.petnolja.BuildConfig;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.model.PrivateInfo;
import com.chaigene.petnolja.ui.dialog.DialogAlertFragment;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.CARD_REG_ACTIVITY;
import static com.chaigene.petnolja.Constants.SECURITY_PIN_ACTIVITY;
import static com.chaigene.petnolja.ui.activity.CardRegistrationActivity.ACTION_STATUS_INITIAL_REGISTER;
import static com.chaigene.petnolja.ui.activity.SecurityPinActivity.ACTION_STATUS_REGISTER;

public class IDVerificationActivity extends BaseActivity {
    public static final String TAG = "IDVerificationActivity";

    private static final String ACTION_STATUS = "action_status";
    public static final int ACTION_STATUS_ID_VERIFY = 0;
    public static final int ACTION_STATUS_CARD_REGISTER = 1;
    public static final int ACTION_STATUS_SECURITY_PIN_REGISTER = 2;

    private static final int PHONE_VERIFY_STATE_INITIALIZED = 1;
    private static final int PHONE_VERIFY_STATE_MODIFY = 2;
    private static final int PHONE_VERIFY_STATE_REQUEST_CODE = 3;
    private static final int PHONE_VERIFY_STATE_CODE_SENT = 4;
    private static final int PHONE_VERIFY_STATE_VERIFY_FAILED = 5;
    private static final int PHONE_VERIFY_STATE_VERIFY_SUCCESS = 6;
    private static final int PHONE_VERIFY_STATE_UPDATE_FAILED = 7;
    private static final int PHONE_VERIFY_STATE_UPDATE_SUCCESS = 8;

    @BindView(R.id.username_input)
    EditText mEtUsername;

    @BindView(R.id.birthday_input)
    EditText mEtBirthday;

    @BindView(R.id.sex_input)
    EditText mEtSex;

    @BindView(R.id.phone_input)
    EditText mEtPhone;

    @BindView(R.id.phone_verify_button)
    Button mBtnPhoneVerify;

    @BindView(R.id.phone_verify_resend_button)
    Button mBtnPhoneVerifyResend;

    @BindView(R.id.phone_sms_code_tab)
    ViewGroup mVgPhoneSmsCodeTab;

    @BindView(R.id.phone_sms_code_input)
    EditText mEtPhoneSmsCode;

    @BindView(R.id.phone_verify_code_button)
    Button mBtnPhoneVerifyCode;

    private int mActionStatus;
    private PrivateInfo mPrivateInfo;

    private PhoneAuthCredential mPhoneAuthCredential;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

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

        setContentView(R.layout.activity_id_verification);
        ButterKnife.bind(this);

        if (mActionStatus == ACTION_STATUS_ID_VERIFY || mActionStatus == ACTION_STATUS_CARD_REGISTER) {
            initView();
        }

        if (mActionStatus == ACTION_STATUS_SECURITY_PIN_REGISTER) {
            showLoadingDialog();
            asyncTask().continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(@NonNull Task<Void> task) throws Exception {
                    dismissDialog();
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "asyncTask:ERROR:" + task.getException().getMessage());
                        return null;
                    }
                    initView();
                    return null;
                }
            });
        }
    }

    @Override
    protected void readIntent() {
        super.readIntent();
        mActionStatus = getIntent().getIntExtra(ACTION_STATUS, 0);
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle("본인 인증");
    }

    private Task<Void> asyncTask() {
        mPrivateInfo = null;
        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Task<PrivateInfo> getBuyerPrivateInfoTask = UserUtil.getPrivateInfo(AuthManager.getUserId());
                mPrivateInfo = Tasks.await(getBuyerPrivateInfoTask);

                if (!getBuyerPrivateInfoTask.isSuccessful()) {
                    Exception getBuyerPrivateInfoError = getBuyerPrivateInfoTask.getException();
                    Log.w(TAG, "asyncTask:getBuyerPrivateInfoError" + getBuyerPrivateInfoError.getMessage());
                    throw getBuyerPrivateInfoError;
                }

                Log.d(TAG, "asyncTask:getBuyerPrivateInfoTask:buyerPrivateInfo:" +
                        (mPrivateInfo != null ? mPrivateInfo.toMap() : "null")
                );
                return null;
            }
        });
    }

    @Override
    protected void initView() {
        super.initView();
        setupUsernameInput();
        setupBirthdayInput();
        setupSexInput();
        updatePhoneVerificationUI(PHONE_VERIFY_STATE_INITIALIZED);
    }

    private void setupUsernameInput() {
        mEtUsername.setFilters(new InputFilter[]{new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Pattern pattern = Pattern.compile("^[ㄱ-ㅣ가-힣]*$");
                if (!pattern.matcher(source).matches()) {
                    return "";
                }
                return null;
            }
        }});
    }

    private void setupBirthdayInput() {
        mEtBirthday.addTextChangedListener(new TextWatcher() {
            final String originalHint = mEtBirthday.getHint().toString();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (TextUtils.isEmpty(editable.toString().trim())) {
                    mEtBirthday.setHint(originalHint);
                } else {
                    mEtBirthday.setHint(null);
                }
            }
        });
        mEtBirthday.addTextChangedListener(new TextWatcher() {
            final String TAG = "TextWatcher";

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 6) mEtSex.requestFocus();
            }
        });
        mEtBirthday.setOnKeyListener(new View.OnKeyListener() {
            final String TAG = "OnKeyListener";

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_DEL) {
                    if (mEtBirthday.getText().length() == 6) {
                        Log.i(TAG, "onKey:text:" + mEtBirthday.getText());
                        Log.i(TAG, "onKey:keyCode:" + keyCode);
                        if (mEtSex.getText().length() == 0) {
                            char unicodeChar = (char) event.getUnicodeChar();
                            mEtSex.setText(String.valueOf(unicodeChar));
                            mEtSex.requestFocus();
                        }
                    }
                }
                return false;
            }
        });
    }

    private void setupSexInput() {
        mEtSex.addTextChangedListener(new TextWatcher() {
            final String originalHint = mEtSex.getHint().toString();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                /*if (count == 0 && TextUtils.isEmpty(mEtSex.getText())) {
                    mEtBirthday.setSelection(mEtBirthday.getText().length());
                    mEtBirthday.requestFocus();
                }*/
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (TextUtils.isEmpty(editable.toString().trim())) {
                    mEtSex.setHint(originalHint);
                } else {
                    mEtSex.setHint(null);
                }
            }
        });
        mEtSex.setOnKeyListener(new View.OnKeyListener() {
            final String TAG = "OnKeyListener";

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.i(TAG, "onKey:text:" + mEtSex.getText());
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (mEtSex.getText().length() == 0) {
                        mEtBirthday.setSelection(mEtBirthday.getText().length());
                        mEtBirthday.requestFocus();
                    }
                }
                return false;
            }
        });
    }

    @OnClick(R.id.phone_verify_button)
    void verifyPhoneNumber() {
        Log.i(TAG, "verifyPhoneNumber");

        CommonUtil.hideKeyboard(this);

        if (TextUtils.isEmpty(mEtUsername.getText().toString().trim())) {
            CommonUtil.showSnackbar(this, "이름을 입력해주세요.");
            mEtUsername.requestFocus();
            CommonUtil.delayCall(new Callable() {
                @Override
                public Object call() throws Exception {
                    CommonUtil.showKeyboard(IDVerificationActivity.this);
                    return null;
                }
            }, 1000);
            return;
        }

        if (!isValidBirthday()) {
            CommonUtil.showSnackbar(this, "주민번호 앞자리를 정확하게 입력해주세요.");
            mEtBirthday.requestFocus();
            CommonUtil.delayCall(new Callable() {
                @Override
                public Object call() throws Exception {
                    CommonUtil.showKeyboard(IDVerificationActivity.this);
                    return null;
                }
            }, 1000);
            return;
        }

        if (TextUtils.isEmpty(mEtSex.getText().toString().trim())) {
            CommonUtil.showSnackbar(this, "주민번호 뒷자리 첫번째 숫자를 입력해주세요.");
            mEtSex.requestFocus();
            CommonUtil.delayCall(new Callable() {
                @Override
                public Object call() throws Exception {
                    CommonUtil.showKeyboard(IDVerificationActivity.this);
                    return null;
                }
            }, 1000);
            return;
        }

        // TODO: +82로 시작하지 않는다면 앞에 +82를 붙혀준다.
        String phoneNumber = mEtPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            CommonUtil.showSnackbar(this, "휴대폰 번호를 입력해주세요.");
            mEtPhone.requestFocus();
            CommonUtil.delayCall(new Callable() {
                @Override
                public Object call() throws Exception {
                    CommonUtil.showKeyboard(IDVerificationActivity.this);
                    return null;
                }
            }, 1000);
            return;
        }

        if (phoneNumber.startsWith("0")) {
            phoneNumber = phoneNumber.substring(1);
        }

        // 국가번호가 존재하지 않는다면 국가번호를 붙힌다.
        if (phoneNumber.startsWith("+")) {
            if (!phoneNumber.startsWith("+82")) {
                CommonUtil.showSnackbar(this, "한국 마켓에서는 한국 휴대폰 번호만 등록 가능합니다.");
                mEtPhone.requestFocus();
                CommonUtil.delayCall(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        CommonUtil.showKeyboard(IDVerificationActivity.this);
                        return null;
                    }
                }, 1000);
                return;
            }
        } else {
            phoneNumber = "+82" + phoneNumber;
        }

        if (!CommonUtil.isValidNumber(phoneNumber)) {
            CommonUtil.showSnackbar(this, "정상적인 번호가 아닙니다.");
            mEtPhone.requestFocus();
            CommonUtil.delayCall(new Callable() {
                @Override
                public Object call() throws Exception {
                    CommonUtil.showKeyboard(IDVerificationActivity.this);
                    return null;
                }
            }, 1000);
            return;
        }

        if (mActionStatus == ACTION_STATUS_SECURITY_PIN_REGISTER) {
            // TODO: 저장된 번호와 대조해서 불일치할 경우 리턴시킨다.
            String username = mEtUsername.getText().toString().trim();
            String birthday = mEtBirthday.getText().toString().trim();
            int sexDigit = CommonUtil.toInt(mEtSex.getText().toString());
            String sex = getSexCode(sexDigit);
            if (!username.equals(mPrivateInfo.getUsername())) {
                CommonUtil.showSnackbar(this, "기존에 인증된 이름을 입력해주세요.");
                mEtUsername.requestFocus();
                CommonUtil.delayCall(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        CommonUtil.showKeyboard(IDVerificationActivity.this);
                        return null;
                    }
                }, 1000);
                return;
            }
            if (!birthday.equals(mPrivateInfo.getBirthday()) || !sex.equals(mPrivateInfo.getSex())) {
                CommonUtil.showSnackbar(this, "기존에 인증된 주민번호를 입력해주세요.");
                mEtBirthday.requestFocus();
                CommonUtil.delayCall(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        CommonUtil.showKeyboard(IDVerificationActivity.this);
                        return null;
                    }
                }, 1000);
                return;
            }
            if (!phoneNumber.equals(mPrivateInfo.getPhone())) {
                CommonUtil.showSnackbar(this, "기존에 인증된 폰번호를 입력해주세요.");
                mEtPhone.requestFocus();
                CommonUtil.delayCall(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        CommonUtil.showKeyboard(IDVerificationActivity.this);
                        return null;
                    }
                }, 1000);
                return;
            }
        }

        CommonUtil.disableViews(mEtUsername, mEtBirthday, mEtSex);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            final String TAG = "OnVerificationCallbacks";

            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                Log.i(TAG, "onVerificationCompleted:phoneAuthCredential" + phoneAuthCredential);
                mPhoneAuthCredential = phoneAuthCredential;
                updatePhoneVerificationUI(PHONE_VERIFY_STATE_VERIFY_SUCCESS);
                verify(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed:ERROR:", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // ...
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                }

                // 일단은 에러가 발생하면 기록할 수 있게 로깅을 남긴다.
                Crashlytics.logException(e);

                // Show a message and update the UI
                updatePhoneVerificationUI(PHONE_VERIFY_STATE_VERIFY_FAILED);
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                Log.i(TAG, "onCodeSent:verificationId" + verificationId);
                super.onCodeSent(verificationId, forceResendingToken);

                mVerificationId = verificationId;
                mResendToken = forceResendingToken;

                updatePhoneVerificationUI(PHONE_VERIFY_STATE_CODE_SENT);
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(String s) {
                Log.i(TAG, "onCodeAutoRetrievalTimeOut");
                super.onCodeAutoRetrievalTimeOut(s);
            }
        };

        updatePhoneVerificationUI(PHONE_VERIFY_STATE_REQUEST_CODE);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,              // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,       // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    @OnClick(R.id.phone_verify_resend_button)
    void resendVerificationCode(View view) {
        String phoneNumber = mEtPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            CommonUtil.showSnackbar(this, "휴대폰 번호를 입력해주세요.");
            mEtPhone.requestFocus();
            return;
        }

        // 국가번호가 존재하지 않는다면 국가번호를 붙힌다.
        if (phoneNumber.startsWith("+")) {
            if (!phoneNumber.startsWith("+82")) {
                CommonUtil.showSnackbar(this, "한국 마켓에서는 한국 휴대폰 번호만 등록 가능합니다.");
                mEtPhone.requestFocus();
                return;
            }
        } else {
            phoneNumber = "+82" + phoneNumber;
        }

        updatePhoneVerificationUI(PHONE_VERIFY_STATE_REQUEST_CODE);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,              // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,       // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                mResendToken);      // ForceResendingToken from callbacks
    }

    @OnClick(R.id.phone_verify_code_button)
    void verifyCode() {
        String code = mEtPhoneSmsCode.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            CommonUtil.showSnackbar(this, "SMS로 전송받은 6자리 인증코드를 입력해주세요.");
            mEtPhoneSmsCode.requestFocus();
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        verify(credential);
    }

    private void verify(final PhoneAuthCredential credential) {
        Log.i(TAG, "verifiy:credential_sms_code:" + credential.getSmsCode());
        showLoadingDialog();
        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Task<Void> updatePhoneNumberTask = AuthManager.updatePhoneNumber(credential);

                try {
                    Tasks.await(updatePhoneNumberTask);
                } catch (Exception e) {
                    dismissDialog();
                    Exception updatePhoneNumberError = updatePhoneNumberTask.getException();
                    Log.w(TAG, "updatePhoneNumberTask:ERROR:" + updatePhoneNumberError.getMessage());
                    if (updatePhoneNumberError instanceof FirebaseException) {
                        FirebaseException fe = (FirebaseException) updatePhoneNumberError;

                        // TODO: updatePhoneNumber를 쓰지않으려면 linkWithCredential를 써야한다.
                        // 대신에 linkWithCredential를 호출하기 전에 폰번호 프로바이더를 우선 삭제(unlink 사용)해야 한다.
                        // AuthManager.getOldUser().unlink(PhoneAuthProvider.PROVIDER_ID).addOnCompleteListener();

                        if (fe instanceof FirebaseAuthInvalidCredentialsException) {
                            CommonUtil.showSnackbar(IDVerificationActivity.this, "인증코드를 잘못 입력하셨습니다.");
                        } else if (fe instanceof FirebaseAuthUserCollisionException) {
                            // TODO: 연동된 폰번호를 삭제할 수 있는 버튼이 필요함.
                            CommonUtil.showSnackbar(IDVerificationActivity.this, "다른 계정에 이미 등록된 번호입니다.");
                        }
                    }
                    CommonUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updatePhoneVerificationUI(PHONE_VERIFY_STATE_UPDATE_FAILED);
                        }
                    });
                    return null;
                }

                if (mActionStatus == ACTION_STATUS_ID_VERIFY || mActionStatus == ACTION_STATUS_CARD_REGISTER) {
                    String username = mEtUsername.getText().toString().trim();
                    String birthday = mEtBirthday.getText().toString().trim();
                    int sexDigit = CommonUtil.toInt(mEtSex.getText().toString());
                    String sex = getSexCode(sexDigit);
                    String phone = AuthManager.getPhoneNumber();

                    PrivateInfo privateInfo = new PrivateInfo();
                    privateInfo.setUsername(username);
                    privateInfo.setBirthday(birthday);
                    privateInfo.setSex(sex);
                    privateInfo.setPhone(phone);

                    Task<Void> updatePrivateInfoTask = UserUtil.updatePrivateInfo(privateInfo);
                    Tasks.await(updatePrivateInfoTask);
                    if (!updatePrivateInfoTask.isSuccessful()) {
                        dismissDialog();
                        Exception updatePrivateInfoError = updatePrivateInfoTask.getException();
                        Log.w(TAG, "verify:updatePrivateInfoError:" + updatePrivateInfoError.getMessage());
                        CommonUtil.showSnackbar(IDVerificationActivity.this, "일시적인 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.");
                        return null;
                    }
                }

                Log.d(TAG, "updatePhoneNumber:SUCCESS");
                updatePhoneVerificationUI(PHONE_VERIFY_STATE_UPDATE_SUCCESS);

                dismissDialog();

                DialogAlertFragment alertDialog = DialogAlertFragment.newInstance(
                        "인증이 확인되었습니다.",
                        new DialogAlertFragment.OnDoneListener() {
                            @Override
                            public void onDone() {
                                super.onDone();
                                onSuccessVerification();
                            }
                        },
                        false
                );
                alertDialog.show(getSupportFragmentManager(), null);

                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "verify:ERROR:", task.getException());
                    return null;
                }
                return null;
            }
        });
    }

    private void onSuccessVerification() {
        if (mActionStatus == ACTION_STATUS_ID_VERIFY) {
            setResult(RESULT_OK);
            finish();
        }
        if (mActionStatus == ACTION_STATUS_CARD_REGISTER) {
            startCardRegistrationActivity();
        }
        if (mActionStatus == ACTION_STATUS_SECURITY_PIN_REGISTER) {
            startSecurityPinActivity();
        }
    }

    private void startCardRegistrationActivity() {
        Intent in = CardRegistrationActivity.createIntent(this, ACTION_STATUS_INITIAL_REGISTER);
        startActivityForResult(in, CARD_REG_ACTIVITY);
        setResult(RESULT_OK);
        finish();
    }

    private void startSecurityPinActivity() {
        Intent in = SecurityPinActivity.createIntent(this, ACTION_STATUS_REGISTER);
        startActivityForResult(in, SECURITY_PIN_ACTIVITY);
        setResult(RESULT_OK);
        finish();
    }

    private void updatePhoneVerificationUI(int uiState) {
        switch (uiState) {
            case PHONE_VERIFY_STATE_INITIALIZED:
                mEtPhone.clearFocus();
                mBtnPhoneVerify.setEnabled(true);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyResend, View.GONE);
                CommonUtil.setVisibilityWithSlide(mVgPhoneSmsCodeTab, View.GONE);
                mEtPhone.setEnabled(true);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerify, View.VISIBLE);
                break;
            case PHONE_VERIFY_STATE_MODIFY:
                mEtPhone.setEnabled(true);
                mEtPhone.setSelection(mEtPhone.getText().length());
                mEtPhone.requestFocus();
                CommonUtil.showKeyboard(this);
                mBtnPhoneVerify.setEnabled(true);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerify, View.VISIBLE);
                break;
            case PHONE_VERIFY_STATE_REQUEST_CODE:
                showLoadingDialog();
                mBtnPhoneVerifyResend.setEnabled(false);
                CommonUtil.hideKeyboard(this);
                break;
            case PHONE_VERIFY_STATE_CODE_SENT:
                dismissDialog();
                // TODO: 인증번호를 전송했더라도 폰번호를 바꾸고 싶을 수도 있다.
                mEtPhone.clearFocus();
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerify, View.GONE);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyResend, View.VISIBLE);
                mBtnPhoneVerifyResend.setEnabled(true);
                CommonUtil.setVisibilityWithSlide(mVgPhoneSmsCodeTab, View.VISIBLE);
                mEtPhoneSmsCode.setText(null);
                mEtPhoneSmsCode.setEnabled(true);
                mBtnPhoneVerifyCode.setEnabled(true);
                break;
            // 잘못된 폰번호이거나 Quata를 초과했을 때 호출 됨.
            case PHONE_VERIFY_STATE_VERIFY_FAILED:
                dismissDialog();
                // Verification has failed, show all options
                // enableViews(mStartButton, mVerifyButton, mResendButton, mPhoneNumberField, mVerificationField);
                // mDetailText.setText(R.string.status_verification_failed);
                mEtPhone.setEnabled(true);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerify, View.VISIBLE);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyResend, View.GONE);
                mBtnPhoneVerifyResend.setEnabled(false);
                break;
            // 자동적으로 인증이 성공되었을 때. (인증코드도 일치하는 상황)
            case PHONE_VERIFY_STATE_VERIFY_SUCCESS:
                // mBtnPhoneVerifyResend.setEnabled(false);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyResend, View.GONE);
                if (mPhoneAuthCredential != null)
                    mEtPhoneSmsCode.setText(mPhoneAuthCredential.getSmsCode());
                mEtPhone.setEnabled(false);
                mEtPhoneSmsCode.setEnabled(false);
                mBtnPhoneVerifyCode.setEnabled(false);
                break;
            // 이미 다른 사람에게 등록된 폰 번호일 경우, 수동으로 인증코드 입력한 뒤 인증코드가 잘못되었을 때
            case PHONE_VERIFY_STATE_UPDATE_FAILED:
                // No-op, handled by sign-in check
                CommonUtil.enableViews(mEtPhone, mEtPhoneSmsCode, mBtnPhoneVerify, mBtnPhoneVerifyResend);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyResend, View.GONE);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerify, View.VISIBLE);
                CommonUtil.setVisibilityWithSlide(mVgPhoneSmsCodeTab, View.GONE);
                break;
            case PHONE_VERIFY_STATE_UPDATE_SUCCESS:
                // Np-op, handled by sign-in check
                mEtPhone.setEnabled(false);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyResend, View.GONE);
                CommonUtil.setVisibilityWithSlide(mVgPhoneSmsCodeTab, View.GONE);
                break;
        }

        /*if (user == null) {
            // Signed out
            mPhoneNumberViews.setVisibility(View.VISIBLE);
            mSignedInViews.setVisibility(View.GONE);

            mStatusText.setText(R.string.signed_out);
        } else {
            // Signed in
            mPhoneNumberViews.setVisibility(View.GONE);
            mSignedInViews.setVisibility(View.VISIBLE);

            enableViews(mPhoneNumberField, mVerificationField);
            mPhoneNumberField.setText(null);
            mVerificationField.setText(null);

            mStatusText.setText(R.string.signed_in);
            mDetailText.setText(getString(R.string.firebase_status_fmt, user.getUserId()));
        }*/
    }

    private boolean isValidBirthday() {
        String birthday = mEtBirthday.getText().toString().trim();
        if (birthday.length() != 6) return false;
        SimpleDateFormat format = new SimpleDateFormat("yyMMdd", Locale.getDefault());
        try {
            Date birthdayDate = format.parse(birthday);
            if (format.format(birthdayDate).equals(birthday)) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private String getSexCode(int sexDigit) {
        String sexCode;
        if (sexDigit % 2 != 0) {
            sexCode = "M";
        } else {
            sexCode = "F";
        }
        return sexCode;
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

    // TODO: 그냥 현재 상태가 최초결제정보 등록을 위한 상태를 명시하자.

    public static Intent createIntent(Context context, int actionStatus) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, IDVerificationActivity.class);
        intent.putExtra(ACTION_STATUS, actionStatus);
        return intent;
    }
}