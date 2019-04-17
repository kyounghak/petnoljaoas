package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.chaigene.petnolja.ui.dialog.DialogConfirmFragment;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.model.PrivateInfo;
import com.chaigene.petnolja.ui.activity.DaumPostcodeActivity;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.DAUM_POSTCODE_ACTIVITY;

public class SettingShopinfoFragment extends ChildFragment {
    public static final String TAG = "SettingProfileFragment";

    private static final int PHONE_VERIFY_STATE_INITIALIZED = 1;
    private static final int PHONE_VERIFY_STATE_MODIFY = 2;
    private static final int PHONE_VERIFY_STATE_REQUEST_CODE = 3;
    private static final int PHONE_VERIFY_STATE_CODE_SENT = 4;
    private static final int PHONE_VERIFY_STATE_VERIFY_FAILED = 5;
    private static final int PHONE_VERIFY_STATE_VERIFY_SUCCESS = 6;
    private static final int PHONE_VERIFY_STATE_UPDATE_FAILED = 7;
    private static final int PHONE_VERIFY_STATE_UPDATE_SUCCESS = 8;

    @BindView(R.id.email_input)
    TextView mEtEmail;

    @BindView(R.id.phone_input)
    EditText mEtPhone;

    @BindView(R.id.phone_edit_button)
    Button mBtnPhoneEdit;

    @BindView(R.id.phone_verify_cancel_button)
    Button mBtnPhoneVerifyCancel;

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

    @BindView(R.id.username_input)
    EditText mEtUsername;

    @BindView(R.id.address_input)
    EditText mEtAddress;

    @BindView(R.id.detail_address_tab)
    ViewGroup mVgDetailAddressTab;

    @BindView(R.id.detail_address_input)
    EditText mEtDetailAddress;

    @BindView(R.id.bank_account_holder_input)
    EditText mEtBankAccountHolder;

    @BindView(R.id.bank_name_input)
    EditText mEtBankName;

    @BindView(R.id.bank_account_input)
    EditText mEtBankAccount;

    // @Nullable
    // private OldUser mMyUser;

    private PrivateInfo mPrivateInfo;

    private PhoneAuthCredential mPhoneAuthCredential;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    public static SettingShopinfoFragment newInstance() {
        SettingShopinfoFragment fragment = new SettingShopinfoFragment();

        // Bundle args = new Bundle();
        // args.putInt(ACTION_STATUS, status);
        // fragment.setArguments(args);

        return fragment;
    }

    /*@Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        // Log.d(TAG, "readBundle:actionStatus:" + mActionStatus);
    }*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_setting_shopinfo, container, false);
        ButterKnife.bind(this, mView);

        showLoadingDialog();
        asyncTask().continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    // ERROR
                }
                // SUCCESS
                initView();
                return null;
            }
        });

        return mView;
    }

    private Task<Void> asyncTask() {
        /*showLoadingDialog();
        final Task<Void> downloadProfileImageTask = UserUtil.downloadProfileImage(AuthManager.getUserId(), mCivProfileImage);
        final Task<Void> loadUserInformationTask = loadUserInformation();
        Tasks.whenAll(downloadProfileImageTask, loadUserInformationTask).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!downloadProfileImageTask.isSuccessful()) {
                    Log.w(TAG, "onCreate:downloadProfileImageTask:ERROR:", task.getException());
                }

                if (!loadUserInformationTask.isSuccessful()) {
                    Log.w(TAG, "onCreate:loadUserInformationTask:ERROR:", task.getException());
                }

                Log.d(TAG, "onCreate:loadUserInformation+downloadProfileImage:SUCCESS");
                setInitializedAsyncTask(true);
                dismissDialog();
                return null;
            }
        });*/

