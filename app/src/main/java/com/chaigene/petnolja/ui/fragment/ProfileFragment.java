package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.chaigene.petnolja.hashtag.HashTagHelper;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.model.FIRUser;
import com.chaigene.petnolja.model.User;
import com.chaigene.petnolja.ui.activity.ChatActivity;
import com.chaigene.petnolja.ui.activity.ShopActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.ARTICLE_SCOPE_PROFILE;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;
import static com.chaigene.petnolja.Constants.CHAT_ACTIVITY;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_NICKNAME;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_USER;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_USER_ID;
import static com.chaigene.petnolja.Constants.FOLLOW_TYPE_FOLLOWER;
import static com.chaigene.petnolja.Constants.FOLLOW_TYPE_FOLLOWING;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_SELL;

public class ProfileFragment extends ChildFragment {
    public static final String TAG = "ProfileFragment";

    private static final String FRAGMENT_TAG_FEED = String.valueOf(ARTICLE_TYPE_FEED);
    private static final String FRAGMENT_TAG_TALENT = String.valueOf(ARTICLE_TYPE_TALENT);

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

    @BindView(R.id.follow_button)
    ImageView mIvFollowBtn;

    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;

    @BindView(R.id.fragment_container)
    ViewGroup mVgFragmentContainer;

    private String mTargetUid;
    private String mTargetNickname;
    private User mTargetUser;

    private TabArticleFragment mSelectedFragment;

