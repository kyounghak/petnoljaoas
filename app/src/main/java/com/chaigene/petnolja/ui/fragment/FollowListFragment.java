package com.chaigene.petnolja.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chaigene.petnolja.model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.FollowListAdapter;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.COUNT_FOLLOW_LIST;
import static com.chaigene.petnolja.Constants.EXTRA_FOLLOW_TYPE;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_USER_ID;
import static com.chaigene.petnolja.Constants.FOLLOW_TYPE_FOLLOWER;
import static com.chaigene.petnolja.Constants.FOLLOW_TYPE_FOLLOWING;

public class FollowListFragment extends ChildFragment {
    public static final String TAG = "FollowListFragment";

    private int mFollowType;
    private String mTargetUserId;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty_view)
    View mEmptyView;

    @BindView(R.id.empty_label_text)
    TextView mTvEmptyLabel;

    private LinearLayoutManager mManager;
    private FollowListAdapter mAdapter;
    private List<User> mUser;

    public static FollowListFragment newInstance(int followType, String userId) {
        Log.i(TAG, "newInstance");

        if (followType != FOLLOW_TYPE_FOLLOWER && followType != FOLLOW_TYPE_FOLLOWING)
            throw new IllegalArgumentException("Unexpected follow type retrived.");

        FollowListFragment fragment = new FollowListFragment();

        Bundle args = new Bundle();
        args.putInt(EXTRA_FOLLOW_TYPE, followType);
        args.putString(EXTRA_TARGET_USER_ID, userId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        Log.i(TAG, "readBundle");
        super.readBundle(bundle);
        mFollowType = bundle.getInt(EXTRA_FOLLOW_TYPE);
        mTargetUserId = bundle.getString(EXTRA_TARGET_USER_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_follow_list, container, false);
        ButterKnife.bind(this, mView);
        initView();
        showLoadingDialog();
        getFollows().continueWith((Continuation<Void, Void>) task -> {
            dismissDialog();
            if (!task.isSuccessful()) {
                Log.w(TAG, "ERROR:");
                return null;
            }
            return null;
        });
        return mView;
    }

    @Override
    protected void initView() {
        super.initView();
        String emptyLabel = null;
        if (mFollowType == FOLLOW_TYPE_FOLLOWER) {
            emptyLabel = "팔로우 유저가 없습니다.";
        }
        if (mFollowType == FOLLOW_TYPE_FOLLOWING) {
            emptyLabel = "팔로잉 유저가 없습니다.";
        }
        mTvEmptyLabel.setText(emptyLabel);
        setupRecyclerView();
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        String toolbarTitle = null;
        if (mFollowType == FOLLOW_TYPE_FOLLOWER) {
            toolbarTitle = "팔로워";
        }
        if (mFollowType == FOLLOW_TYPE_FOLLOWING) {
            toolbarTitle = "팔로잉";
        }
        setToolbarTitle(toolbarTitle);
        // setToolbarTitleAlign(Gravity.START);
    }

    private void setupRecyclerView() {
        Log.i(TAG, "setupRecyclerView");

        mManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mManager);

        // Source: http://stackoverflow.com/a/28828749/4729203
        mRecyclerView.setHasFixedSize(true);
        // false로 하게 되면 하단 부분이 짤리게 된다.
        mRecyclerView.setNestedScrollingEnabled(true);

        // Divider
        DividerItemDecoration divider = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(CommonUtil.getDrawable(getContext(), R.drawable.shape_divider_notification));
        mRecyclerView.addItemDecoration(divider);

        mUser = new ArrayList<>();
        mAdapter = new FollowListAdapter(getContext(), mUser, mRecyclerView);
        mAdapter.setEmptyView(mEmptyView);
        mAdapter.setOnItemClickListener(new FollowListAdapter.OnItemClickListener() {
            final String TAG = "OnItemClickListener";

            @Override
            public void onItemClick(User user) {
                Log.i(TAG, "onItemClick");
                startProfileFragment(user.getId());
            }

            // FIXME
            @Override
            public void onItemFollowButtonClick(User user, View btnView) {
                Log.i(TAG, "onItemFollowButtonClick");
                // updateFollow(user, btnView);
            }
        });
        mAdapter.setOnLoadMoreListener(new FollowListAdapter.OnLoadMoreListener() {
            final String TAG = "OnLoadMoreListener";

            @Override
            public void onLoadMore() {
                Log.i(TAG, "onLoadMore");

                // 로딩을 보여준다.
                getFollows().continueWith((Continuation<Void, Void>) task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "onLoadMore:getFollows:ERROR");
                    }
                    mAdapter.setLoading(false);
                    return null;
                });
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    // FIXME
    /*private void updateFollow(User user, final View btnView) {
        btnView.setEnabled(false);
        showLoadingDialog();
        String targetUid = user.getId();
        Task<Void> followTask;
        final boolean isActivated = btnView.isActivated();
        if (!isActivated) {
            followTask = UserUtil.follow(targetUid);
        } else {
            followTask = UserUtil.unfollow(targetUid);
        }
        followTask.continueWith((Continuation<Void, Void>) task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "follow:ERROR");
                dismissDialog();
                btnView.setEnabled(true);
                return null;
            }
            dismissDialog();
            btnView.setActivated(!isActivated);
            btnView.setEnabled(true);

            Log.d(TAG, "updateFollow:isActivated:" + btnView.isActivated());

            return null;
        });
    }*/

    private void startProfileFragment(String uid) {
        ProfileFragment profileFragment = ProfileFragment.newInstance(uid);
        // TODO: setTargetFragment를 호출해 줄 필요가 있을까?
        // 해당 유저를 팔로우하고 프래그먼트를 종료했을 때 필요성이 있기는 할 것 같다.
        // profileFragment.setTargetFragment(this, ARTICLE_FRAGMENT);
        getRootFragment().add(profileFragment);
    }

    private Task<Void> getFollows() {
        Log.i(TAG, "getFollows");

        mAdapter.setLoadingDataset(true);

        String maxKey = null;
        if (!mUser.isEmpty()) maxKey = mUser.get(mUser.size() - 1).getId();

        Task<List<User>> task = null;
        if (mFollowType == FOLLOW_TYPE_FOLLOWER) {
            task = UserUtil.getFollowers(mTargetUserId, COUNT_FOLLOW_LIST, maxKey);
        }
        if (mFollowType == FOLLOW_TYPE_FOLLOWING) {
            task = UserUtil.getFollowings(mTargetUserId, COUNT_FOLLOW_LIST, maxKey);
        }

        return task.continueWith((Continuation<List<User>, Void>) task12 -> {
            if (!task12.isSuccessful()) {
                Exception getFollowsError = task12.getException();
                Log.w(TAG, "getFollows:ERROR", getFollowsError);
                throw getFollowsError;
            }

            mAdapter.setLoadingDataset(false);
            mAdapter.hideLoading();

            List<User> users = task12.getResult();

            if (users.size() < COUNT_FOLLOW_LIST) mAdapter.setLastDataReached(true);

            if (users.isEmpty() && users.isEmpty()) {
                Log.d(TAG, "getFollows:current_dataset_and_new_dataset_are_empty");
                mAdapter.notifyDataSetChanged();
            }

            for (User user : users) {
                Log.d(TAG, "getFollows:user:" + user.toMap());
                mUser.add(user);
                int currentIndex = mUser.indexOf(user);
                mAdapter.notifyItemInserted(currentIndex);
            }

            return null;
        }).continueWith(task1 -> {
            if (!task1.isSuccessful()) {
                Log.w(TAG, "getFollows:ERROR:" + task1.getException());
                throw task1.getException();
            }
            Log.d(TAG, "getFollows:SUCCESS");
            return null;
        });
    }

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
        }
    }

    private void finish() {
        Log.i(TAG, "finish");
        getActivity().onBackPressed();
    }
}