        /*return UserUtil.getPrivateInfo(AuthManager.getUserId()).continueWith(new Continuation<PrivateInfo, Void>() {
            @Override
            public Void then(@NonNull Task<PrivateInfo> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                mPrivateInfo = task.getResult() != null ? task.getResult() : new PrivateInfo();

                // 회원정보를 설정한 적이 없으면 무조건 모든 값이 null이다.
                if (mPrivateInfo.getEmail() == null) {
                    mPrivateInfo.setEmail(AuthManager.getEmail());
                }

                if (mPrivateInfo.getPhone() == null) {
                    mPrivateInfo.setPhone(AuthManager.getPhoneNumber());
                }

                return null;
            }
        });*/
        return Tasks.forResult(null);
    }

    @Override
    protected void initView() {
        // setupNicknameInput();
        mEtEmail.setText(mPrivateInfo.getEmail());
        mEtPhone.setText(mPrivateInfo.getPhone());
        mEtUsername.setText(mPrivateInfo.getUsername());
        mEtAddress.setText(mPrivateInfo.getAddress());

        updatePhoneVerificationUI(PHONE_VERIFY_STATE_INITIALIZED);
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        setToolbarTitle("회원정보 수정");
        setToolbarTitleAlign(Gravity.CENTER_HORIZONTAL);
    }

    private void setupNicknameInput() {
        Log.i(TAG, "setupNicknameInput");

        /*if (mActionStatus == ACTION_STATUS_UPDATE) {
            mEtNickname.setEnabled(false);
            return;
        }*/

        /*mEtNickname.addTextChangedListener(new TextWatcher() {
            final String TAG = "TextWatcher";

            final Pattern UPPER_CASE_REGEX = Pattern.compile("[A-Z]");

            String oldText;
            String newText;
            int oldCursor;
            int newCursor;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.i(TAG, "beforeTextChanged:s:" + s + "|start:" + start + "|count:" + count + "|after:" + after);

                oldText = s.toString();
                oldCursor = mEtNickname.getSelectionStart();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.i(TAG, "onTextChanged:s:" + s + "|start:" + start + "|before:" + before + "|count:" + count);

                newText = s.toString();
                newCursor = mEtNickname.getSelectionStart();
                Log.d(TAG, "onTextChanged:newText:" + newText);
            }

            @Override
            public void afterTextChanged(final Editable s) {
                Log.i(TAG, "afterTextChanged:s:" + s);

                if (TextUtils.isEmpty(s.toString())) return;

                if (!isNicknameValidChars(s.toString())) {
                    Log.d(TAG, "onTextChanged:isNicknameValidChars:false");
                    Log.d(TAG, "onTextChanged:oldText:" + oldText);

                    mEtNickname.removeTextChangedListener(this);
                    s.clear();
                    s.insert(0, oldText);
                    mEtNickname.setSelection(oldCursor);
                    mEtNickname.addTextChangedListener(this);

                    CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_nickname);
                } else {
                    Log.d(TAG, "onTextChanged:isNicknameValidChars:true");

                    // Ref: https://gist.github.com/karolw/549cb5c1ef46c008d4b1
                    Matcher matcher = UPPER_CASE_REGEX.matcher(s);
                    while (matcher.find()) {
                        CharSequence upperCaseRegion = s.subSequence(matcher.start(), matcher.end());
                        s.replace(matcher.start(), matcher.end(), upperCaseRegion.toString().toLowerCase());
                    }
                }
            }
        });*/
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
                        mEtAddress.setText(address.trim());
                        mVgDetailAddressTab.setVisibility(View.VISIBLE);
                        mEtDetailAddress.setText(null);
                        mEtDetailAddress.requestFocus();
                    }
                }

                if (resultCode == RESULT_CANCELED) {
                }
                break;
        }
    }

    /*private Task<Void> loadUserInformation() {
        return UserUtil.getOldUser(AuthManager.getUserId()).continueWith(new Continuation<OldUser, Void>() {
            @Override
            public Void then(@NonNull Task<OldUser> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "loadUserInformation:ERROR:", task.getException());
                    return null;
                }

                OldUser user = task.getResult();
                Log.d(TAG, "loadUserInformation:user:" + user.toMap().toString());

                mEtNickname.setText(user.getNickname());
                mEtDescription.setText(user.getDescription());

                mMyUser = user;
                return null;
            }
        });
    }*/

    @OnClick(R.id.phone_edit_button)
    void editPhoneNumber() {
        updatePhoneVerificationUI(PHONE_VERIFY_STATE_MODIFY);
    }

    @OnClick(R.id.phone_verify_cancel_button)
    void phoneVerifyCancel() {
        updatePhoneVerificationUI(PHONE_VERIFY_STATE_INITIALIZED);
    }

    @OnClick(R.id.phone_verify_button)
    void verifyPhoneNumber(View view) {
        // TODO: +82로 시작하지 않는다면 앞에 +82를 붙혀준다.
        String phoneNumber = mEtPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            CommonUtil.showSnackbar(getActivity(), "휴대폰 번호를 입력해주세요.");
            mEtPhone.requestFocus();
            return;
        }

        // 국가번호가 존재하지 않는다면 국가번호를 붙힌다.
        if (phoneNumber.startsWith("+")) {
            if (!phoneNumber.startsWith("+82")) {
                CommonUtil.showSnackbar(getActivity(), "한국 마켓에서는 한국 휴대폰 번호만 등록 가능합니다.");
                mEtPhone.requestFocus();
                return;
            }
        } else {
            phoneNumber = "+82" + phoneNumber;
        }

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            final String TAG = "OnVerificationCallbacks";

            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                Log.i(TAG, "onVerificationCompleted:phoneAuthCredential" + phoneAuthCredential);
                mPhoneAuthCredential = phoneAuthCredential;
                updatePhoneVerificationUI(PHONE_VERIFY_STATE_VERIFY_SUCCESS);
                updatePhoneNumber(phoneAuthCredential);
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
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                getActivity(),      // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    @OnClick(R.id.phone_verify_resend_button)
    void resendVerificationCode(View view) {
        String phoneNumber = mEtPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            CommonUtil.showSnackbar(getActivity(), "휴대폰 번호를 입력해주세요.");
            mEtPhone.requestFocus();
            return;
        }

        // 국가번호가 존재하지 않는다면 국가번호를 붙힌다.
        if (phoneNumber.startsWith("+")) {
            if (!phoneNumber.startsWith("+82")) {
                CommonUtil.showSnackbar(getActivity(), "한국 마켓에서는 한국 휴대폰 번호만 등록 가능합니다.");
                mEtPhone.requestFocus();
                return;
            }
        } else {
            phoneNumber = "+82" + phoneNumber;
        }

        updatePhoneVerificationUI(PHONE_VERIFY_STATE_REQUEST_CODE);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                getActivity(),      // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                mResendToken);      // ForceResendingToken from callbacks
    }

    @OnClick(R.id.phone_verify_code_button)
    void verifyCode() {
        String code = mEtPhoneSmsCode.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            CommonUtil.showSnackbar(getActivity(), "SMS로 전송받은 6자리 인증코드를 입력해주세요.");
            mEtPhoneSmsCode.requestFocus();
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        updatePhoneNumber(credential);
    }

    private void updatePhoneNumber(PhoneAuthCredential credential) {
        showLoadingDialog();
        AuthManager.updatePhoneNumber(credential).addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                dismissDialog();
                if (task.isSuccessful()) {
                    Log.d(TAG, "updatePhoneNumber:SUCCESS");
                    updatePhoneVerificationUI(PHONE_VERIFY_STATE_UPDATE_SUCCESS);
                } else {
                    Log.w(TAG, "updatePhoneNumber:ERROR", task.getException());
                    if (task.getException() instanceof FirebaseException) {
                        FirebaseException e = (FirebaseException) task.getException();

                        // TODO: updatePhoneNumber를 쓰지않으려면 linkWithCredential를 써야한다.
                        // 대신에 linkWithCredential를 호출하기 전에 폰번호 프로바이더를 우선 삭제해야 한다.

                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            // mVerificationField.setError("Invalid code.");
                            CommonUtil.showSnackbar(getActivity(), "인증코드를 잘못 입력하셨습니다.");
                        } else if (e instanceof FirebaseAuthUserCollisionException) {
                            // mVerificationField.setError("This credential is already associated with a different user account.");
                            // TODO: 연동된 폰번호를 삭제할 수 있는 버튼이 필요함.
                            CommonUtil.showSnackbar(getActivity(), "다른 계정에 이미 등록된 번호입니다.");
                        }

                        updatePhoneVerificationUI(PHONE_VERIFY_STATE_UPDATE_FAILED);
                    }
                }
            }
        });
    }

    private void updatePhoneVerificationUI(int uiState) {
        switch (uiState) {
            case PHONE_VERIFY_STATE_INITIALIZED:
                mEtPhone.clearFocus();
                mEtPhone.setText(mPrivateInfo.getPhone());

                mBtnPhoneVerify.setEnabled(true);

                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyResend, View.GONE);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyCancel, View.GONE);
                CommonUtil.setVisibilityWithSlide(mVgPhoneSmsCodeTab, View.GONE);

                if (mPrivateInfo.getPhone() != null) {
                    mEtPhone.setEnabled(false);
                    // mBtnPhoneEdit.setVisibility(View.VISIBLE);
                    // mBtnPhoneVerify.setVisibility(View.GONE);

                    CommonUtil.setVisibilityWithFade(mBtnPhoneEdit, View.VISIBLE);
                    CommonUtil.setVisibilityWithFade(mBtnPhoneVerify, View.GONE);
                } else {
                    mEtPhone.setEnabled(true);
                    // mBtnPhoneEdit.setVisibility(View.GONE);
                    // mBtnPhoneVerify.setVisibility(View.VISIBLE);

                    CommonUtil.setVisibilityWithFade(mBtnPhoneEdit, View.GONE);
                    CommonUtil.setVisibilityWithFade(mBtnPhoneVerify, View.VISIBLE);
                }
                break;
            case PHONE_VERIFY_STATE_MODIFY:
                mEtPhone.setEnabled(true);
                mEtPhone.setSelection(mEtPhone.getText().length());
                mEtPhone.requestFocus();
                CommonUtil.showKeyboard(getActivity());
                mBtnPhoneVerify.setEnabled(true);

                CommonUtil.setVisibilityWithFade(mBtnPhoneEdit, View.GONE);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerify, View.VISIBLE);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyCancel, View.VISIBLE);
                break;
            case PHONE_VERIFY_STATE_REQUEST_CODE:
                showLoadingDialog();
                mBtnPhoneVerifyResend.setEnabled(false);
                CommonUtil.hideKeyboard(getActivity());
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
                mBtnPhoneVerifyResend.setEnabled(false);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyCancel, View.GONE);
                if (mPhoneAuthCredential != null)
                    mEtPhoneSmsCode.setText(mPhoneAuthCredential.getSmsCode());
                mEtPhoneSmsCode.setEnabled(false);
                mBtnPhoneVerifyCode.setEnabled(false);
                // CommonUtil.setVisibilityWithSlide(mVgPhoneSmsCodeTab, View.GONE);
                break;
            // 이미 다른 사람에게 등록된 폰 번호일 경우, 수동으로 인증코드 입력한 뒤 인증코드가 잘못되었을 때
            case PHONE_VERIFY_STATE_UPDATE_FAILED:
                // No-op, handled by sign-in check
                // mDetailText.setText(R.string.status_sign_in_failed);
                break;
            case PHONE_VERIFY_STATE_UPDATE_SUCCESS:
                // Np-op, handled by sign-in check
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyResend, View.GONE);
                CommonUtil.setVisibilityWithFade(mBtnPhoneVerifyCancel, View.GONE);
                CommonUtil.setVisibilityWithSlide(mVgPhoneSmsCodeTab, View.GONE);
                CommonUtil.setVisibilityWithFade(mBtnPhoneEdit, View.VISIBLE);
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

    @OnClick({R.id.address_tab, R.id.address_input})
    void startDaumPostcodeActivity() {
        Intent intent = createIntent(DaumPostcodeActivity.class);
        startActivityForResult(intent, DAUM_POSTCODE_ACTIVITY);
    }

    @OnClick(R.id.detail_address_confirm_button)
    void makeCompleteAddress() {
        String detailAddress = mEtDetailAddress.getText().toString().trim();
        /*if (TextUtils.isEmpty(detailAddress)) {
            CommonUtil.showSnackbar(getActivity(), "상세주소를 입력해주세요.");
            mEtDetailAddress.requestFocus();
            return;
        }*/
        String address = mEtAddress.getText().toString().trim();
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
                        mEtAddress.setText(completeAddress);
                        mVgDetailAddressTab.setVisibility(View.GONE);
                        mEtDetailAddress.setText(null);
                    }
                }
        );
        dialogConfirmFragment.show(getFragmentManager(), null);
    }

    private void updateShopinfo() {
        Log.i(TAG, "updateShopinfo");

        CommonUtil.hideKeyboard(getActivity());

        final String email = mEtEmail.getText().toString().trim();
        final String phone = mEtPhone.getText().toString().trim();
        final String username = mEtUsername.getText().toString().trim();
        final String address = mEtAddress.getText().toString().trim();

        boolean isCancelled = false;
        View focusView = null;

        if (TextUtils.isEmpty(username)) {
            CommonUtil.showSnackbar(getActivity(), "이름을 입력해주세요.");
            focusView = mEtUsername;
            isCancelled = true;
        }

        if (!isUsernameValid(username)) {
            CommonUtil.showSnackbar(getActivity(), "이름은 10자 이하로 작성해주세요.");
            focusView = mEtUsername;
            isCancelled = true;
        }

        if (!isAddressValid(address)) {
            CommonUtil.showSnackbar(getActivity(), "프로필 설명은 1000자 이하로 작성해주세요.");
            focusView = mEtAddress;
            isCancelled = true;
        }

        if (isCancelled) {
            focusView.requestFocus();
        } else {
            showLoadingDialog();

            Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // Task<Boolean> isNicknameExistsTask = UserUtil.isOldNicknameExists(nickname);
                    // Boolean isOldNicknameExists = Tasks.await(isNicknameExistsTask);

                    /*if (!isNicknameExistsTask.isSuccessful()) {
                        dismissDialog();
                        Log.w(TAG, "isNicknameExistsTask:ERROR:", isNicknameExistsTask.getException());
                        CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                        return null;
                    }*/

                    /*if (isOldNicknameExists) {
                        CommonUtil.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dismissDialog();
                                CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_nickname_duplication);
                                mEtNickname.requestFocus();
                            }
                        });
                        return null;
                    }*/

                    /*FIRUser firUser = new FIRUser();
                    firUser.setKey(AuthManager.getUserId());
                    firUser.setNickname(nickname);
                    firUser.setDescription(description);
                    UserUtil.update(firUser).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(@NonNull Task<Void> task) throws Exception {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "updateUserInformation:ERROR");
                                CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                                return null;
                            }
                            Log.d(TAG, "updateUserInformation:SUCCESS");

                            ConfigManager.getInstance(getContext()).setNickname(nickname);
                            ConfigManager.getInstance(getContext()).setEmail(AuthManager.getEmail());

                            ((BaseActivity) getActivity()).onFragmentResult(SETTING_PROFILE_FRAGMENT, RESULT_OK, null);
                            return null;
                        }
                    });*/
                    return null;
                }
            }).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(@NonNull Task<Void> task) throws Exception {
                    dismissDialog();
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "updateShopinfo:ERROR:" + task.getException().getMessage());
                    }
                    return null;
                }
            });
        }
    }

    /*private boolean isNicknameValid(String nickname) {
        return isNicknameValidChars(nickname);
    }*/

    /*private boolean isNicknameValidChars(String nickname) {
        return nickname.matches("([A-Za-z0-9\\_]+)");
    }*/

    // 이름 검증
    private boolean isUsernameValid(String username) {
        return username.length() <= 10;
    }

    // 주소 검증
    private boolean isAddressValid(String address) {
        return address.length() <= 1000;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (isPrimaryFragment()) {
            // 우측 메뉴 생성: 확인
            inflater.inflate(R.menu.menu_fragment_setting_shopinfo, menu);
            getToolbar().setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                final String TAG = "OnClickListener";

                @Override
                public void onClick(View v) {
                    Log.i(TAG, "onClick");
                    finish();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected");

        switch (item.getItemId()) {
            // TODO: 현재 상태를 저장한다.
            case R.id.action_confirm:
                // Intent intent = createIntent(ChatRoomActivity.class);
                // startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void finish() {
        // 임시 조취.
        getActivity().onBackPressed();
    }
}