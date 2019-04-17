package com.chaigene.petnolja.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.chaigene.petnolja.manager.TasksManager;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.RemoteMessage;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.PageFragmentAdapter;
import com.chaigene.petnolja.event.ChatBadgeEvent;
import com.chaigene.petnolja.event.StartActivityEvent;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.ui.fragment.ArticleFragment;
import com.chaigene.petnolja.ui.fragment.ChildFragment;
import com.chaigene.petnolja.ui.fragment.ProfileFragment;
import com.chaigene.petnolja.ui.fragment.RootFragment;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.NotificationUtil;
import com.chaigene.petnolja.util.UserUtil;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Map;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.ACTION_STATUS_ARTICLE;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_DEFAULT;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_PROFILE;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_SHOP;
import static com.chaigene.petnolja.Constants.EXTRA_ACTION_STATUS;
import static com.chaigene.petnolja.Constants.EXTRA_ORDER_ID;
import static com.chaigene.petnolja.Constants.EXTRA_SHOP_TYPE;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_POST_ID;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_USER_ID;
import static com.chaigene.petnolja.Constants.PAGE_EXPLORE;
import static com.chaigene.petnolja.Constants.PAGE_GALLERY;
import static com.chaigene.petnolja.Constants.PAGE_HOME;
import static com.chaigene.petnolja.Constants.PAGE_NOTIFICATION;
import static com.chaigene.petnolja.Constants.PAGE_PROFILE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_CHAT_MESSAGE;
import static com.chaigene.petnolja.ui.activity.LoginActivity.FRAGMENT_TAG_SETTING_PROFILE;

public class OldMainActivity extends BaseActivity {
    public static final String TAG = "OldMainActivity";

    @BindView(R.id.tabs)
    TabLayout mTabLayout;

    @BindView(R.id.view_pager)
    ViewPager mViewPager;

    private int mActionStatus;
    private String mTargetPostId;
    private String mTargetUserId;
    private int mTargetShopType;
    private String mTargetOrderId;
    @Deprecated
    private String mTargetRoomId;

    private PageFragmentAdapter mAdapter;

    private RootFragment mRootHomeFragment;
    private RootFragment mRootExploreFragment;
    private RootFragment mRootGalleryFragment;
    private RootFragment mRootNotificationFragment;
    private RootFragment mRootProfileFragment;

    private ChildFragment mTargetFragment;

    private static int[] imageResIds = {
            R.drawable.selector_tab_home,
            R.drawable.selector_tab_explore,
            R.drawable.selector_tab_gallery,
            R.drawable.selector_tab_notification,
            R.drawable.selector_tab_profile
    };

    private ValueEventListener mChatBadgeListener;

