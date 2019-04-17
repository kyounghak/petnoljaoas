package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.event.StartActivityEvent;
import com.chaigene.petnolja.hashtag.HashTagHelper;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.model.FIRUser;
import com.chaigene.petnolja.model.User;
import com.chaigene.petnolja.ui.activity.ShopActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.ARTICLE_SCOPE_PROFILE;
import static com.chaigene.petnolja.Constants.ARTICLE_SCOPE_SAVE;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;
import static com.chaigene.petnolja.Constants.FOLLOW_TYPE_FOLLOWER;
import static com.chaigene.petnolja.Constants.FOLLOW_TYPE_FOLLOWING;
import static com.chaigene.petnolja.Constants.PROFILE_EDIT_ACTIVITY;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_SELL;

public class PageProfileFragment extends ChildFragment {
    public static final String TAG = "PageProfileFragment";

    private static final String FRAGMENT_TAG_ALL = "all";
    private static final String FRAGMENT_TAG_TALENT = "talent";
    private static final String FRAGMENT_TAG_SAVE = "save";

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @BindView(R.id.profile_image)
    ImageView mCivProfileImage;

    @BindView(R.id.nickname)
    TextView mTvNickname;

    @BindView(R.id.description)
    TextView mTvDescription;

    @BindView(R.id.purchase_count)
    TextView mTvPurchaseCount;

    @BindView(R.id.following_count)
    TextView mTvFollowingCount;

    @BindView(R.id.follower_count)
    TextView mTvFollowerCount;

    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;

    private String mMyUid;
    private User mMyUser;

