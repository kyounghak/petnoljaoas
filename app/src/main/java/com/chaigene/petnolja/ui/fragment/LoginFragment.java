package com.chaigene.petnolja.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.model.User;
import com.facebook.login.LoginManager;
import com.firebase.ui.auth.util.FirebaseAuthError;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.ConfigManager;
import com.chaigene.petnolja.ui.activity.LoginActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.BuildConfig.NAVER_AUTH_VALIDATION_ENDPOINT;
import static com.chaigene.petnolja.Constants.PROVIDER_NAVER;
import static com.google.android.gms.tasks.Tasks.await;
import static com.chaigene.petnolja.Constants.PROVIDER_FACEBOOK;
import static com.chaigene.petnolja.Constants.PROVIDER_KAKAO;
import static com.chaigene.petnolja.ui.activity.LoginActivity.FRAGMENT_TAG_SIGNUP_BEGIN;

public class LoginFragment extends BaseFragment implements FirebaseAuth.AuthStateListener {
    public static final String TAG = "LoginFragment";

    @BindView(R.id.container)
    ViewGroup mVgContainer;

    @BindView(R.id.identifier_input)
    EditText mEtIdentifier;

    @BindView(R.id.password_input)
    EditText mEtPassword;

    private FirebaseAuth mAuth;
    private boolean isProgressing;

    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_login, container, false);
        ButterKnife.bind(this, mView);

        mAuth = FirebaseAuth.getInstance();

        initView();

        return mView;
    }

    protected void initView() {
        // 로그인 성공했던 기록이 있는 아이디나 이메일을 ConfigManager에 저장해두고 삽입해준다.
        String savedEmail = ConfigManager.getInstance(getActivity()).getSavedIdentifier();
        if (savedEmail != null) {
            mEtIdentifier.setText(savedEmail);
            mEtIdentifier.setSelection(savedEmail.length());
        }

        // IME에서 터치한 키보드가 R.id.login일 경우 attemptLogin 메서드를 호출한다.
        mEtPassword.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (isProgressing) return false;

            Log.i(TAG, "onEditorAction:id:" + id);

            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            if (id == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(this);
    }

    @OnClick(R.id.email_login_button)
    void attemptLogin() {
        Log.i(TAG, "attemptLogin");

        isProgressing = true;

        CommonUtil.hideKeyboard(getActivity());

        final String identifier = mEtIdentifier.getText().toString().trim();
        final String password = mEtPassword.getText().toString().trim();

        boolean cancel = false;
        EditText focusView = null;

        // Valication
        if (TextUtils.isEmpty(identifier)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_identifier_field_required);
            focusView = mEtIdentifier;
            cancel = true;
        } else if (TextUtils.isEmpty(password)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_password_field_required);
            focusView = mEtPassword;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_password);
            focusView = mEtPassword;
            cancel = true;
        }

        if (cancel) {
            requestFocus(focusView);
            isProgressing = false;
        } else {
            showLoadingDialog();

            final AtomicBoolean isTaskFinished = new AtomicBoolean(false);
            Tasks.call(FirestoreManager.getInstance().getExecutor(), (Callable<Void>) () -> {
                String email;
                if (isEmail(identifier)) {
                    Task<User> getUserByEmailTask = UserUtil.getUserByEmail(identifier);
                    User user = await(getUserByEmailTask);

                    if (!getUserByEmailTask.isSuccessful()) {
                        Exception getUserByEmailError = getUserByEmailTask.getException();
                        Log.w(TAG, "attemptLogin:getUserByEmailTask:ERROR:", getUserByEmailError);
                        isTaskFinished.set(true);
                        return null;
                    }

                    // FIXME: 이메일로 가입하고 정보를 입력하기 전이거나 소셜 로그인으로 가입하고 정보를 입력하기 전에는
                    // user가 null로 출력된다.
                    // TODO: 가입이 성공하면 무조건 user를 생성한다.
                    // 대신에 email, provider에 대한 값만 삽입한다.
                    // 그러면 여기서는 추가 작업을 할 필요가 없다.
                    // 대신에 로그인에 성공했을 때는 nickname 여부를 체크하고 존재하지 않으면
                    // 정보입력화면을 띄워줘야 한다.
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
                                String.format("%s 소셜 로그인을 통해 가입된 유저입니다. %s으로 로그인을 시도해주세요.", providerName, providerName)
                        );
                        isProgressing = false;
                        isTaskFinished.set(true);
                        return null;
                    }

                    email = identifier;
                } else {
                    Task<User> getUserByNicknameTask = UserUtil.getUserByNickname(identifier);
                    User user = await(getUserByNicknameTask);

                    /*if (!getUserByNicknameTask.isSuccessful()) {
                        Exception getUserByNicknameError = getUserByNicknameTask.getException();
                        Log.w(TAG, "attemptLogin:getUserByNicknameTask:ERROR:", getUserByNicknameError);
                        isTaskFinished.set(true);
                        return null;
                    }*/

                    if (user == null) {
                        dismissDialog();
                        CommonUtil.showSnackbar(getActivity(), "해당 아이디로 가입된 유저가 없습니다.");
                        requestFocus(mEtIdentifier);
                        isProgressing = false;
                        isTaskFinished.set(true);
                        return null;
                    }

                    String provider = user.getProvider();
                    if (provider.equals(PROVIDER_FACEBOOK)) {
                        dismissDialog();
                        CommonUtil.showSnackbar(getActivity(), "페이스북 소셜 로그인을 통해 가입된 유저입니다. 페이스북으로 로그인을 시도해주세요.");
                        isProgressing = false;
                        isTaskFinished.set(true);
                        return null;
                    }

                    if (provider.equals(PROVIDER_KAKAO)) {
                        dismissDialog();
                        CommonUtil.showSnackbar(getActivity(), "카카오톡 소셜 로그인을 통해 가입된 유저입니다. 카카오톡으로 로그인을 시도해주세요.");
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

                Task<AuthResult> signInTask = signIn(email, password);
                AuthResult authResult = Tasks.await(signInTask);

                /*if (!signInTask.isSuccessful()) {
                    // 알 수 없는 오류
                    dismissDialog();
                    Log.w(TAG, "attemptLogin:signInTask:ERROR:", signInTask.getException());
                    CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생하였습니다. 잠시후 다시 시도해주세요. 문제가 지속되면 pandazmaker@gmail.com으로 문의해주세요.");
                    isProgressing = false;
                    isTaskFinished.set(true);
                    return null;
                }*/

                ConfigManager.getInstance(getActivity()).setSavedIdentifier(identifier);
                return null;
            }).continueWith((Continuation<Void, Void>) task -> {
                dismissDialog();
                if (isTaskFinished.get()) return null;
                if (!task.isSuccessful()) {
                    Log.w(TAG, "attemptLogin:signIn:ERROR:", task.getException());
                    isProgressing = false;
                    return null;
                }
                Log.d(TAG, "attemptLogin:signIn:SUCCESS");
                ((LoginActivity) getActivity()).onSuccessSignIn();
                isProgressing = false;
                return null;
            });
        }
    }

    private boolean isEmail(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    // TODO: AuthManager에 집어넣어야 함.
    @SuppressLint("RestrictedApi")
    private Task<AuthResult> signIn(String email, String password) {
        final TaskCompletionSource<AuthResult> tcs = new TaskCompletionSource<>();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(getActivity(), task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        Log.w(TAG, "signIn:ERROR", e);
                        tcs.setException(e);
                        if (e instanceof FirebaseNetworkException) {
                            CommonUtil.showSnackbar(getActivity(), R.string.msg_no_network);
                            return;
                        }
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            CommonUtil.showSnackbar(getActivity(), R.string.msg_wrong_password);
                            return;
                        }
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            FirebaseAuthInvalidUserException authEx = (FirebaseAuthInvalidUserException) e;
                            switch (FirebaseAuthError.fromException(authEx)) {
                                case ERROR_USER_DISABLED:
                                    CommonUtil.showSnackbar(
                                            getActivity(),
                                            "탈퇴했거나 이용이 정지된 사용자입니다. 문의사항은 pandazmaker@gmail.com으로 문의해주세요."
                                    );
                                    return;
                                case ERROR_USER_NOT_FOUND:
                                    CommonUtil.showSnackbar(getActivity(), R.string.msg_email_not_exists);
                                    return;
                                default:
                                    CommonUtil.showSnackbar(getActivity(), "알 수 없는 오류가 발생했습니다. pandazmaker@gmail.com으로 문의해주세요.");
                                    return;
                            }
                        }
                        return;
                    }
                    Log.d(TAG, "signIn:SUCCESS");

                    // TODO: LoginActivity에 Result code를 반환해야 한다.
                    // LoginActivity에서는 해당 코드를 바탕으로 MainActivity를 시작한다.
                    tcs.setResult(task.getResult());
                }
        );
        return tcs.getTask();
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // OldUser is signed in
            Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
        } else {
            // OldUser is signed out
            Log.d(TAG, "onAuthStateChanged:signed_out");
        }
    }

    @OnClick(R.id.facebook_login_button)
    void performFacebookLogIn() {
        mEtIdentifier.clearFocus();
        mEtPassword.clearFocus();
        LoginManager.getInstance().logInWithReadPermissions(getActivity(), Arrays.asList("email", "public_profile", "user_friends"));
    }

    // TODO: 코드가 너무 복잡하므로 다른 클래스로 이동.
    @OnClick(R.id.naver_login_button)
    void performNaverLogIn(View btnNaverLogin) {
        Log.i(TAG, "performNaverLogIn");
        mEtIdentifier.clearFocus();
        mEtPassword.clearFocus();
        OAuthLogin.getInstance().startOauthLoginActivity(
                getActivity(),
                new NaverLoginHandler(getContext(), new NaverLoginHandler.OnCompleteListener() {
                    @Override
                    public void onSuccess(String accessToken, String refreshToken, long expiresAt, String tokenType, String state) {
                        final AtomicBoolean isTaskFinished = new AtomicBoolean(false);
                        showLoadingDialog();
                        getFirebaseJwt(accessToken).continueWithTask(task -> {
                            if (!task.isSuccessful()) {
                                dismissDialog();
                                Exception e = task.getException();
                                if (e instanceof TimeoutError) {
                                    Log.w(TAG, "getFirebaseJwt:ERROR:TimeoutError");
                                }
                                CommonUtil.showSnackbar(getActivity(), /* FIXME */R.string.msg_failed_kakao_sign_in);
                                Log.w(TAG, "getFirebaseJwt:ERROR", e);
                                naverSignOut();
                                isTaskFinished.set(true);
                            }
                            String firebaseToken = task.getResult();
                            return AuthManager.getAuth().signInWithCustomToken(firebaseToken);
                        }).addOnCompleteListener(task -> {
                            if (isTaskFinished.get()) return;
                            if (!task.isSuccessful()) {
                                dismissDialog();
                                CommonUtil.showSnackbar(getActivity(), R.string.msg_failed_kakao_sign_in);
                                Log.w(TAG, "signInWithCustomToken:ERROR", task.getException());
                                naverSignOut();
                                return;
                            }
                            Log.d(TAG, "signInWithCustomToken:SUCCESS");
                            ((LoginActivity) getActivity()).onSuccessSignIn();
                        });
                    }

                    @Override
                    public void onFail(String errorCode, String errorDesc) {
                        Log.w(TAG, "onFail:errorCode:" + errorCode + "|errorDesc:" + errorDesc);
                        CommonUtil.showSnackbar(getActivity(), /* FIXME */R.string.msg_failed_kakao_sign_in);
                    }

                    void naverSignOut() {
                        OAuthLogin oAuthLogin = OAuthLogin.getInstance();
                        boolean isSuccessDeleteToken = oAuthLogin.logoutAndDeleteToken(getContext());
                        if (!isSuccessDeleteToken) {
                            // 서버에서 토큰 삭제에 실패했어도 클라이언트에 있는 토큰은 삭제되어 로그아웃된 상태입니다.
                            // 클라이언트에 토큰 정보가 없기 때문에 추가로 처리할 수 있는 작업은 없습니다.
                            Log.d(TAG, "signOut:naver:errorCode:" + oAuthLogin.getLastErrorCode(getContext()));
                            Log.d(TAG, "signOut:naver:errorDesc:" + oAuthLogin.getLastErrorDesc(getContext()));
                        }
                    }
                })
        );
    }

    // TODO: 코드가 너무 복잡하므로 별로 클래스 파일로 분리.
    // Ref: https://stackoverflow.com/q/11407943/4729203
    private static class NaverLoginHandler extends OAuthLoginHandler {
        private final String TAG = "NaverLoginHandler";
        private Context mContext;
        private OnCompleteListener mListener;

        private NaverLoginHandler(Context context, OnCompleteListener listener) {
            this.mContext = context;
            this.mListener = listener;
        }

        @Override
        public void run(boolean success) {
            OAuthLogin oAuthLoginModule = OAuthLogin.getInstance();
            if (success) {
                String accessToken = oAuthLoginModule.getAccessToken(mContext);
                String refreshToken = oAuthLoginModule.getRefreshToken(mContext);
                long expiresAt = oAuthLoginModule.getExpiresAt(mContext);
                String tokenType = oAuthLoginModule.getTokenType(mContext);
                String state = oAuthLoginModule.getState(mContext).toString();
                Log.i(TAG, "login:SUCCESS:accessToken:" + accessToken);
                Log.i(TAG, "login:SUCCESS:refreshToken:" + refreshToken);
                Log.i(TAG, "login:SUCCESS:expiresAt:" + expiresAt);
                Log.i(TAG, "login:SUCCESS:tokenType:" + tokenType);
                Log.i(TAG, "login:SUCCESS:state:" + state);
                mListener.onSuccess(accessToken, refreshToken, expiresAt, tokenType, state);
            } else {
                String errorCode = oAuthLoginModule.getLastErrorCode(mContext).getCode();
                String errorDesc = oAuthLoginModule.getLastErrorDesc(mContext);
                Toast.makeText(mContext, "errorCode:" + errorCode + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
                mListener.onFail(errorCode, errorDesc);
            }
        }

        private interface OnCompleteListener {
            void onSuccess(String accessToken, String refreshToken, long expiresAt, String tokenType, String state);

            void onFail(String errorCode, String errorDesc);
        }
    }

    // TODO: 코드가 너무 복잡하므로 다른 클래스로 이동.
    private Task<String> getFirebaseJwt(final String naverAccessToken) {
        Log.i(TAG, "getFirebaseJwt:naverAccessToken:" + naverAccessToken);
        final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
        RequestQueue queue = Volley.newRequestQueue(getContext().getApplicationContext());
        HashMap<String, String> validationObject = new HashMap<>();
        validationObject.put("token", naverAccessToken);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                NAVER_AUTH_VALIDATION_ENDPOINT,
                new JSONObject(validationObject),
                response -> {
                    try {
                        String firebaseToken = response.getString("firebase_token");
                        tcs.setResult(firebaseToken);
                    } catch (Exception e) {
                        tcs.setException(e);
                    }
                }, error -> {
            Log.e(TAG, error.toString());
            tcs.setException(error);
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("token", naverAccessToken);
                return params;
            }
        };

        // Volley의 타임아웃을 5초로 지정한다.
        request.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(request);
        return tcs.getTask();
    }

    @OnClick(R.id.forgot_account)
    void showForgotAccount() {
        LoginForgotFragment forgotAccountFragment = LoginForgotFragment.newInstance();
        ((LoginActivity) getActivity()).add(forgotAccountFragment);
    }

    @OnClick(R.id.signup_button)
    void showSignupBeginFragment() {
        LoginSignupBeginFragment signupBeginFragment = LoginSignupBeginFragment.newInstance();
        ((LoginActivity) getActivity()).add(signupBeginFragment, FRAGMENT_TAG_SIGNUP_BEGIN);
    }

    private void requestFocus(final EditText editText) {
        CommonUtil.runOnUiThread(() -> editText.requestFocus());
    }

    public void disableFocus() {
        Log.i(TAG, "disableFocus");
        CommonUtil.runOnUiThread(() -> {
            mEtIdentifier.clearFocus();
            mEtPassword.clearFocus();
            mEtIdentifier.setFocusable(false);
            mEtPassword.setFocusable(false);
            CommonUtil.hideKeyboard(getActivity());
        });
    }

    public void restoreFocus() {
        Log.i(TAG, "restoreFocus");
        CommonUtil.runOnUiThread(() -> {
            mEtIdentifier.setFocusable(true);
            mEtPassword.setFocusable(true);
            mEtIdentifier.setFocusableInTouchMode(true);
            mEtPassword.setFocusableInTouchMode(true);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Log.i(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);

        // LoginFragment만 메뉴가 존재하지 않기 때문에 강제로 모두 초기화 해준다.
        getToolbar().setNavigationIcon(null);
        getToolbar().setNavigationOnClickListener(null);
        getToolbar().getMenu().clear();
    }
}