    // private int mUncheckedCount;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 앱이 복구되면 스택이 전부 사라진다.
        // 따라서 자체적으로 스택을 추적하여 앱이 복구될 때 모두 복구시키거나
        // 아니면 걍 앱을 재실행하는 방법 밖에 없다.
        // Souce: https://stackoverflow.com/a/35700161/4729203
        if (savedInstanceState != null) {
            Intent intent = new Intent(this, LauncherActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        setContentView(R.layout.old_activity_main);
        ButterKnife.bind(this);

        asyncTask();

        setupViewPager();
        setupTab();

        startInitialWindow();
    }

    @Override
    protected void readIntent() {
        Log.i(TAG, "readIntent");
        super.readIntent();

        // Handle possible data accompanying notification message.
        /*if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "readIntent:key:" + key + "|value:" + value);
            }
        } else {
            Log.d(TAG, "readIntent:extras_are_null");
        }*/

        mActionStatus = getIntent().getIntExtra(EXTRA_ACTION_STATUS, 0);
        mTargetPostId = getIntent().getStringExtra(EXTRA_TARGET_POST_ID);
        mTargetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        mTargetShopType = getIntent().getIntExtra(EXTRA_SHOP_TYPE, 0);
        mTargetOrderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        /*if (TextUtils.isEmpty(mProductId) ||
                TextUtils.isEmpty(mProductTitle) ||
                TextUtils.isEmpty(mProductPrice) ||
                TextUtils.isEmpty(mCoverPhoto) ||
                TextUtils.isEmpty(mSellerId) ||
                TextUtils.isEmpty(mSellerNickname)) {
            Toast.makeText(getApplicationContext(), "주문정보에 오류가 있습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
            finish();
        }*/

        getIntent().removeExtra(EXTRA_ACTION_STATUS);
        getIntent().removeExtra(EXTRA_TARGET_POST_ID);
        getIntent().removeExtra(EXTRA_TARGET_USER_ID);
        getIntent().removeExtra(EXTRA_SHOP_TYPE);
        getIntent().removeExtra(EXTRA_ORDER_ID);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent");
        super.onNewIntent(intent);
        setIntent(intent);
        readIntent();
        startInitialWindow();
    }

    private void startInitialWindow() {
        Log.i(TAG, "startInitialWindow");
        if (mActionStatus == ACTION_STATUS_DEFAULT) return;

        if (mActionStatus == ACTION_STATUS_ARTICLE) {
            ChildFragment targetFragment = ArticleFragment.newInstance(mTargetPostId);
            startChildActivity(targetFragment);
        }

        if (mActionStatus == ACTION_STATUS_PROFILE) {
            ChildFragment targetFragment = ProfileFragment.newInstance(mTargetUserId);
            startChildActivity(targetFragment);
        }

        if (mActionStatus == ACTION_STATUS_SHOP) {
            Intent in = ShopActivity.createIntent(this, mTargetShopType, mTargetOrderId);
            startActivity(in);
        }
    }

    private void startChildActivity(ChildFragment targetFragment) {
        Log.i(TAG, "startChildActivity");
        this.mTargetFragment = targetFragment;
        Intent intent = createIntent(ChildActivity.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartActivityEvent(StartActivityEvent event) {
        Log.i(TAG, "onStartActivityEvent");
        RootFragment rootFragment = event.rootFragment;
        rootFragment.add(mTargetFragment);
    }

    private void asyncTask() {
        Log.i(TAG, "asyncTask");

        TasksManager.call((Callable<Void>) () -> {
            Task<Boolean> isUserExistsTask = UserUtil.isExists(AuthManager.getUserId());
            Tasks.await(isUserExistsTask);

            // 가입을 완료하지 않은 유저라면 유저정보 입력 페이지를 보여준다.
            /*if (!isUserExistsTask.isSuccessful()) {
                Log.w(TAG, "asyncTask:isUserExistsTask:ERROR:");
            }*/

            boolean isUserExists = isUserExistsTask.getResult();
            if (!isUserExists) {
                Log.d(TAG, "asyncTask:isUserExists:false:show_insert_user_info_page");

                // TODO: LoginActivity를 띄우고 MainActivity를 종료시킨다.
                Intent in = LoginActivity.createIntent(getApplicationContext(), FRAGMENT_TAG_SETTING_PROFILE);
                in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(in);
                finish();
            }

            Task<Integer> getUncheckedCountTask = NotificationUtil.getUncheckedCount();
            Tasks.await(getUncheckedCountTask);

            /*if (!getUncheckedCountTask.isSuccessful()) {
                Log.w(TAG, "asyncTask:getUncheckedCount:ERROR:");
            }*/

            int uncheckedCount = getUncheckedCountTask.getResult();
            NotificationUtil.getInstance().saveUncheckedCount(uncheckedCount);
            updateNotificationBadge();

            return null;
        }).continueWith((Continuation<Void, Void>) task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "asyncTask:ERROR:", task.getException());
                return null;
            }
            return null;
        });
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
    }

    // 하단의 페이지 이동은 ViewPager로 이루어진다.
    private void setupViewPager() {
        mAdapter = new PageFragmentAdapter(getSupportFragmentManager());

        if (mRootHomeFragment == null)
            mRootHomeFragment = RootFragment.newInstance(PAGE_HOME);
        if (mRootExploreFragment == null)
            mRootExploreFragment = RootFragment.newInstance(PAGE_EXPLORE);
        if (mRootGalleryFragment == null)
            mRootGalleryFragment = RootFragment.newInstance(PAGE_GALLERY);
        if (mRootNotificationFragment == null)
            mRootNotificationFragment = RootFragment.newInstance(PAGE_NOTIFICATION);
        if (mRootProfileFragment == null)
            mRootProfileFragment = RootFragment.newInstance(PAGE_PROFILE);

        mAdapter.addFragment(mRootHomeFragment, null);
        mAdapter.addFragment(mRootExploreFragment, null);
        mAdapter.addFragment(mRootGalleryFragment, null);
        mAdapter.addFragment(mRootNotificationFragment, null);
        mAdapter.addFragment(mRootProfileFragment, null);

        mViewPager.setAdapter(mAdapter);
        // Minimum is 1 (Ref: https://stackoverflow.com/a/10073916/4729203)
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.addOnPageChangeListener(new OnPageChangeListener());
    }

