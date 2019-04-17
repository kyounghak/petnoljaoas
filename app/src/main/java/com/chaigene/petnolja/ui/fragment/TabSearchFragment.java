package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.SearchHashtagListAdapter;
import com.chaigene.petnolja.adapter.SearchUserListAdapter;
import com.chaigene.petnolja.event.StartActivityEvent;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.model.ALGHashtag;
import com.chaigene.petnolja.model.ALGUser;
import com.chaigene.petnolja.model.FIRQuery;
import com.chaigene.petnolja.ui.activity.ChildActivity;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.EXTRA_SEARCH_TYPE;
import static com.chaigene.petnolja.Constants.SEARCH_TYPE_HASHTAG;
import static com.chaigene.petnolja.Constants.SEARCH_TYPE_USER;

public class TabSearchFragment extends TabFragment {
    public static final String TAG = "TabSearchFragment";

    private int mSearchType;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private LinearLayoutManager mManager;
    private RecyclerView.Adapter mAdapter;
    private List<ALGUser> mSearchUsers;
    private List<ALGHashtag> mSearchHashtags;

    private String mCurrentQuery;

    private ChildFragment mTargetFragment;

    private boolean mInitView;

    public boolean isInitView() {
        return mInitView;
    }

    public void setInitView(boolean initView) {
        this.mInitView = initView;
    }

    private boolean mClickLoading;

    private void setClickLoading(boolean clickLoading) {
        this.mClickLoading = clickLoading;
    }

    private boolean isClickLoading() {
        return this.mClickLoading;
    }

    public static TabSearchFragment newInstance(@IntRange(from = SEARCH_TYPE_USER, to = SEARCH_TYPE_HASHTAG) int searchType) {
        Log.i(TAG, "newInstance");

        if (searchType != SEARCH_TYPE_USER && searchType != SEARCH_TYPE_HASHTAG)
            throw new IllegalArgumentException("Unexpected search type retrived.");

        TabSearchFragment fragment = new TabSearchFragment();

        Bundle args = new Bundle();
        args.putInt(EXTRA_SEARCH_TYPE, searchType);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        Log.i(TAG, "readBundle");
        mSearchType = bundle.getInt(EXTRA_SEARCH_TYPE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_tab_notification, container, false);
        ButterKnife.bind(this, mView);
        setupRecyclerView();
        setInitView(true);

        if (mCurrentQuery != null) onSearchTextChanged(mCurrentQuery);

        // String uid = AuthManager.getUserId();
        /*DatabaseReference resultsRef = DatabaseManager.getSearchUsersResultsRef().child(uid).child("hits");
        resultsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "onDataChange");

                if (!dataSnapshot.exists()) {
                    Log.d(TAG, "onDataChange:no_data");
                }

                Log.d(TAG, "onDataChange:count:" + dataSnapshot.getChildrenCount());

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Object obj = snapshot.getValue();

                    Log.d(TAG, "onDataChange:snapshot:" + snapshot);

                    ALGUser algUser = snapshot.getValue(ALGUser.class);
                    Log.d(TAG, "onDataChange:algUser:" + algUser.toMap());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/

        return mView;
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

        mSearchUsers = new ArrayList<>();
        mSearchHashtags = new ArrayList<>();

        if (mSearchType == SEARCH_TYPE_USER) {
            SearchUserListAdapter searchUserListAdapter = new SearchUserListAdapter(getContext(), mSearchUsers, mRecyclerView);
            searchUserListAdapter.setOnItemClickListener(new SearchUserListAdapter.OnItemClickListener() {
                final String TAG = "OnItemClickListener";

                @Override
                public void onItemClick(int position, ALGUser algUser) {
                    Log.i(TAG, "onItemClick");

                    if (isClickLoading()) return;
                    setClickLoading(true);

                    String uid = algUser.getObjectID();
                    startProfileResult(uid);
                    setClickLoading(false);
                }
            });
            /*searchUserListAdapter.setOnLoadMoreListener(new NotificationListAdapter.OnLoadMoreListener() {
                final String TAG = "OnLoadMoreListener";

                @Override
                public void onLoadMore() {
                    Log.i(TAG, "onLoadMore");

                    // 로딩을 보여준다.
                    getNotifications().continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(@NonNull Task<Void> task) throws Exception {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "onLoadMore:getNotifications:ERROR");
                            }
                            mAdapter.setLoading(false);
                            return null;
                        }
                    });
                }
            });*/
            mAdapter = searchUserListAdapter;
        }

