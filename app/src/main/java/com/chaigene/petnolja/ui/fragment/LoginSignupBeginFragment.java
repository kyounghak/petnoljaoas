package com.chaigene.petnolja.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.firebase.ui.auth.util.data.TaskFailureLogger;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.ConfigManager;
import com.chaigene.petnolja.ui.activity.LoginActivity;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.firebase.ui.auth.util.FirebaseAuthError.ERROR_EMAIL_ALREADY_IN_USE;

public class LoginSignupBeginFragment extends BaseFragment {
    public static final String TAG = "LoginSignupBeginFrag";

    @BindView(R.id.identifier_input)
    EditText mEtIdentifier;

    @BindView(R.id.password_input)
    EditText mEtPassword;

    private boolean isProgressing;

    public static LoginSignupBeginFragment newInstance() {
        LoginSignupBeginFragment fragment = new LoginSignupBeginFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);

        // TODO: 여기서 번들을 읽는다.
        // mUid = bundle.getString(Constants.EXTRA_TARGET_USER_ID);
        // Log.d(TAG, "readBundle:mUid:" + mUid);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_login_signup_begin, container, false);
        ButterKnife.bind(this, mView);

        initView();

        return mView;
    }

    protected void initView() {
        // IME에서 터치한 키보드가 R.id.login일 경우 attemptSignup 메서드를 호출한다.
        mEtPassword.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (isProgressing) return false;

            Log.i(TAG, "onEditorAction:id:" + id);

            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptSignup();
                return true;
            }
            if (id == EditorInfo.IME_ACTION_DONE) {
                attemptSignup();
                return true;
            }
            return false;
        });
    }

    // 유저가 타이핑 할 때마다 호출 되도록 만든다.
    public void checkAccountExists(@NonNull final String email) {
        // mHelper.showLoadingDialog(R.string.progress_dialog_checking_accounts);

        if (!TextUtils.isEmpty(email)) {
            AuthManager.getAuth().fetchProvidersForEmail(email)
                    .addOnFailureListener(new TaskFailureLogger(TAG, "Error fetching providers for email"))
                    .addOnCompleteListener(
                            getActivity(),
                            task -> {
                                // mHelper.dismissDialog();
                            }
                    )
                    .addOnSuccessListener(
                            getActivity(),
                            result -> {
                                List<String> providers = result.getProviders();
                                if (providers == null || providers.isEmpty()) {
                                    // 중복되지 않는 이메일
                                } else if (EmailAuthProvider.PROVIDER_ID.equalsIgnoreCase(providers.get(0))) {
                                    // 중복되는 이메일
                                } else {
                                    // 소셜 로그인으로 존재하는 이메일
                                }
                            }
                    );
        }
    }

    @OnClick(R.id.next_button)
    void attemptSignup() {
        Log.i(TAG, "attemptSignup");

        isProgressing = true;

        CommonUtil.hideKeyboard(getActivity());

        final String email = mEtIdentifier.getText().toString().trim();
        final String password = mEtPassword.getText().toString().trim();

        boolean isCancelled = false;
        View focusView = null;

        if (TextUtils.isEmpty(email)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_email_field_required);
            focusView = mEtIdentifier;
            isCancelled = true;
        }

        if (!isEmailValid(email)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_email);
            focusView = mEtIdentifier;
            isCancelled = true;
        }

        if (TextUtils.isEmpty(password)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_password_field_required);
            focusView = mEtPassword;
            isCancelled = true;
        }

        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_password);
            focusView = mEtPassword;
            isCancelled = true;
        }

        if (isCancelled) {
            focusView.requestFocus();
            isProgressing = false;
        } else {
            showLoadingDialog();

            // 이메일 회원가입을 시도한다.
            // FIXME: 소셜로그인으로 가입된 유저일 경우 어떤 소셜서비스로 가입했는지 보여줘야 한다.
            AuthManager.getAuth().createUserWithEmailAndPassword(email, password).addOnCompleteListener(
                    getActivity(),
                    task -> {
                        dismissDialog();
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            Log.w(TAG, "createUserWithEmailAndPassword:ERROR:" + e);
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                FirebaseAuthException authEx = (FirebaseAuthException) e;
                                String errorCode = authEx.getErrorCode();
                                if (errorCode.equals(ERROR_EMAIL_ALREADY_IN_USE.name())) {
                                    CommonUtil.showSnackbar(getActivity(), R.string.msg_email_already_exists);
                                }
                            }
                            isProgressing = false;
                            return;
                        }
                        Log.d(TAG, "createUserWithEmailAndPassword:SUCCESS");
                        ConfigManager.getInstance(getActivity()).setSavedIdentifier(email);

                        AuthResult authResult = task.getResult();
                        authResult.getUser().sendEmailVerification();

                        // 그 다음 시나리오는 현재 프래그먼트 자체에서 페이지를 끝내지 않고 Activity에게 역할을 위임한다.
                        ((LoginActivity) getActivity()).onSuccessSignIn();

                        isProgressing = false;
                    }
            );
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
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

    @OnClick(R.id.signin_button)
    void finish() {
        getActivity().onBackPressed();
    }
}