    // 탭을 클릭했을 때 호출되는 메서드.
    private void setupTab() {
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                mViewPager.setCurrentItem(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.getTabAt(0).setIcon(imageResIds[0]);
        mTabLayout.getTabAt(1).setIcon(imageResIds[1]);

        // 가운데 아이콘만 사이즈가 크다.
        mTabLayout.getTabAt(2).setIcon(imageResIds[2]);
        mTabLayout.getTabAt(2).setCustomView(R.layout.view_tab);

        mTabLayout.getTabAt(3).setIcon(imageResIds[3]);
        mTabLayout.getTabAt(3).setCustomView(R.layout.view_tab_notification);
        mTabLayout.getTabAt(4).setIcon(imageResIds[4]);
        KeyboardVisibilityEvent.setEventListener(
                this,
                b -> mTabLayout.setVisibility(b ? View.GONE : View.VISIBLE)
        );
    }

    @Nullable
    public ViewPager getViewPager() {
        return mViewPager;
    }

    @Nullable
    public PageFragmentAdapter getPagerAdapter() {
        return mAdapter;
    }

    @Nullable
    public ChildFragment getPageFragment(int position) {
        RootFragment rootFragment = (RootFragment) mAdapter.getItem(position);
        if (rootFragment == null) return null;
        return rootFragment.getBaseFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        initChatBadgeListener();
        initOnMessageReceiveListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        releaseOnMessageReceiveListener();
        releaseChatBadgeListener();
    }

    // 현재 Resume 상태인 최상위 액티비티에서만 호출된다.
    protected void initChatBadgeListener() {
        Log.i(TAG, "initChatBadgeListener");

        // 이미 존재할 때는 중복 실행하지 않는다.
        if (mChatBadgeListener != null) return;

        mChatBadgeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Log.i(TAG, "onDataChange");
                Integer totalUnreadCount = dataSnapshot.getValue(Integer.class);
                totalUnreadCount = totalUnreadCount != null ? totalUnreadCount : 0;
                Log.d(TAG, "onDataChange:totalUnreadCount:" + totalUnreadCount);
                EventBus.getDefault().post(new ChatBadgeEvent(totalUnreadCount));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled");
            }
        };