    private TabArticleFragment mSelectedFragment;
    private ChildFragment mTargetFragment;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);

        mView = inflater.inflate(R.layout.page_fragment_profile, container, false);
        ButterKnife.bind(this, mView);

        // 여기서는 showDialog를 호출할 수 없다. 만약에 호출한다면 Null pointer exception이 발생한다.
        // Ref: http://stackoverflow.com/a/23653779/4729203
        showLoadingDialog();
        asyncTask().continueWith((Continuation<Void, Void>) task -> {
            dismissDialog();
            if (!task.isSuccessful()) {
                Log.w(TAG, "asyncTask:ERROR:" + task.getException());
                return null;
            }
            Log.i(TAG, "asyncTask:SUCCESS");
            initView();
            return null;
        });

        // TODO: 새로고침을 했을 때 뷰를 처음부터 다시 만들 것인가 or 각 탭에 refresh 메서드를 요청할 것인가 검토 필요.
        // 뷰를 처음부터 다시 만들 경우 가끔 탭프래그먼트가 사라지는 버그가 있음.
        mRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    final String TAG = "OnRefreshListener";

                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh");
                        mRefreshLayout.setRefreshing(false);
                        mMyUser = null;
                        showLoadingDialog();
                        asyncTask().continueWith((Continuation<Void, Void>) task -> {
                            dismissDialog();
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "onRefresh:asyncTask:ERROR:" + task.getException().getMessage());
                                return null;
                            }
                            setupProfileView();
                            return null;
                        });
                        if (mSelectedFragment != null) mSelectedFragment.onRefresh();
                    }
                }
        );

        return mView;
    }

    private Task<Void> asyncTask() {
        Log.i(TAG, "asyncTask");
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            mMyUid = AuthManager.getUserId();

            if (mMyUser == null) {
                Task<User> getUserTask = UserUtil.getUser(mMyUid, true);
                User user = Tasks.await(getUserTask);
                if (!getUserTask.isSuccessful()) {
                    Log.w(TAG, "getUserTask:ERROR:", getUserTask.getException());
                    throw getUserTask.getException();
                }
                mMyUser = user;
            }

            Log.d(TAG, "asyncTask:user:" + mMyUser.toMap().toString());

            String signature = mMyUser.getSignature();
            /*Map<String, Boolean> userRegions = mMyUser.getRegions();
            String userRegion = null;
            if (userRegions != null && !userRegions.isEmpty()) {
                userRegion = userRegions.keySet().iterator().next();
            }*/
            Task<Void> downloadProfileImageTask = UserUtil.downloadProfileImage(mMyUid, signature, mCivProfileImage);
            try {
                Tasks.await(downloadProfileImageTask);
            } catch (Exception e) {
                Log.w(TAG, "asyncTask:downloadProfileImageTask:ERROR:" + e.getMessage());
            }
            /*if (!downloadProfileImageTask.isSuccessful()) {
                Log.w(TAG, "asyncTask:downloadProfileImageTask:ERROR:", downloadProfileImageTask.getException());
                throw downloadProfileImageTask.getException();
            }*/
            return null;
        });
    }

    @Override
    protected void initView() {
        Log.i(TAG, "initView");
        super.initView();
        setupProfileView();
        setupTabs();
    }

    private void setupProfileView() {
        mTvNickname.setText(mMyUser.getNickname());
        mTvDescription.setText(mMyUser.getDescription());
        HashTagHelper helper = CommonUtil.createDefaultHashTagHelper(getContext(), hashTag -> {
            // TODO: Start HashtagFragment
        });
        helper.handle(mTvDescription);
        /* FIXME */ // mTvPurchaseCount.setText(String.valueOf(mMyUser.getPurchaseCount()));
        /* FIXME */ // mTvFollowingCount.setText(String.valueOf(mMyUser.getFollowingCount()));
        /* FIXME */ // mTvFollowerCount.setText(String.valueOf(mMyUser.getFollowerCount()));
    }

    @OnClick(R.id.purchase_count_field)
    void showShopActivity() {
        showLoadingDialog();
        UserUtil.getUser(AuthManager.getUserId()).continueWith((Continuation<User, Void>) task -> {
            dismissDialog();
            if (!task.isSuccessful()) {
                CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                return null;
            }
            User myUser = task.getResult();
            if (myUser.getType() == FIRUser.TYPE_USER) {
                CommonUtil.showSnackbar(getActivity(), "메이커 유저만 판매내역을 확인할 수 있습니다.");
                return null;
            }
            startActivity(ShopActivity.createIntent(getContext(), SHOP_TYPE_SELL));
            return null;
        });
    }

    @OnClick(R.id.follower_count_field)
    void showFollowerListWindow() {
        startFollowListFragment(FOLLOW_TYPE_FOLLOWER, AuthManager.getUserId());
    }

    @OnClick(R.id.following_count_field)
    void showFollowingListWindow() {
        startFollowListFragment(FOLLOW_TYPE_FOLLOWING, AuthManager.getUserId());
    }

    private void startFollowListFragment(int followType, String userId) {
        FollowListFragment followListFragment = FollowListFragment.newInstance(followType, userId);
        // TODO: setTargetFragment를 호출해 줄 필요가 있을까?
        // 해당 유저를 팔로우하고 프래그먼트를 종료했을 때 필요성이 있기는 할 것 같다.
        // profileFragment.setTargetFragment(this, ARTICLE_FRAGMENT);
        getRootFragment().add(followListFragment);
    }

    private void setupTabs() {
        mTabLayout.addTab(mTabLayout.newTab().setText("ALL"), true);
        mTabLayout.addTab(mTabLayout.newTab().setText("PANDA"));
        mTabLayout.addTab(mTabLayout.newTab().setText("SAVE"));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showFeedTab();
                }
                if (tab.getPosition() == 1) {
                    showTalentTab();
                }
                if (tab.getPosition() == 2) {
                    showSaveTab();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        showFeedTab();
    }

    private void showFeedTab() {
        Log.i(TAG, "showFeedTab");

        Fragment allFragment = find(FRAGMENT_TAG_ALL);
        if (allFragment == null) {
            allFragment = TabArticleFragment.newInstance(ARTICLE_SCOPE_PROFILE, ARTICLE_TYPE_ALL, AuthManager.getUserId());
            add(allFragment, FRAGMENT_TAG_ALL);
        } else {
            show(allFragment);
        }

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment != null) hide(talentFragment);
        Fragment saveFragment = find(FRAGMENT_TAG_SAVE);
        if (saveFragment != null) hide(saveFragment);

        mSelectedFragment = (TabArticleFragment) allFragment;
        Log.d(TAG, "showFeedTab:selected_fragment:" + mSelectedFragment);
    }

    private void showTalentTab() {
        Log.i(TAG, "showTalentTab");

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment == null) {
            talentFragment = TabArticleFragment.newInstance(ARTICLE_SCOPE_PROFILE, ARTICLE_TYPE_TALENT, AuthManager.getUserId());
            add(talentFragment, FRAGMENT_TAG_TALENT);
        } else {
            show(talentFragment);
        }

        Fragment allFragment = find(FRAGMENT_TAG_ALL);
        if (allFragment != null) hide(allFragment);
        Fragment saveFragment = find(FRAGMENT_TAG_SAVE);
        if (saveFragment != null) hide(saveFragment);

        mSelectedFragment = (TabArticleFragment) talentFragment;
    }

    private void showSaveTab() {
        Log.i(TAG, "showSaveTab");

        Fragment saveFragment = find(FRAGMENT_TAG_SAVE);
        if (saveFragment == null) {
            saveFragment = TabArticleFragment.newInstance(ARTICLE_SCOPE_SAVE, ARTICLE_TYPE_ALL, AuthManager.getUserId());
            add(saveFragment, FRAGMENT_TAG_SAVE);
        } else {
            show(saveFragment);
        }

        Fragment allFragment = find(FRAGMENT_TAG_ALL);
        if (allFragment != null) hide(allFragment);
        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment != null) hide(talentFragment);

        mSelectedFragment = (TabArticleFragment) saveFragment;
    }

    private Fragment find(String tag) {
        Fragment fragment = getChildFragmentManager().findFragmentByTag(tag);
        Log.i(TAG, "find:tag:" + tag + "|result:" + fragment);
        return fragment;
    }

    protected void add(Fragment fragment, String tag) {
        Log.i(TAG, "add:" + fragment + " " + tag);

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commit();
    }

    private void show(Fragment fragment) {
        Log.i(TAG, "show:" + fragment);

        getChildFragmentManager()
                .beginTransaction()
                .show(fragment)
                .commit();
    }

    private void hide(Fragment fragment) {
        Log.i(TAG, "hide:" + fragment);

        getChildFragmentManager()
                .beginTransaction()
                .hide(fragment)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROFILE_EDIT_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                showLoadingDialog();
                mMyUser = null;
                asyncTask().continueWith((Continuation<Void, Void>) task -> {
                    dismissDialog();
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "onRefresh:asyncTask:ERROR:" + task.getException().getMessage());
                    }
                    setupProfileView();
                    return null;
                });
            }
        }
    }

    // mTargetFragment를 BaseFragment에 넣는 것도 좋을 것 같다.
    @OnClick(R.id.setting_button)
    void startSettingWindow() {
        Log.i(TAG, "startSettingWindow");
        mTargetFragment = SettingFragment.newInstance();
        startChildActivity();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartActivityEvent(StartActivityEvent event) {
        Log.i(TAG, "onStartActivityEvent");
        RootFragment rootFragment = event.rootFragment;
        rootFragment.add(mTargetFragment);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (isPrimaryFragment()) {
            // inflater.inflate(R.menu.menu_fragment_page_profile, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            /*case R.id.action_profile_edit: {
                startActivityForResult(createIntent(ProfileEditActivity.class), Constants.PROFILE_EDIT_ACTIVITY);
                return true;
            }*/
            /*case R.id.action_logout: {
                showLoadingDialog();
                NotificationUtil.deleteToken().continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(@NonNull Task<Void> task) throws Exception {
                        if (!task.isSuccessful()) {
                            // 에러가 발생해도 로그아웃 한다.
                        }
                        dismissDialog();
                        AuthManager.signOut();
                        getActivity().startActivity(createIntent(LoginActivity.class));
                        getActivity().finish();
                        return null;
                    }
                });
                return true;
            }*/
            case R.id.action_settings: {
                startSettingWindow();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
