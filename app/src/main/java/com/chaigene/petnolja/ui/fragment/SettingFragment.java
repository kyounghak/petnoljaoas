package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.chaigene.petnolja.ui.activity.LauncherActivity;
import com.chaigene.petnolja.ui.dialog.DialogConfirmFragment;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.PhoneAuthProvider;
import com.chaigene.petnolja.BuildConfig;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.ConfigManager;
import com.chaigene.petnolja.ui.activity.ChatActivity;
import com.chaigene.petnolja.ui.activity.IDVerificationActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.NotificationUtil;
import com.chaigene.petnolja.util.ShopUtil;
import com.chaigene.petnolja.util.UserUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.CHAT_ACTIVITY;
import static com.chaigene.petnolja.Constants.ID_VERIFICATION_ACTIVITY;
import static com.chaigene.petnolja.Constants.SECURITY_PIN_ACTIVITY;
import static com.chaigene.petnolja.Constants.SETTING_PROFILE_FRAGMENT;
import static com.chaigene.petnolja.Constants.SETTING_SHOPINFO_FRAGMENT;
import static com.chaigene.petnolja.ui.activity.IDVerificationActivity.ACTION_STATUS_ID_VERIFY;
import static com.chaigene.petnolja.ui.fragment.SettingProfileFragment.ACTION_STATUS_UPDATE;

// Tab을 누를 때마다 getRootFragment를 호출하여 세부 프래그먼트를 붙인다.
public class SettingFragment extends ChildFragment {
    public static final String TAG = "SettingFragment";

    @BindView(R.id.profile_edit_tab)
    View mVgProfileEditTab;

    @BindView(R.id.unlink_phone_tab)
    View mVgUnlinkPhoneTab;

    @BindView(R.id.verify_retry_tab)
    View mVgVerifyRetryTab;

    @BindView(R.id.security_pin_reset_tab)
    View mVgSecurityPinResetTab;

    @BindView(R.id.fake_payment_tab)
    View mVgFakePaymentTab;

    @BindView(R.id.fake_payment_switch)
    SwitchCompat mSwFakePayment;

    @BindView(R.id.signout_tab)
    View mVgSignoutTab;

    boolean mIsSecurityPinExists;

    public boolean isSecurityPinExists() {
        return mIsSecurityPinExists;
    }

    public void setSecurityPinExists(boolean securityPinExists) {
        this.mIsSecurityPinExists = securityPinExists;
    }