        DatabaseReference countRef = DatabaseManager.getChatTotalUnreadCountRef().child(AuthManager.getUserId());
        countRef.addValueEventListener(mChatBadgeListener);
    }

    protected void releaseChatBadgeListener() {
        Log.i(TAG, "releaseChatBadgeListener");
        // Log.i(TAG, "releaseAuthStateListener:auth_state_listener:" + mAuthStateListener);

        DatabaseReference countRef = DatabaseManager.getChatTotalUnreadCountRef().child(AuthManager.getUserId());
        countRef.removeEventListener(mChatBadgeListener);
        mChatBadgeListener = null;
    }

    /*protected void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        Log.i(TAG, "onAuthStateChanged");
    }*/

    // 이렇게 할 경우 메모리가 초기화 되면 알림 갯수가 사라진다.
    // 또한 액티비티가 pausing 상태일 경우 카운팅을 할 수 없다.
    // preference에 담는 방법이 좋을 것 같다.
    private void initOnMessageReceiveListener() {
        Log.i(TAG, "initOnMessageReceiveListener");
        setOnMessageReceiveListener(new OnMessageReceiveListener() {
            final String TAG = "(A)FCMListener";

            @Override
            public void onReceive(RemoteMessage remoteMessage) {
                Log.i(TAG, "onReceive");

                /*String body = remoteMessage.getNotification().getBody();
                if (body != null) {
                    Log.d(TAG, "onReceive:body: " + body);
                }*/

                // TODO: FIRNotification을 생성해서 adapter의 dataset을 업데이트 해준다.
                Map<String, String> data = remoteMessage.getData();
                if (data.size() > 0) {
                    Log.d(TAG, "onReceive:data: " + remoteMessage.getData());
                }

                if (!data.containsKey("type")) return;

                int type = Integer.parseInt(data.get("type"));
                if (type == TYPE_CHAT_MESSAGE) return;

                updateNotificationBadge();
            }
        });
    }

    protected void releaseOnMessageReceiveListener() {
        Log.i(TAG, "releaseOnMessageReceiveListener");
        // ((BaseActivity) getActivity()).setOnMessageReceiveListener(null);
    }

    public void updateNotificationBadge() {
        if (getViewPager().getCurrentItem() == PAGE_NOTIFICATION) {
            NotificationUtil.getInstance().clearUncheckedCount();
        }
        final int uncheckedCount = NotificationUtil.getInstance().loadUncheckedCount();
        Log.i(TAG, "updateNotificationBadge:" + uncheckedCount);
        CommonUtil.runOnUiThread(() -> {
            TabLayout.Tab notificationTab = mTabLayout.getTabAt(PAGE_NOTIFICATION);
            if (notificationTab == null || notificationTab.getCustomView() == null) return;
            TextView tvBadge = notificationTab.getCustomView().findViewById(R.id.badge);
            if (tvBadge == null) return;
            tvBadge.setVisibility(uncheckedCount > 0 ? View.VISIBLE : View.GONE);
            tvBadge.setText(String.valueOf(uncheckedCount));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        Log.i(TAG, "onBackPressed:backStackCount:" + backStackCount);

        int currentIndex = mViewPager.getCurrentItem();
        Fragment currentFragment = mAdapter.getItem(currentIndex);
        FragmentManager childFragmentManager = currentFragment.getChildFragmentManager();
        int childBackStackCount = childFragmentManager.getBackStackEntryCount();
        Log.i(TAG, "onBackPressed:childBackStackCount:" + childBackStackCount);

        // 최초의 BaseFragment는 남겨두고 모두 제거한다.
        if (childBackStackCount > 1) {
            childFragmentManager.popBackStack();
        } else super.onBackPressed();
    }

    // TODO: BaseFragment
    class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        final String TAG = "OnPageChangeListener";

        final Activity mainActivity = OldMainActivity.this;

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            Log.i(TAG, "onPageSelected:" + position);

            switch (position) {
                case PAGE_HOME:
                    /*AnalyticsManager.setCurrentScreen(
                            mainActivity,
                            AuthManager.getEmail() + "|",
                            PageHomeFragment.TAG
                    );*/
                    break;
                case PAGE_EXPLORE:
                    break;
                case PAGE_GALLERY:
                    break;
                case PAGE_NOTIFICATION:
                    if (NotificationUtil.getInstance().loadUncheckedCount() > 0) {
                        NotificationUtil.getInstance().clearUncheckedCount();
                        updateNotificationBadge();
                        NotificationUtil.checkAll().continueWith((Continuation<Void, Void>) task -> {
                            if (!task.isSuccessful()) {
                                // ERROR
                                Log.w(TAG, "checkAll:ERROR:" + task.getException().getMessage());
                            }
                            // SUCCESS
                            Log.d(TAG, "checkAll:SUCCESS");
                            return null;
                        });
                    }
                    break;
                case PAGE_PROFILE:
                    break;
            }

            /*if (mOnViewPagerChangeListener != null) {
                mOnViewPagerChangeListener.OnPageSelected(position);
            }*/
        }
    }

    private OnViewPagerChangeListener mOnViewPagerChangeListener;

    public interface OnViewPagerChangeListener {
        void OnPageSelected(int position);
    }

    public void setOnViewPagerChangeListener(OnViewPagerChangeListener l) {
        this.mOnViewPagerChangeListener = l;
    }

    public void removeOnViewPagerChangeListener() {
        this.mOnViewPagerChangeListener = null;
    }

    // Like, Comment
    public static Intent createIntentForArticle(Context context, String postId) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, OldMainActivity.class);
        intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_ARTICLE);
        intent.putExtra(EXTRA_TARGET_POST_ID, postId);
        return intent;
    }

    // Follow
    public static Intent createIntentForProfile(Context context, String userId) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, OldMainActivity.class);
        intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_PROFILE);
        intent.putExtra(EXTRA_TARGET_USER_ID, userId);
        return intent;
    }

    // Shop
    public static Intent createIntentForShop(Context context, int shopType, String orderId) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, OldMainActivity.class);
        intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_SHOP);
        intent.putExtra(EXTRA_SHOP_TYPE, shopType);
        intent.putExtra(EXTRA_ORDER_ID, orderId);
        return intent;
    }
}