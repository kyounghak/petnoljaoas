package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.chaigene.petnolja.manager.TasksManager;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.iid.FirebaseInstanceId;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.util.exception.KakaoException;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.ui.fragment.BaseFragment;
import com.chaigene.petnolja.ui.dialog.DialogConfirmFragment;
import com.chaigene.petnolja.ui.fragment.LoginFragment;
import com.chaigene.petnolja.ui.fragment.SettingProfileFragment;
import com.chaigene.petnolja.util.OldArticleUtil;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.NotificationUtil;
import com.chaigene.petnolja.util.UserUtil;
import com.nhn.android.naverlogin.OAuthLogin;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.ButterKnife;

import static com.chaigene.petnolja.BuildConfig.KAKAO_AUTH_VALIDATION_ENDPOINT;
import static com.chaigene.petnolja.BuildConfig.NAVER_OAUTH_CLIENT_ID;
import static com.chaigene.petnolja.BuildConfig.NAVER_OAUTH_CLIENT_NAME;
import static com.chaigene.petnolja.BuildConfig.NAVER_OAUTH_CLIENT_SECRET;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_FRAGMENT;
import static com.chaigene.petnolja.Constants.SETTING_PROFILE_FRAGMENT;
import static com.chaigene.petnolja.ui.fragment.SettingProfileFragment.ACTION_STATUS_INSERT;

// Ref: https://developer.android.com/training/basics/fragments/fragment-ui.html?hl=ko#AddAtRuntime
public class LoginActivity extends BaseActivity implements BaseFragment.OnAttatchListener {
    public static final String TAG = "LoginActivity";

    private String mTargetFragment;

    public static final String FRAGMENT_TAG_LOGIN = "login";
    public static final String FRAGMENT_TAG_SIGNUP_BEGIN = "signup_begin";
    public static final String FRAGMENT_TAG_SETTING_PROFILE = "setting_profile";

    private CallbackManager mCallbackManager;
    private ISessionCallback mKakaoSessionCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        // TODO: Search google: savedInstanceState fragment
        if (savedInstanceState != null) {
            return;
        }

        setupAttatchedListener();

        setupFacebook();
        setupKakao();
        setupNaver();

