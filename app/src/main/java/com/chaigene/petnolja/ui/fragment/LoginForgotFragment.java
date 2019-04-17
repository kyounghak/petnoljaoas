package com.chaigene.petnolja.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.TasksManager;
import com.chaigene.petnolja.model.User;
import com.chaigene.petnolja.ui.activity.LoginActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.PROVIDER_FACEBOOK;
import static com.chaigene.petnolja.Constants.PROVIDER_KAKAO;
import static com.chaigene.petnolja.Constants.PROVIDER_NAVER;
import static com.chaigene.petnolja.ui.activity.LoginActivity.FRAGMENT_TAG_SIGNUP_BEGIN;

// TODO: 로그인을 하지 않은 상태에서 DB에 접근하는 것이 안전한가?
public class LoginForgotFragment extends BaseFragment {
    public static final String TAG = "LoginForgotFragment";

    @BindView(R.id.identifier_input)
    EditText mEtIdentifier;

    private boolean isProgressing;

    public static LoginForgotFragment newInstance() {
        LoginForgotFragment fragment = new LoginForgotFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_login_forgot, container, false);
        ButterKnife.bind(this, mView);

        // IME에서 터치한 키보드가 R.id.login일 경우 attemptLogin 메서드를 호출한다.
        mEtIdentifier.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (isProgressing) return false;

