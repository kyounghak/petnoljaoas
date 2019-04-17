package com.chaigene.petnolja.ui.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.messaging.RemoteMessage;
import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.util.CommonUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.EXTRA_REMOTE_MESSAGE;
import static com.chaigene.petnolja.util.CommonUtil.dpToPx;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";

    @Nullable
    @BindView(R.id.appbar)
    AppBarLayout mAppBar;

    @Nullable
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Nullable
    @BindView(R.id.toolbar_icon)
    ImageView mIvToolbarIcon;

    @Nullable
    @BindView(R.id.toolbar_title)
    TextView mTvToolbarTitle;

    @Nullable
    @BindView(R.id.profile_container)
    ViewGroup mVgProfileContainer;

    ViewGroup mVGLoading;

    private CoreServiceStateReceiver mCoreServiceStateReceiver;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readIntent();
    }

    protected void readIntent() {
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
        setupToolbar();
        setupAppBar();
    }

    protected void initView() {
    }

    protected void setupAppBar() {
        lockAppBar(true);
    }

    // 이 메서드는 언제든지 오버라이드 받아서 다른 코드를 사용할 수 있다.
    protected void setupToolbar() {
        Log.i(TAG, "setupToolbar:toolbar:" + mToolbar);
        if (mToolbar != null) {
            getToolbar().setTitleTextColor(CommonUtil.getColor(this, android.R.color.black));
            //toolbar.setNavigationIcon(R.drawable.ic_menu_white);
            setSupportActionBar(mToolbar);
            setToolbarTitle(null);
            showToolbarIcon();
        }

        // Source: http://stackoverflow.com/a/27703150/4729203
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(getString(R.string.app_name), bm, CommonUtil.getColor(this, R.color.material_grey_200));
            setTaskDescription(taskDesc);
        }
    }

    public void resetToolbar() {
        if (getToolbar() == null) return;
        setToolbarTitle(null);
        setToolbarTitleAlign(Gravity.CENTER_HORIZONTAL);
        showToolbarIcon();
        getToolbar().setBackgroundResource(R.drawable.background_toolbar_border_bottom);
    }

    public Toolbar getToolbar() {
        // Log.i(TAG, "getToolbar");
        return mToolbar;
    }

    public void setToolbarTitle(@StringRes int titleResId) {
        setToolbarTitle(getString(titleResId));
    }

    @SuppressWarnings("ConstantConditions")
    public void setToolbarTitle(String title) {
        Log.i(TAG, "setToolbarTitle:title:" + title);
        if (getToolbarTitleView() == null) {
            Log.i(TAG, "setToolbarTitle:getToolbarTitleView:null");
            return;
        }
        if (title == null) title = "";
        // setDisplayShowTitleEnabled가 중복 호출되면 타이틀이 사라지기 때문에 적용
        // setSupportActionBar(mToolbar);
        if (mIvToolbarIcon != null) mIvToolbarIcon.setVisibility(View.GONE);
        getToolbarTitleView().setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(title);
        getToolbarTitleView().setText(title);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    public void setToolbarTitleAlign(int horizontalGravity) {
        // Log.i(TAG, "setToolbarTitleAlign:horizontalGravity:" + horizontalGravity);
        if (getToolbarTitleView() == null) {
            // Log.i(TAG, "setToolbarTitle:getToolbarTitleView:null");
            return;
        }
        Toolbar.LayoutParams params = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.MATCH_PARENT);
        params.gravity = horizontalGravity | Gravity.CENTER_VERTICAL;
        getToolbarTitleView().setLayoutParams(params);
    }

    public void showToolbarIcon() {
        // Log.i(TAG, "showToolbarIcon");
        if (getToolbarIconView() == null) {
            Log.i(TAG, "showToolbarIcon:getToolbarIconView:null");
            return;
        }
        if (mTvToolbarTitle != null) mTvToolbarTitle.setVisibility(View.GONE);
        mIvToolbarIcon.setVisibility(View.VISIBLE);
    }

    public void setSystemBar(boolean coloring) {
        // for system bar in lollipop
        if (coloring) CommonUtil.systemBarLolipop(this);
    }

    public ImageView getToolbarIconView() {
        return mIvToolbarIcon;
    }

    public TextView getToolbarTitleView() {
        return mTvToolbarTitle;
    }

    public AppBarLayout getAppBar() {
        // Log.i(TAG, "getToolbar");
        return mAppBar;
    }

    public void lockAppBar(boolean locked) {
        if (locked) {
            // getAppBar().setExpanded(false, true);
            // getAppBar().setActivated(false);
            // int collapsedHeight = CommonUtil.dpToPx(this, 40);
            // CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) getAppBar().getLayoutParams();
            // lp.height = collapsedHeight;
            // getAppBar().setLayoutParams(lp);
            // collapsingToolbarLayout.setTitleEnabled(false);
            // toolbar.setTitle(title);
        } else {
            // getAppBar().setExpanded(true, true);
            // getAppBar().setActivated(true);
            // CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) getAppBar().getLayoutParams();
            // int expandedHeight = CommonUtil.dpToPx(this, 200);
            // lp.height = expandedHeight;
            // getAppBar().setLayoutParams(lp);
            // collapsingToolbarLayout.setTitleEnabled(true);
            // collapsingToolbarLayout.setTitle(title);
        }
    }

    /*public void setProfileViewEnabled(final boolean enabled) {
        final ViewGroup targetView = mVgProfileContainer;

        int start, end;
        if (enabled) {
            start = 0;
            end = 180;
        } else {
            start = 180;
            end = 0;
        }

        int startPx = CommonUtil.dpToPx(this, start);
        int endPx = CommonUtil.dpToPx(this, end);
        ValueAnimator animator = ValueAnimator.ofInt(startPx, endPx);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int animatedValue = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = targetView.getLayoutParams();
                layoutParams.height = animatedValue;
                targetView.setLayoutParams(layoutParams);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (enabled) targetView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!enabled) targetView.setVisibility(View.GONE);
            }
        });
        animator.setDuration(1000);
        animator.start();
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        startCoreServiceStateReceiver();
        startFCMReceiver();
        initAuthStateListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCoreServiceStateReceiver();
        stopFCMReceiver();
        releaseAuthStateListener();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        // TODO: 종료할 때 현재 액티비티의 로딩뷰를 삭제해줄 필요가 있을까?
        // dismissDialog();
    }

    // 현재 Resume 상태인 최상위 액티비티에서만 호출된다.
    protected void initAuthStateListener() {
        Log.i(TAG, "initAuthStateListener");

        // 이미 존재할 때는 중복 실행하지 않는다.
        if (mAuthStateListener != null) return;

        final BaseActivity activity = this;
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.i(TAG, "AuthStateListener:onAuthStateChanged");

                activity.onAuthStateChanged(firebaseAuth);

                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:uid:" + user.getUid());
                    Log.d(TAG, "onAuthStateChanged:signed_in:email:" + user.getEmail());

                    for (UserInfo info : user.getProviderData()) {
                        Log.d(TAG, "onAuthStateChanged:signed_in:provider_data:provider_id:" + info.getProviderId());
                        Log.d(TAG, "onAuthStateChanged:signed_in:provider_data:uid:" + info.getUid());
                        Log.d(TAG, "onAuthStateChanged:signed_in:provider_data:email:" + info.getEmail());
                        Log.d(TAG, "onAuthStateChanged:signed_in:provider_data:display_name:" + info.getDisplayName());
                        Log.d(TAG, "onAuthStateChanged:signed_in:provider_data:photo_url:" + info.getPhotoUrl());
                    }

                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
        AuthManager.getAuth().addAuthStateListener(mAuthStateListener);
    }

    protected void releaseAuthStateListener() {
        Log.i(TAG, "releaseAuthStateListener:auth_state_listener:" + mAuthStateListener);

        AuthManager.getAuth().removeAuthStateListener(mAuthStateListener);
        mAuthStateListener = null;
    }

    protected void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        Log.i(TAG, "onAuthStateChanged");
    }

    private void startCoreServiceStateReceiver() {
        if (mCoreServiceStateReceiver != null) return;
        mCoreServiceStateReceiver = new CoreServiceStateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.CORE_SERVICE_ACTION_ENABLED);
        intentFilter.addAction(Constants.CORE_SERVICE_ACTION_DISABLED);
        registerReceiver(mCoreServiceStateReceiver, intentFilter);
    }

    private void stopCoreServiceStateReceiver() {
        if (mCoreServiceStateReceiver == null) return;
        unregisterReceiver(mCoreServiceStateReceiver);
        mCoreServiceStateReceiver = null;
    }

    protected void onCoreServiceEnabled() {
    }

    protected void onCoreServiceDisabled() {
        finish();
    }

    private class CoreServiceStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "CoreServiceStateReceiver:onReceive:action:" + action);

            if (action.equals(Constants.CORE_SERVICE_ACTION_ENABLED))
                onCoreServiceEnabled();
            else if (action.equals(Constants.CORE_SERVICE_ACTION_DISABLED))
                onCoreServiceDisabled();
        }
    }

    // MESSAGING_EVENT
    private FCMReceiver mFCMReceiver;

    private void startFCMReceiver() {
        Log.i(TAG, "startFCMReceiver");
        if (mFCMReceiver != null) return;
        mFCMReceiver = new FCMReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.pwdr.nacky.intent.action.MESSAGING_EVENT");
        // Higher numbers have a higher priority.
        intentFilter.setPriority(1);
        registerReceiver(mFCMReceiver, intentFilter);
    }

    private void stopFCMReceiver() {
        Log.i(TAG, "stopFCMReceiver");
        if (mFCMReceiver == null) return;
        unregisterReceiver(mFCMReceiver);
        mFCMReceiver = null;
    }

    private class FCMReceiver extends BroadcastReceiver {
        final String TAG = "(A)FCMReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive");
            RemoteMessage remoteMessage = intent.getParcelableExtra(EXTRA_REMOTE_MESSAGE);
            if (mOnMessageReceiveListener != null)
                mOnMessageReceiveListener.onReceive(remoteMessage);
            // abortBroadcast();
        }
    }

    private OnMessageReceiveListener mOnMessageReceiveListener;

    public interface OnMessageReceiveListener {
        void onReceive(RemoteMessage remoteMessage);
    }

    public void setOnMessageReceiveListener(OnMessageReceiveListener l) {
        this.mOnMessageReceiveListener = l;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onFragmentResult(int requestCode, int resultCode, Intent data) {
    }

    // ProgressDialog
    public void showLoadingDialog() {
        Log.i(TAG, "showLoadingDialog");

        dismissDialog();

        if (mVGLoading == null) {
            FrameLayout loadingContainer = new FrameLayout(getApplicationContext());
            loadingContainer.setBackgroundColor(Color.parseColor("#00000000"));
            loadingContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            // HOW TO USE LOTTIE (Ref: http://airbnb.io/lottie/#/android)
            LottieAnimationView loadingView = new LottieAnimationView(getApplicationContext());
            loadingView.setAnimation(R.raw.lottie_icecream);
            loadingView.setRepeatCount(LottieDrawable.INFINITE);
            loadingView.playAnimation();

            loadingView.setLayoutParams(new FrameLayout.LayoutParams(
                    dpToPx(this, 200),
                    dpToPx(this, 200),
                    Gravity.CENTER
            ));
            loadingContainer.addView(loadingView);
            mVGLoading = loadingContainer;

            ViewGroup rootVIew = findViewById(android.R.id.content);
            rootVIew.addView(loadingContainer);
        } else {
            mVGLoading.setVisibility(View.VISIBLE);
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    public void dismissDialog() {
        Log.i(TAG, "dismissDialog");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mVGLoading != null) {
                    mVGLoading.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            }
        });
    }

    public boolean isLoading() {
        return mVGLoading != null && mVGLoading.isShown();
    }
    // /ProgressDialog

    protected Intent createIntent(Class activityClass) {
        return new Intent().setClass(getApplicationContext(), activityClass);
    }

    // 뒤로 가기를 두번 누르면 앱이 종료된다.
    private long exitTime = 0;

    public void doExitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(this, R.string.msg_press_again_to_exit_app, Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        doExitApp();
    }
}