        if (mSearchType == SEARCH_TYPE_HASHTAG) {
            SearchHashtagListAdapter searchHashtagListAdapter = new SearchHashtagListAdapter(getContext(), mSearchHashtags, mRecyclerView);
            searchHashtagListAdapter.setOnItemClickListener(new SearchHashtagListAdapter.OnItemClickListener() {
                final String TAG = "OnItemClickListener";

                @Override
                public void onItemClick(int position, ALGHashtag algHashtag) {
                    Log.i(TAG, "onItemClick");

                    if (isClickLoading()) return;
                    setClickLoading(true);

                    String hashtag = algHashtag.getHashtag();
                    startHashtagResult(hashtag);
                    setClickLoading(false);
                }
            });
            /*searchUserListAdapter.setOnLoadMoreListener(new NotificationListAdapter.OnLoadMoreListener() {
                final String TAG = "OnLoadMoreListener";

                @Override
                public void onLoadMore() {
                    Log.i(TAG, "onLoadMore");

                    // 로딩을 보여준다.
                    getNotifications().continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(@NonNull Task<Void> task) throws Exception {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "onLoadMore:getNotifications:ERROR");
                            }
                            mAdapter.setLoading(false);
                            return null;
                        }
                    });
                }
            });*/
            mAdapter = searchHashtagListAdapter;
        }
        mRecyclerView.setAdapter(mAdapter);
    }

    public void onSearchTextChanged(String searchText) {
        Log.i(TAG, "onSearchTextChanged:searchText:" + searchText);
        if (isInitView()) {
            if (searchText.length() <= 0) {
                // DO NOTHING
                mSearchUsers.clear();
                mSearchHashtags.clear();
                mAdapter.notifyDataSetChanged();
            } else {
                search(searchText, true);
            }
        }
        mCurrentQuery = searchText;
    }

    private void search(String searchText, final boolean isForceRefresh) {
        String uid = AuthManager.getUserId();

        // TODO: Search type에 따라서 다른 레퍼런스를 사용해야 한다.

        String key;
        DatabaseReference queriesRef = null;
        DatabaseReference resultsRef = null;

        if (mSearchType == SEARCH_TYPE_USER) {
            key = DatabaseManager.getSearchUsersQueriesRef().child(uid).push().getKey();
            queriesRef = DatabaseManager.getSearchUsersQueriesRef().child(uid).child(key);
            resultsRef = DatabaseManager.getSearchUsersResultsRef().child(uid).child(key);
        }

        if (mSearchType == SEARCH_TYPE_HASHTAG) {
            key = DatabaseManager.getSearchHashtagsQueriesRef().child(uid).push().getKey();
            queriesRef = DatabaseManager.getSearchHashtagsQueriesRef().child(uid).child(key);
            resultsRef = DatabaseManager.getSearchHashtagsResultsRef().child(uid).child(key);
        }

        int page = 0;
        FIRQuery firQuery = new FIRQuery(searchText, page);
        DatabaseManager.Cancellable cancellable = new DatabaseManager.Cancellable();

        showInputLoading();
        DatabaseManager.getDelayValue(queriesRef, resultsRef, firQuery, cancellable).continueWith(
                new Continuation<DataSnapshot, Void>() {
                    @Override
                    public Void then(@NonNull Task<DataSnapshot> task) throws Exception {
                        dismissInputLoading();

                        if (!task.isSuccessful()) {
                            Log.w(TAG, "searchText:ERROR");
                        }

                        DataSnapshot dataSnapshot = task.getResult();

                        // 현재 검색어와 일치하지 않을 경우 종료한다.
                        String query = dataSnapshot.child("query").getValue(String.class);
                        Log.d(TAG, "searchText:current_query:" + mCurrentQuery + "/query:" + query);
                        if (!mCurrentQuery.equals(query)) {
                            return null;
                        }

                        if (isForceRefresh) {
                            mSearchUsers.clear();
                            mSearchHashtags.clear();
                            mAdapter.notifyDataSetChanged();
                        }

                        // 실제 검색결과 snapshot을 가져온다.
                        DataSnapshot hitsSnapshop = dataSnapshot.child("hits");
                        if (!hitsSnapshop.exists()) {
                            Log.d(TAG, "searchText:no_data");
                        }

                        Log.d(TAG, "searchText:count:" + hitsSnapshop.getChildrenCount());

                        if (mSearchType == SEARCH_TYPE_USER) {
                            for (DataSnapshot snapshot : hitsSnapshop.getChildren()) {
                                ALGUser algUser = snapshot.getValue(ALGUser.class);
                                Log.d(TAG, "searchText:algUser:" + algUser.toMap());

                                mSearchUsers.add(algUser);
                                int currentPosition = mSearchUsers.indexOf(algUser);
                                mAdapter.notifyItemInserted(currentPosition);
                            }
                        }

                        if (mSearchType == SEARCH_TYPE_HASHTAG) {
                            for (DataSnapshot snapshot : hitsSnapshop.getChildren()) {
                                ALGHashtag algHashtag = snapshot.getValue(ALGHashtag.class);
                                Log.d(TAG, "searchText:algHashtag:" + algHashtag.toMap());

                                mSearchHashtags.add(algHashtag);
                                int currentPosition = mSearchHashtags.indexOf(algHashtag);
                                mAdapter.notifyItemInserted(currentPosition);
                            }
                        }

                        return null;
                    }
                }
        );
    }

    // 너무 tight coupling 이라고 생각된다.
    private void showInputLoading() {
        View vLoading = getToolbar().findViewById(R.id.loading_view);
        vLoading.setVisibility(View.VISIBLE);
    }

    private void dismissInputLoading() {
        View vLoading = getToolbar().findViewById(R.id.loading_view);
        vLoading.setVisibility(View.GONE);
    }

    private void startHashtagResult(String hashtag) {
        Log.i(TAG, "startHashtagResult");
        mTargetFragment = HashtagFragment.newInstance(hashtag);
        Intent intent = createIntent(ChildActivity.class);
        startActivity(intent);
    }

    private void startProfileResult(String uid) {
        Log.i(TAG, "startProfileResult");
        mTargetFragment = ProfileFragment.newInstance(uid);
        Intent intent = createIntent(ChildActivity.class);
        startActivity(intent);
    }

    public void onStartActivityEvent(StartActivityEvent event) {
        Log.i(TAG, "onStartActivityEvent");
        RootFragment rootFragment = event.rootFragment;
        rootFragment.add(mTargetFragment);
    }

    /*private Task<Void> getNotifications() {
        Log.i(TAG, "getNotifications");

        String maxKey = null;
        if (!mUsers.isEmpty()) maxKey = mUsers.getUserPosts(mUsers.size() - 1).getKey();

        return NotificationUtil.getNotifications(mSearchType, 10, maxKey).continueWith(new Continuation<List<Notification>, Void>() {
            @Override
            public Void then(@NonNull Task<List<Notification>> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getNotifications:ERROR");
                    throw task.getException();
                }

                // TODO: 여기서 loading를 삭제해버리면 삭제된 직후 아이템이 insert 되기 때문에 살짝 깜빡거리는 것처럼 보이는 이슈가 있다.
                mAdapter.hideLoading();

                List<Notification> notifications = task.getResult();

                if (notifications.size() < 10) mAdapter.setLastDataReached(true);

                for (Notification notification : notifications) {
                    Log.d(TAG, "getNotifications:notification:" + notification.toMap());

                    mUsers.add(user);
                    int currentIndex = mUsers.indexOf(notification);
                    mAdapter.notifyItemInserted(currentIndex);
                }

                return null;
            }
        });
    }*/
}