        // 최초로 보여질 프래그먼트
        if (mTargetFragment == null) {
            LoginFragment loginFragment = LoginFragment.newInstance();
            add(loginFragment, FRAGMENT_TAG_LOGIN);
        } else if (mTargetFragment.equals(FRAGMENT_TAG_SETTING_PROFILE)) {
            SettingProfileFragment settingProfileFragment = SettingProfileFragment.newInstance(ACTION_STATUS_INSERT);
            add(settingProfileFragment, FRAGMENT_TAG_SETTING_PROFILE);
        }
    }

    @Override
    protected void readIntent() {
        super.readIntent();
        mTargetFragment = getIntent().getStringExtra(EXTRA_TARGET_FRAGMENT);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull String tag) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, LoginActivity.class);
        intent.putExtra(EXTRA_TARGET_FRAGMENT, tag);
        return intent;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 콜백을 삭제해주지 않으면 중복 호출된다.
        if (mKakaoSessionCallback != null)
            Session.getCurrentSession().removeCallback(mKakaoSessionCallback);
    }

    private void setupAttatchedListener() {
        // find()
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
    }

    public Fragment find(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        Log.i(TAG, "find:tag:" + tag + "/result:" + fragment);
        return fragment;
    }

    @Deprecated
    public void add(BaseFragment fragment) {

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(R.animator.fade_in)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public void add(Fragment fragment, String tag) {
        Log.i(TAG, "add:" + fragment + " " + tag);

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        // 이 시점에 리스너를 붙혀준다.
        // 하지만 리스너들이 중첩될 수 있는가가 의문.
        BaseFragment baseFragment = ((BaseFragment) fragment);
        baseFragment.setOnAttatchListener(this);

        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(R.animator.fade_in)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commit();
    }

    public void remove(Fragment fragment) {
        Log.i(TAG, "remove:" + fragment);

        if (!fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:false");
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .remove(fragment)
                .commit();
    }

    public void pop() {
        Log.i(TAG, "pop");
        getSupportFragmentManager().popBackStack();
    }

    private void setupFacebook() {
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                showLoadingDialog();
                AuthCredential credential = FacebookAuthProvider.getCredential(loginResult.getAccessToken().getToken());
                AuthManager.getAuth().signInWithCredential(credential).continueWith(new Continuation<AuthResult, Void>() {
                    @Override
                    public Void then(@NonNull Task<AuthResult> task) throws Exception {
                        dismissDialog();
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential:ERROR", task.getException());
                            Exception e = task.getException();

                            // FirebaseAuthUserCollisionException:
                            // An account already exists with the same email address but different sign-in credentials.
                            // Sign in using a provider associated with this email address.
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                // 동일한 이메일의 다른 방식(이메일, 카카오)
                                CommonUtil.showSnackbar(LoginActivity.this, "페이스북 계정에 사용된 이메일로 이미 가입하셨습니다. 이메일과 비밀번호로 다시 시도해주세요.");
                                return null;
                            }

                            CommonUtil.showSnackbar(LoginActivity.this, R.string.msg_failed_facebook_sign_in);
                            return null;
                        }
                        Log.d(TAG, "signInWithCredential:SUCCESS");
                        onSuccessSignIn();
                        return null;
                    }
                });
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Facebook login canceled.");
            }

            @Override
            public void onError(FacebookException error) {
                Log.w(TAG, "Facebook Login Error", error);
            }
        });
    }

    private void setupKakao() {
        mKakaoSessionCallback = new ISessionCallback() {
            final String TAG = "ISessionCallback";

            @Override
            public void onSessionOpened() {
                Log.i(TAG, "onSessionOpened");

                final AtomicBoolean isTaskFinished = new AtomicBoolean(false);

                showLoadingDialog();
                String accessToken = Session.getCurrentSession().getAccessToken();
                getFirebaseJwt(accessToken).continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        dismissDialog();
                        Exception e = task.getException();
                        if (e instanceof TimeoutError) {
                            Log.w(TAG, "getFirebaseJwt:ERROR:TimeoutError");
                        }
                        CommonUtil.showSnackbar(LoginActivity.this, R.string.msg_failed_kakao_sign_in);
                        Log.w(TAG, "getFirebaseJwt:ERROR", e);
                        kakaoSignOut();
                        isTaskFinished.set(true);
                    }
                    String firebaseToken = task.getResult();
                    return AuthManager.getAuth().signInWithCustomToken(firebaseToken);
                }).addOnCompleteListener(task -> {
                    if (isTaskFinished.get()) return;
                    // dismissDialog();
                    if (!task.isSuccessful()) {
                        dismissDialog();
                        CommonUtil.showSnackbar(LoginActivity.this, R.string.msg_failed_kakao_sign_in);
                        Log.w(TAG, "signInWithCustomToken:ERROR", task.getException());
                        kakaoSignOut();
                        return;
                    }
                    Log.d(TAG, "signInWithCustomToken:SUCCESS");
                    onSuccessSignIn();
                });
            }

            @Override
            public void onSessionOpenFailed(KakaoException exception) {
                if (exception != null) {
                    Log.w(TAG, "onSessionOpenFailed", exception);
                    CommonUtil.showSnackbar(LoginActivity.this, R.string.msg_failed_kakao_sign_in);
                }
            }

            void kakaoSignOut() {
                UserManagement.requestLogout(new LogoutResponseCallback() {
                    @Override
                    public void onCompleteLogout() {
                        Log.d(TAG, "signOut:kakao_signout_complete");
                        Session.getCurrentSession().close();
                    }
                });
            }
        };
        Session.getCurrentSession().addCallback(mKakaoSessionCallback);
    }

    /**
     * 카카오 SDK로부터 받은 Access token을 가지고 파이어베이스 서버에 전송하여 파이어베이스 토큰을 반환받는다.
     * JWT (Ref: https://velopert.com/2389)
     *
     * @param kakaoAccessToken Access token retrieved after successful Kakao Login
     * @return Task object that will call validation server and retrieve firebase token
     */
    private Task<String> getFirebaseJwt(final String kakaoAccessToken) {
        Log.i(TAG, "getFirebaseJwt:kakaoAccessToken:" + kakaoAccessToken);

        final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        HashMap<String, String> validationObject = new HashMap<>();
        validationObject.put("token", kakaoAccessToken);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                KAKAO_AUTH_VALIDATION_ENDPOINT,
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
                params.put("token", kakaoAccessToken);
                return params;
            }
        };

        // Volley의 타임아웃을 5초로 지정한다.
        request.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(request);
        return tcs.getTask();
    }

    private void setupNaver() {
        OAuthLogin oAuthLoginModule = OAuthLogin.getInstance();
        oAuthLoginModule.init(
                this,
                NAVER_OAUTH_CLIENT_ID,
                NAVER_OAUTH_CLIENT_SECRET,
                NAVER_OAUTH_CLIENT_NAME
        );
    }

    // I don't think it's good idea but we have to handle result code at activity scope bcuz of kakao sdk.
    // TODO: LoginFragment에서 email로 로그인 했을 때의 대한 Result code를 여기서 핸들링 해야 한다.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));

        // Kakao SDK
        Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onFragmentResult:requestCode:%d|resultCode:%d", requestCode, resultCode));

        if (requestCode == SETTING_PROFILE_FRAGMENT) {
            if (resultCode == RESULT_OK) {
                showLoadingDialog();
                TasksManager.call(() -> {
                    String token = FirebaseInstanceId.getInstance().getToken();
                    Task<Boolean> isTokenExistsTask = NotificationUtil.isStableTokenExists(token);
                    boolean isTokenExists = Tasks.await(isTokenExistsTask);
                    if (!isTokenExists) {
                        // 현재 디바이스에 대한 토큰값이 서버에 존재하지 않으면 삽입해준다.
                        Task<Void> insertTokenTask = NotificationUtil.insertStableToken(token);
                        Tasks.await(insertTokenTask);
                    }
                    dismissDialog();
                    startMainActivity();
                    return null;
                }).continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "onFragmentResult:ERROR:" + task.getException().getMessage());
                        return null;
                    }
                    return null;
                });
            }
        }
    }

    // TODO: 로그인을 했지만 /user/users/$uid 값이 존재하지 않는다면 프로필 정보 입력 페이지를 보여준다.
    public void onSuccessSignIn() {
        Log.i(TAG, "onSuccessSignIn");

        showLoadingDialog();

        // 로그인에 성공하였다면 혹시 캐쉬에 저장되었을지도 모르는 유틸들을 초기화시켜 주어야 한다.
        UserUtil.releaseInstance();
        OldArticleUtil.releaseInstance();

        final AtomicBoolean isTaskFinished = new AtomicBoolean(false);

        TasksManager.call((Callable<Void>) () -> {
            String token = FirebaseInstanceId.getInstance().getToken();
            Task<Boolean> isStableTokenExistsTask = NotificationUtil.isStableTokenExists(token);
            Task<Boolean> isUserExistsTask = UserUtil.isExists(AuthManager.getUserId());
            Task<Void> tasks = Tasks.whenAll(isStableTokenExistsTask, isUserExistsTask);

            Tasks.await(tasks);

            boolean isUserExists = isUserExistsTask.getResult();
            if (!isUserExists) {
                // Ref: https://stackoverflow.com/a/14294323/4729203
                dismissDialog();
                Log.d(TAG, "onSuccessSignIn:isUserExists:false:show_insert_user_info_page");

                SettingProfileFragment settingProfileFragment = SettingProfileFragment.newInstance(ACTION_STATUS_INSERT);
                add(settingProfileFragment, FRAGMENT_TAG_SETTING_PROFILE);

                CommonUtil.delayCall(() -> {
                    Fragment signupBeginFragment = find(FRAGMENT_TAG_SIGNUP_BEGIN);
                    if (signupBeginFragment != null) remove(signupBeginFragment);
                    return null;
                }, 200, true);

                isTaskFinished.set(true);
                return null;
            }

            boolean isStableTokenExists = isStableTokenExistsTask.getResult();
            if (isStableTokenExists) {
                return null;
            }

            // 현재 디바이스에 대한 토큰값이 서버에 존재하지 않으면 삽입해준다.
            Task<Void> insertStableTokenTask = NotificationUtil.insertStableToken(token);
            Tasks.await(insertStableTokenTask);
            return null;
        }).continueWith((Continuation<Void, Void>) task -> {
            if (isTaskFinished.get()) return null;
            dismissDialog();
            if (!task.isSuccessful()) {
                Log.w(TAG, "onSuccessSignIn:ERROR:", task.getException());
                CommonUtil.showSnackbar(this, "일시적인 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.");
                return null;
            }
            startMainActivity();
            return null;
        });
    }

    // TODO: MainActivity를 시작하기 전에 아이디와 비밀번호를 설정했는지 먼저 확인해야 한다.
    // 카카오나 페이스북으로 로그인 했을 경우 비밀번호를 입력하는 부분은 필요가 없다.
    private void startMainActivity() {
        Log.i(TAG, "startMainActivity");
        Intent in = createIntent(OldMainActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(in);
        finish();
    }

    // TODO: RootFragment를 사용하지 않아도 될 것 같다.
    // 그냥 프래그먼트를 삽입할 수 있는 콘테이너를 레이아웃에 설정하고 getSupportFragment를 사용하여 대체해주기로 한다.
    // 그리고 툴바 영역은 아무런 네비게이션 메뉴나 기타 메뉴가 존재하지 않는다.
    @Override
    public void onBackPressed() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        int backStackCount = fragmentManager.getBackStackEntryCount();
        Log.i(TAG, "onBackPressed:backStackCount:" + backStackCount);

        // 프로필 설정 화면일 때는 뒤로 갈 수가 없다.
        final Fragment settingProfileFragment = find(FRAGMENT_TAG_SETTING_PROFILE);
        if (settingProfileFragment != null && settingProfileFragment.isVisible()) {
            // super.onBackPressed();

            final DialogConfirmFragment dialogFragment = DialogConfirmFragment.newInstance(
                    null,
                    "이전 화면으로 이동하시면 \n현재 계정에서 로그아웃 됩니다. \n로그아웃 하시겠습니까?",
                    new DialogConfirmFragment.OnSelectListener() {
                        @Override
                        public void onConfirm() {
                            super.onConfirm();
                            AuthManager.signOut(getApplicationContext());

                            // TODO: 특정 상황에서 로그아웃을 하면 반드시 익명 로그인을 다시 시켜주어야 한다.
                            // 가장 좋은 것은 signOut 메서드 내에 익명 로그인을 포함하는 것이나
                            // 이렇게 되면 signOut 자체가 비동기 메서드가 되버린다.

                            LoginFragment loginFragment = (LoginFragment) find(FRAGMENT_TAG_LOGIN);
                            if (loginFragment != null) {
                                fragmentManager.popBackStack();
                            } else {
                                loginFragment = LoginFragment.newInstance();
                                add(loginFragment, FRAGMENT_TAG_LOGIN);

                                CommonUtil.delayCall(() -> {
                                    remove(settingProfileFragment);
                                    return null;
                                }, 200, true);
                            }
                        }

                        @Override
                        public void onDeny() {
                            super.onDeny();
                        }
                    });
            dialogFragment.show(fragmentManager, null);
            return;
        }

        // 최초의 BaseFragment는 남겨두고 모두 제거한다.
        if (backStackCount > 1) {
            fragmentManager.popBackStack();
        } else super.onBackPressed();
    }

    @Override
    public void showLoadingDialog() {
        super.showLoadingDialog();
        // 에디트텍스트에 포커스가 그대로 남아있는 이슈 반영.
        LoginFragment loginFragment = (LoginFragment) find(FRAGMENT_TAG_LOGIN);
        if (loginFragment != null) loginFragment.disableFocus();
    }

    @Override
    public void dismissDialog() {
        super.dismissDialog();
        // 에디트텍스트에 포커스가 그대로 남아있는 이슈 반영.
        LoginFragment loginFragment = (LoginFragment) find(FRAGMENT_TAG_LOGIN);
        if (loginFragment != null) loginFragment.restoreFocus();
    }

    @Override
    public void onAttatch(String tag) {
        Log.i(TAG, "onAttatch");
        if (tag.equals(FRAGMENT_TAG_SETTING_PROFILE)) {
            // 가입을 완료했기 때문에 SettingProfileFragment을 보여주고 뒤로 돌아갈 수는 없다.
            // 따라서 이미 추가되어 있는 LoginSignupBeginFragment를 삭제해준다.
            // SettingProfileFragment에서는 뒤로가기를 눌렀을 때 로그아웃이 되고 최초 화면으로 갈 수는 있다.
            // Fragment signupBeginFragment = find(FRAGMENT_TAG_SIGNUP_BEGIN);
            // if (signupBeginFragment != null) remove(signupBeginFragment);
        }
    }

}