    public static SettingFragment newInstance() {
        SettingFragment fragment = new SettingFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_setting, container, false);
        ButterKnife.bind(this, mView);
        showLoadingDialog();
        asyncTask().continueWith((Continuation<Void, Void>) task -> {
            dismissDialog();
            if (!task.isSuccessful()) {
                return null;
            }
            initView();
            return null;
        });
        return mView;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        // mTargetUid = bundle.getString(EXTRA_TARGET_USER_ID);
        // mTargetUser = (OldUser) bundle.getSerializable(EXTRA_TARGET_USER);
    }

    private Task<Void> asyncTask() {
        return ShopUtil.isSecurityPinExists().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            setSecurityPinExists(task.getResult());
            return null;
        });
    }

    @Override
    protected void initView() {
        Log.i(TAG, "initView");
        super.initView();
        /*if (AuthManager.getUserId().equals("z98ZqYCzvFSmkMyH8IacJYLUoWC2") || BuildConfig.DEBUG) {
            mVgVerifyRetryTab.setVisibility(View.VISIBLE);
            mVgSecurityPinResetTab.setVisibility(View.VISIBLE);
        }*/
        if (isSecurityPinExists()) {
            mVgSecurityPinResetTab.setVisibility(View.VISIBLE);
        }
        if (BuildConfig.DEBUG) {
            mVgUnlinkPhoneTab.setVisibility(View.VISIBLE);
            boolean isFakePaymentMode = ConfigManager.getInstance(getContext()).isFakePaymentMode();
            mSwFakePayment.setChecked(isFakePaymentMode);
            mSwFakePayment.setOnCheckedChangeListener((buttonView, isChecked) -> ConfigManager.getInstance(getContext()).setFakePaymentMode(isChecked));
            mVgFakePaymentTab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        setToolbarTitle("설정");
        setToolbarTitleAlign(Gravity.CENTER_HORIZONTAL);
    }

    @OnClick(R.id.profile_edit_tab)
    void profileEdit() {
        SettingProfileFragment settingProfileFragment = SettingProfileFragment.newInstance(ACTION_STATUS_UPDATE);
        settingProfileFragment.setTargetFragment(this, SETTING_PROFILE_FRAGMENT);
        getRootFragment().add(settingProfileFragment, true);
    }

    @OnClick(R.id.unlink_phone_tab)
    void unlinkPhone() {
        showLoadingDialog();
        AuthManager.getUser().unlink(PhoneAuthProvider.PROVIDER_ID).continueWith((Continuation<AuthResult, Void>) task -> {
            dismissDialog();
            if (!task.isSuccessful()) {
                CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                return null;
            }
            CommonUtil.showSnackbar(getActivity(), "연동된 휴대폰 번호가 삭제되었습니다.");
            return null;
        });
    }

    @OnClick(R.id.verify_retry_tab)
    void verifyRetry() {
        DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "기존에 인증된 정보를 삭제하고 새로운 사용자 정보로 다시 인증하시겠습니까?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        showLoadingDialog();
                        UserUtil.resetPhoneVerification().continueWith((Continuation<Void, Void>) task -> {
                            dismissDialog();
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "verifyRetry:updatePrivateInfo:ERROR:" + task.getException().getMessage());
                                CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                                return null;
                            }
                            Intent in = IDVerificationActivity.createIntent(getContext(), ACTION_STATUS_ID_VERIFY);
                            startActivityForResult(in, ID_VERIFICATION_ACTIVITY);
                            return null;
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

    @OnClick(R.id.security_pin_reset_tab)
    void securityPinReset() {
        Intent in = IDVerificationActivity.createIntent(getContext(), IDVerificationActivity.ACTION_STATUS_SECURITY_PIN_REGISTER);
        startActivityForResult(in, SECURITY_PIN_ACTIVITY);

        // Intent in = SecurityPinActivity.createIntent(getContext(), ACTION_STATUS_REGISTER);
        // startActivityForResult(in, SECURITY_PIN_ACTIVITY);
    }

    @OnClick(R.id.shopinfo_edit_tab)
    void shopinfoEdit() {
        SettingShopinfoFragment settingShopinfoFragment = SettingShopinfoFragment.newInstance();
        settingShopinfoFragment.setTargetFragment(this, SETTING_SHOPINFO_FRAGMENT);
        getRootFragment().add(settingShopinfoFragment, true);
    }

    @OnClick(R.id.signout_tab)
    void signout() {
        DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "로그아웃 하시겠습니까?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        showLoadingDialog();
                        NotificationUtil.deleteToken().continueWith((Continuation<Void, Void>) task -> {
                            dismissDialog();
                            if (!task.isSuccessful()) {
                                // 에러가 발생해도 로그아웃 한다.
                            }
                            AuthManager.signOut(getContext());

                            // HOTFIX: 로그아웃을 발생하면 다시 익명 로그인을 시켜줘야 하기 때문에
                            // 반드시 LauncherActivity를 실행시켜주어야 한다.
                            Intent intent = createIntent(LauncherActivity.class);
                            // Intent intent = createIntent(LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            getActivity().startActivity(intent);
                            getActivity().finish();
                            return null;
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

    @OnClick(R.id.pandaz_introduce_tab)
    void showPandazIntroduce() {
        SettingWebViewFragment settingWebViewFragment = SettingWebViewFragment.newInstance("판다즈 소개", "https://pandazmaker.com/");
        getRootFragment().add(settingWebViewFragment, true);
    }

    @OnClick(R.id.privacy_terms_tab)
    void showPrivacyTerms() {
        SettingWebViewFragment settingWebViewFragment = SettingWebViewFragment.newInstance("개인정보처리방침", "https://pandazmaker.com/policy.html");
        getRootFragment().add(settingWebViewFragment, true);
    }

    @OnClick(R.id.service_terms_tab)
    void showServiceTerms() {
        SettingWebViewFragment settingWebViewFragment = SettingWebViewFragment.newInstance("약관", "https://pandazmaker.com/clause.html");
        getRootFragment().add(settingWebViewFragment, true);
    }

    @OnClick(R.id.ask_tab)
    void ask() {
        showLoadingDialog();
        UserUtil.getUserIdByNickname("pandaz").continueWith((Continuation<String, Void>) task -> {
            dismissDialog();
            if (!task.isSuccessful()) {
                CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                return null;
            }
            String targetUid = task.getResult();
            if (targetUid == null) {
                CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                return null;
            }
            Intent intent = ChatActivity.createIntent(getContext(), targetUid);
            startActivityForResult(intent, CHAT_ACTIVITY);
            return null;
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));
        if (requestCode == SETTING_PROFILE_FRAGMENT) {
            if (resultCode == RESULT_OK) {
                CommonUtil.showSnackbar(getActivity(), "프로필이 업데이트 되었습니다.");
                return;
            }
        }
        if (requestCode == ID_VERIFICATION_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                CommonUtil.showSnackbar(getActivity(), "인증정보가 업데이트 되었습니다.");
                return;
            }
        }
        if (requestCode == SECURITY_PIN_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                CommonUtil.showSnackbar(getActivity(), "결제비밀번호가 재설정 되었습니다.");
                return;
            }
        }
    }

    // 일단 메뉴가 없다고 가정.
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
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

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.i(TAG, "onPrepareOptionsMenu");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        /*switch (id) {
            case R.id.action_login: {
                return true;
            }
        }*/
        return super.onOptionsItemSelected(item);
    }

    // 자신의 백스택에 아무 것도 없을 경우 액티비티 자체를 종료해야 한다.
    private void finish() {
        Log.i(TAG, "finish");
        // getRootFragment().pop();
        getActivity().finish();
    }
}