    // 어떤 유저인지 Uid를 넘겨받아야 페이지를 표시할 수 있다.
    // 다만 종료할 때 팔로우 여부 등을 넘겨줘야 한다.
    // 팔로우 여부를 넘겨주려면 OldUser 객체를 넘겨야한다.
    // 근데 게시글 -> 유저 -> 게시글 -> 유저 -> 게시글 -> 유저 이럴 경우에는 어떻게 해야 할까?
    // 메모리 상에 역시 값을 가지고 있는 방법이 최선일 것 같다.
    public static ProfileFragment newInstance(@NonNull String userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_TARGET_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    // userId와 nickname이 둘 다 존재하는 경우에는 무조건 userId 기준으로 유저를 불러온다.
    public static ProfileFragment newInstance(@Nullable String userId, @NonNull String nickname) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_TARGET_USER_ID, userId);
        args.putSerializable(EXTRA_TARGET_NICKNAME, nickname);
        fragment.setArguments(args);
        return fragment;
    }

    public static ProfileFragment newInstance(@NonNull User targetUser) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_TARGET_USER, targetUser);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.bind(this, mView);

        showLoadingDialog();
        asyncTask().continueWith((Continuation<Void, Void>) task -> {
            dismissDialog();
            if (!task.isSuccessful()) {
                Log.w(TAG, "asyncTask:ERROR:", task.getException());
                if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                    mTvNickname.setText("(알 수 없음)");
                    // mTvDescription.setText("해당 유저가 존재하지 않습니다.");
                    CommonUtil.showSnackbar(getActivity(), "해당 유저가 존재하지 않습니다.");
                }
                // finish();
                return null;
            }
            initView();
            return null;
        });

        mRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    final String TAG = "OnRefreshListener";

                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh");
                        mRefreshLayout.setRefreshing(false);
                        mTargetUser = null;
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

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        mTargetUid = bundle.getString(EXTRA_TARGET_USER_ID);
        mTargetNickname = bundle.getString(EXTRA_TARGET_NICKNAME);
        mTargetUser = (User) bundle.getSerializable(EXTRA_TARGET_USER);
        Log.i(TAG, "readBundle:" +
                "targetUid:" + mTargetUid + "|" +
                "targetNickname:" + mTargetNickname + "|" +
                "targetUser:" + (mTargetUser != null ? mTargetUser.toMap() : "null"));
    }

    private Task<Void> asyncTask() {
        Log.i(TAG, "asyncTask");
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            // 절대 발생해서는 안되는 상황.
            if (mTargetUid == null && mTargetNickname == null && mTargetUser == null) {
                finish();
                return null;
            }

            if (mTargetUser == null) {
                Task<User> getUserTask = null;
                if (mTargetNickname != null) {
                    getUserTask = UserUtil.getUserByNickname(mTargetNickname);
                }
                if (mTargetUid != null) {
                    getUserTask = UserUtil.getUser(mTargetUid, true);
                }
                User user = Tasks.await(getUserTask);
                /*if (!getUserTask.isSuccessful()) {
                    Log.w(TAG, "getUserTask:ERROR:", getUserTask.getException());
                    throw getUserTask.getException();
                }*/
                if (user == null) {
                    /*CommonUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "해당 유저가 존재하지 않습니다.", Toast.LENGTH_LONG).show();
                            // finish();
                        }
                    });*/
                    throw new FirebaseAuthInvalidUserException(
                            String.valueOf(CommonStatusCodes.INVALID_ACCOUNT),
                            "The user does not exist."
                    );
                }
                mTargetUser = user;
            }

            if (mTargetUid == null) {
                mTargetUid = mTargetUser.getId();
            }

            Log.d(TAG, "asyncTask:user:" + mTargetUser.toMap().toString());

            String signature = mTargetUser.getSignature();
            /*Map<String, Boolean> userRegions = mTargetUser.getRegions();
            String userRegion = null;
            if (userRegions != null && !userRegions.isEmpty()) {
                userRegion = userRegions.keySet().iterator().next();
            }*/
            Task<Void> downloadProfileImageTask = UserUtil.downloadProfileImage(mTargetUid, signature, mCivProfileImage);
            /*Tasks.await(downloadProfileImageTask);
            if (!downloadProfileImageTask.isSuccessful()) {
                Log.w(TAG, "downloadProfileImageTask:ERROR:", downloadProfileImageTask.getException());
                throw downloadProfileImageTask.getException();
            }*/
            try {
                Tasks.await(downloadProfileImageTask);
            } catch (Exception e) {
                Log.w(TAG, "asyncTask:downloadProfileImageTask:ERROR:" + e.getMessage());
            }
            return null;
        });
    }

    @Override
    protected void initView() {
        Log.i(TAG, "initView");
        super.initView();
        getActivity().invalidateOptionsMenu();
        setupProfileView();
        setupTabs();
    }

    private void setupProfileView() {
        boolean isMyProfile = mTargetUid.equals(AuthManager.getUserId());
        if (!isMyProfile) {
            // FIXME
            /*if (mTargetUser.isFollowing()) {
                mIvFollowBtn.setActivated(true);
            } else {
                mIvFollowBtn.setActivated(false);
            }*/
            mIvFollowBtn.setVisibility(View.VISIBLE);
        } else {
            mIvFollowBtn.setVisibility(View.GONE);
        }

        String nickname = !mTargetUser.isDeleted() ? mTargetUser.getNickname() : "(알 수 없음)";
        mTvNickname.setText(nickname);
        mTvDescription.setText(mTargetUser.getDescription());
        HashTagHelper helper = CommonUtil.createDefaultHashTagHelper(getContext(), hashTag -> {
            // TODO: Start HashtagFragment
        });
        helper.handle(mTvDescription);
        /* FIXME */ // mTvPurchaseCount.setText(String.valueOf(mTargetUser.getPurchaseCount()));
        /* FIXME */ // mTvFollowingCount.setText(String.valueOf(mTargetUser.getFollowingCount()));
        /* FIXME */ // mTvFollowerCount.setText(String.valueOf(mTargetUser.getFollowerCount()));
    }

    @OnClick(R.id.purchase_count_field)
    void showShopActivity() {
        if (!mTargetUid.equals(AuthManager.getUserId())) return;
        if (mTargetUser.getType() == FIRUser.TYPE_USER) return;
        startActivity(ShopActivity.createIntent(getContext(), SHOP_TYPE_SELL));
    }

    @OnClick(R.id.follower_count_field)
    void showFollowerListWindow() {
        startFollowListFragment(FOLLOW_TYPE_FOLLOWER, mTargetUid);
    }

    @OnClick(R.id.following_count_field)
    void showFollowingListWindow() {
        startFollowListFragment(FOLLOW_TYPE_FOLLOWING, mTargetUid);
    }

    private void startFollowListFragment(int followType, String userId) {
        FollowListFragment followListFragment = FollowListFragment.newInstance(followType, userId);
        // TODO: setTargetFragment를 호출해 줄 필요가 있을까?
        // 해당 유저를 팔로우하고 프래그먼트를 종료했을 때 필요성이 있기는 할 것 같다.
        // profileFragment.setTargetFragment(this, ARTICLE_FRAGMENT);
        getRootFragment().add(followListFragment);
    }

    private void setupTabs() {
        Log.i(TAG, "setupTabs");

        // Depreacted (탭이 한번씩 재생성 되지 않는 버그가 존재한다.)
        // 새로고침시 다시 이 메서드를 호출할 수 있게 탭을 초기화 해준다.
        // mTabLayout.removeAllTabs();
        // mTabLayout.clearOnTabSelectedListeners();
        // clear();

        mTabLayout.addTab(mTabLayout.newTab().setText("ALL"), true);
        mTabLayout.addTab(mTabLayout.newTab().setText("PANDA"));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int selectedTab = tab.getPosition() + 1;

                if (selectedTab == ARTICLE_TYPE_FEED) {
                    showFeedTab();
                }
                if (selectedTab == ARTICLE_TYPE_TALENT) {
                    showTalentTab();
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

        Fragment feedFragment = find(FRAGMENT_TAG_FEED);
        if (feedFragment == null) {
            feedFragment = TabArticleFragment.newInstance(ARTICLE_SCOPE_PROFILE, ARTICLE_TYPE_ALL, mTargetUid);
            add(feedFragment, FRAGMENT_TAG_FEED);
        } else {
            show(feedFragment);
        }

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment != null) hide(talentFragment);

        mSelectedFragment = (TabArticleFragment) feedFragment;
    }

    private void showTalentTab() {
        Log.i(TAG, "showTalentTab");

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment == null) {
            talentFragment = TabArticleFragment.newInstance(ARTICLE_SCOPE_PROFILE, ARTICLE_TYPE_TALENT, mTargetUid);
            add(talentFragment, FRAGMENT_TAG_TALENT);
        } else {
            show(talentFragment);
        }

        Fragment feedFragment = find(FRAGMENT_TAG_FEED);
        if (feedFragment != null) hide(feedFragment);

        mSelectedFragment = (TabArticleFragment) talentFragment;
    }

    private Fragment find(String tag) {
        Fragment fragment = getChildFragmentManager().findFragmentByTag(tag);
        Log.i(TAG, "find:tag:" + tag + "/result:" + fragment);
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

    private void remove(Fragment fragment) {
        Log.i(TAG, "remove:" + fragment);

        getChildFragmentManager()
                .beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss();

        getChildFragmentManager().popBackStackImmediate();
    }

    private void clear() {
        Log.i(TAG, "clear");
        Fragment feedFragment = find(FRAGMENT_TAG_FEED);
        if (feedFragment != null) remove(feedFragment);
        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment != null) remove(talentFragment);
        // Source: https://stackoverflow.com/a/26439642/4729203
        // getChildFragmentManager().popBackStack(null, POP_BACK_STACK_INCLUSIVE);
        // mVgFragmentContainer.removeAllViewsInLayout();
    }

    // FIXME
    /*@OnClick(R.id.follow_button)
    void updateFollow(final View view) {
        view.setEnabled(false);
        showLoadingDialog();
        Task<Void> followTask;
        final boolean isActivated = view.isActivated();
        if (!isActivated) {
            followTask = UserUtil.follow(mTargetUid);
        } else {
            followTask = UserUtil.unfollow(mTargetUid);
        }
        followTask.continueWith((Continuation<Void, Void>) task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "follow:ERROR");
                dismissDialog();
                view.setEnabled(true);
                return null;
            }
            dismissDialog();
            view.setActivated(!isActivated);
            view.setEnabled(true);
            // TODO: 팔로우 버튼을 바꿔줘야 한다.
            Log.d(TAG, "updateFollow:isActivated:" + view.isActivated());
            mTargetUser = null;
            asyncTask().continueWith((Continuation<Void, Void>) task1 -> {
                if (!task1.isSuccessful()) {
                    Log.w(TAG, "updateFollow:asyncTask:ERROR:" + task1.getException().getMessage());
                }
                setupProfileView();
                return null;
            });
            return null;
        });
    }*/

    // 일단 메뉴가 없다고 가정.
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (isPrimaryFragment()) {
            getToolbar().setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                final String TAG = "OnClickListener";

                @Override
                public void onClick(View v) {
                    Log.i(TAG, "onClick");
                    finish();
                }
            });

            // 내 프로필 페이지일 경우 메세지 메뉴를 보여주지 않는다.
            if (mTargetUid == null || mTargetUid.equals(AuthManager.getUserId())) return;

            inflater.inflate(R.menu.menu_fragment_profile, menu);

            MenuItem menuItemChat = menu.findItem(R.id.action_chat);
            /*Drawable chatIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_chat);
            menuItemChat.setIcon(chatIcon);*/

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_chat);
            Bitmap newIcon = resizeBitmapImageFn(icon, 80);
            Drawable d = new BitmapDrawable(getResources(), newIcon);
            menuItemChat.setIcon(d);
        }
    }

    private Bitmap resizeBitmapImageFn(Bitmap bmpSource, int maxResolution) {
        int iWidth = bmpSource.getWidth();
        int iHeight = bmpSource.getHeight();
        int newWidth = iWidth;
        int newHeight = iHeight;
        float rate = 0.0f;

        if (iWidth > iHeight) {
            if (maxResolution < iWidth) {
                rate = maxResolution / (float) iWidth;
                newHeight = (int) (iHeight * rate);
                newWidth = maxResolution;
            }
        } else {
            if (maxResolution < iHeight) {
                rate = maxResolution / (float) iHeight;
                newWidth = (int) (iWidth * rate);
                newHeight = maxResolution;
            }
        }

        return Bitmap.createScaledBitmap(bmpSource, newWidth, newHeight, true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.i(TAG, "onPrepareOptionsMenu");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            /*case android.R.id.home:
                finish();
                return true;*/
            case R.id.action_chat:
                Intent intent = ChatActivity.createIntent(getContext(), mTargetUser);
                startActivityForResult(intent, CHAT_ACTIVITY);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void finish() {
        Log.i(TAG, "finish");
        getActivity().onBackPressed();
        // getRootFragment().pop();
    }
}