            Log.i(TAG, "onEditorAction:id:" + id);
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                sendPasswordResetLink();
                return true;
            }
            if (id == EditorInfo.IME_ACTION_DONE) {
                sendPasswordResetLink();
                return true;
            }
            return false;
        });

        return mView;
    }

    @OnClick(R.id.send_reset_link_button)
    void sendPasswordResetLink() {
        Log.i(TAG, "sendPasswordResetLink");

        isProgressing = true;

        CommonUtil.hideKeyboard(getActivity());

        final String identifier = mEtIdentifier.getText().toString().trim();

        boolean isCancelled = false;

        if (TextUtils.isEmpty(identifier)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_field_required);
            isCancelled = true;
        }

        if (isCancelled) {
            requestFocus(mEtIdentifier);
            isProgressing = false;
        } else {
            showLoadingDialog();

            final AtomicBoolean isTaskFinished = new AtomicBoolean(false);
            TasksManager.call((Callable<Void>) () -> {
                String email;
                if (isEmail(identifier)) {
                    Task<User> getUserByEmailTask = UserUtil.getUserByEmail(identifier);
                    User user = Tasks.await(getUserByEmailTask);

                    if (!getUserByEmailTask.isSuccessful()) {
                        Exception getUserByEmailError = getUserByEmailTask.getException();
                        Log.w(TAG, "sendPasswordResetLink:getUserByEmailTask:ERROR:", getUserByEmailError);
                        isTaskFinished.set(true);
                        return null;
                    }

                    if (user == null) {
                        dismissDialog();
                        CommonUtil.showSnackbar(getActivity(), "해당 이메일로 가입된 유저가 없습니다.");
                        requestFocus(mEtIdentifier);
                        isProgressing = false;
                        isTaskFinished.set(true);
                        return null;
                    }

                    String provider = user.getProvider();
                    if (provider.equals(PROVIDER_FACEBOOK) || provider.equals(PROVIDER_KAKAO) || provider.equals(PROVIDER_NAVER)) {
                        String providerName = "";
                        if (provider.equals(PROVIDER_FACEBOOK)) providerName = "페이스북";
                        if (provider.equals(PROVIDER_KAKAO)) providerName = "카카오";
                        if (provider.equals(PROVIDER_NAVER)) providerName = "네이버";

                        dismissDialog();
                        CommonUtil.showSnackbar(
                                getActivity(),
                                String.format("%s 소셜 로그인을 통해 가입된 유저입니다. %s(으)로 로그인을 시도해주세요.", providerName, providerName)
                        );
                        isProgressing = false;
                        isTaskFinished.set(true);
                        return null;
                    }

                    email = identifier;
                } else {
                    Task<User> getUserByNicknameTask = UserUtil.getUserByNickname(identifier);
                    User user = Tasks.await(getUserByNicknameTask);

                    if (!getUserByNicknameTask.isSuccessful()) {
                        Exception getUserByNicknameError = getUserByNicknameTask.getException();
                        Log.w(TAG, "sendPasswordResetLink:getUserByNicknameTask:ERROR:", getUserByNicknameError);
                        isTaskFinished.set(true);
                        return null;
                    }

                    if (user == null) {
                        dismissDialog();
                        CommonUtil.showSnackbar(getActivity(), "해당 아이디로 가입된 유저가 없습니다.");
                        requestFocus(mEtIdentifier);
                        isProgressing = false;
                        isTaskFinished.set(true);
                        return null;
                    }

                    String provider = user.getProvider();
                    if (provider.equals(PROVIDER_FACEBOOK) || provider.equals(PROVIDER_KAKAO) || provider.equals(PROVIDER_NAVER)) {
                        String providerName = "";
                        if (provider.equals(PROVIDER_FACEBOOK)) providerName = "페이스북";
                        if (provider.equals(PROVIDER_KAKAO)) providerName = "카카오";
                        if (provider.equals(PROVIDER_NAVER)) providerName = "네이버";

                        dismissDialog();
                        CommonUtil.showSnackbar(
                                getActivity(),
                                String.format("%s 소셜 로그인을 통해 가입된 유저입니다. %s(으)로 로그인을 시도해주세요.", providerName, providerName)
                        );
                        isProgressing = false;
                        isTaskFinished.set(true);
                        return null;
                    }

                    if (user.getEmail() == null) {
                        // 이런 상황은 절대로 존재하지 않는다.
                        dismissDialog();
                        CommonUtil.showSnackbar(getActivity(), "해당 계정에 오류가 있습니다. pandazmaker@gmail.com으로 문의해주세요.");
                        isProgressing = false;
                        isTaskFinished.set(true);
                        return null;
                    }

                    email = user.getEmail();
                }

                Task<Void> sendPasswordResetEmailTask = AuthManager.getAuth().sendPasswordResetEmail(email);
                try {
                    Tasks.await(sendPasswordResetEmailTask);
                } catch (Exception ignored) {
                }

                if (!sendPasswordResetEmailTask.isSuccessful()) {
                    // 알 수 없는 오류
                    dismissDialog();
                    Log.w(TAG, "sendPasswordResetLink:sendPasswordResetEmailTask:ERROR:", sendPasswordResetEmailTask.getException());
                    isProgressing = false;
                    isTaskFinished.set(true);
                    return null;
                }

                dismissDialog();
                CommonUtil.showSnackbar(getActivity(), CommonUtil.format("%s으로 비밀번호 재설정 이메일을 전송하였습니다.", maskEmail(email)));
                isProgressing = false;

                return null;
            }).continueWith((Continuation<Void, Void>) task -> {
                dismissDialog();
                if (isTaskFinished.get()) return null;
                if (!task.isSuccessful()) {
                    Log.w(TAG, "sendPasswordResetLink:ERROR:", task.getException());
                }
                isProgressing = false;
                return null;
            });
        }
    }

    private boolean isEmail(String email) {
        return email.contains("@");
    }

    private String maskEmail(String email) {
        StringBuilder sb = new StringBuilder(email);
        for (int i = 3; i < sb.length() && sb.charAt(i) != '@'; ++i) {
            sb.setCharAt(i, '*');
        }
        String result = sb.toString();
        Log.i(TAG, "maskEmail:source:" + email + "|result:" + result);
        return result;
    }

    /**
     * 해당 EditText에 포커스를 활성화한다.
     *
     * @param editText
     */
    private void requestFocus(final EditText editText) {
        CommonUtil.runOnUiThread(editText::requestFocus);
    }

    @OnClick(R.id.signup_button)
    void showSignupBeginFragment() {
        LoginSignupBeginFragment signupBeginFragment = LoginSignupBeginFragment.newInstance();
        ((LoginActivity) getActivity()).add(signupBeginFragment, FRAGMENT_TAG_SIGNUP_BEGIN);
        CommonUtil.delayCall(() -> {
            ((LoginActivity) getActivity()).remove(LoginForgotFragment.this);
            return null;
        }, 200, true);
    }

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

    void finish() {
        getActivity().onBackPressed();
    }
}