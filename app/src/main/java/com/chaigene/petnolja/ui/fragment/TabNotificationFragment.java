package com.chaigene.petnolja.ui.fragment;

import android.content.Context;
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

import com.chaigene.petnolja.ui.dialog.DialogListFragment;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.RemoteMessage;
import com.chaigene.petnolja.BuildConfig;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.NotificationListAdapter;
import com.chaigene.petnolja.model.Notification;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.activity.SearchActivity;
import com.chaigene.petnolja.ui.activity.ShopActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.NotificationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;
import static com.chaigene.petnolja.Constants.COUNT_NOTIFICATION;
import static com.chaigene.petnolja.Constants.EXTRA_ARTICLE_TYPE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_CHAT_MESSAGE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_COMMENT;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_FOLLOW;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_LIKE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_MENTION_ARTICLE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_MENTION_COMMENT;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_SHOP;

public class TabNotificationFragment extends TabFragment {
    public static final String TAG = "TabNotificationFragment";

    private int mArticleType;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty_view)
    View mEmptyView;

    private LinearLayoutManager mManager;
    private NotificationListAdapter mAdapter;
    private List<Notification> mNotifications;

    private boolean mClickLoading;

    private void setClickLoading(boolean clickLoading) {
        this.mClickLoading = clickLoading;
    }

    private boolean isClickLoading() {
        return this.mClickLoading;
    }

    public static TabNotificationFragment newInstance(@IntRange(from = ARTICLE_TYPE_ALL, to = ARTICLE_TYPE_TALENT) int articleType) {
        Log.i(TAG, "newInstance");

        if (articleType < ARTICLE_TYPE_ALL || articleType > ARTICLE_TYPE_TALENT)
            throw new IllegalArgumentException("Unexpected article type retrived.");

        TabNotificationFragment fragment = new TabNotificationFragment();

        Bundle args = new Bundle();
        args.putInt(EXTRA_ARTICLE_TYPE, articleType);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        Log.i(TAG, "readBundle");
        super.readBundle(bundle);
        mArticleType = bundle.getInt(EXTRA_ARTICLE_TYPE);
    }

    @Override
    public void onAttach(Context context) {
        Log.i(TAG, "onAttach");
        super.onAttach(context);
    }

    // onCreate ->

    // onCreateView ->

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        // initOnMessageReceiveListener();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        // releaseOnMessageReceiveListener();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "onDestroyView");
        releaseOnMessageReceiveListener();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG, "setUserVisibleHint:" + isVisibleToUser);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_tab_notification, container, false);
        ButterKnife.bind(this, mView);
        initView();
        getNotifications().continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "ERROR:");
                    return null;
                }
                initOnMessageReceiveListener();
                return null;
            }
        });
        return mView;
    }

    private void initOnMessageReceiveListener() {
        Log.i(TAG, "initOnMessageReceiveListener");
        super.startFCMReceiver();
        super.setOnMessageReceiveListener(new OnMessageReceiveListener() {
            final String TAG = "(F)FCMListener";

            @Override
            public void onReceive(RemoteMessage remoteMessage) {
                Log.i(TAG, "onReceive:remoteMessage:" + remoteMessage.getData());

                /*String body = remoteMessage.getNotification().getBody();
                if (body != null) {
                    Log.d(TAG, "onReceive:body: " + body);
                }*/

                // TODO: FIRNotification을 생성해서 adapter의 dataset을 업데이트 해준다.
                Map<String, String> data = remoteMessage.getData();
                if (data.size() > 0) {
                    Log.d(TAG, "onReceive:data: " + remoteMessage.getData());
                }

                int type = Integer.parseInt(data.get("type"));
                if (type == TYPE_CHAT_MESSAGE) return;

                Notification notification = NotificationUtil.parse(data);

                // 판매 탭인데 알림 타입이 판매가 아닐 경우는 리스트에 추가하지 않는다.
                if (mArticleType == ARTICLE_TYPE_TALENT && notification.getType() != TYPE_SHOP) {
                    return;
                }

                mNotifications.add(0, notification);
                mAdapter.notifyItemInserted(0);
                mManager.smoothScrollToPosition(mRecyclerView, null, 0);

                // TODO: 현재는 알림 탭을 확인하지 않더라도 무조건 호출된다. 탭을 이동했을 때만 호출되게 변경한다.
                NotificationUtil.check(notification.getKey()).continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(@NonNull Task<Void> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "onReceive:check:ERROR");
                            return null;
                        }
                        return null;
                    }
                });
            }
        });
    }

    protected void releaseOnMessageReceiveListener() {
        Log.i(TAG, "releaseOnMessageReceiveListener");
        super.stopFCMReceiver();
        super.setOnMessageReceiveListener(null);
    }

    @Override
    protected void initView() {
        super.initView();
        setupRecyclerView();
    }

    @OnClick(R.id.search_window_button)
    void startSearchActivity() {
        Intent in = createIntent(SearchActivity.class);
        startActivity(in);
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

        mNotifications = new ArrayList<>();
        mAdapter = new NotificationListAdapter(getContext(), mNotifications, mRecyclerView);
        mAdapter.setEmptyView(mEmptyView);
        mAdapter.setOnItemClickListener(new NotificationListAdapter.OnItemClickListener() {
            final String TAG = "OnItemClickListener";

            @Override
            public void onItemClick(int position, Notification notification) {
                Log.i(TAG, "onItemClick");

                if (isClickLoading()) return;
                setClickLoading(true);

                String notificationId = notification.getKey();
                int notificationType = notification.getType();

                if (!notification.isChecked()) {
                    NotificationUtil.check(notificationId, mArticleType).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(@NonNull Task<Void> task) throws Exception {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "onItemClick:check:ERROR", task.getException());
                                return null;
                            }
                            Log.d(TAG, "onItemClick:check:SUCCESS");
                            return null;
                        }
                    });
                }

                // TODO: 무조건 postId만을 통해서 ArticleFragment를 불러온다.
                switch (notificationType) {
                    case TYPE_LIKE:
                        startArticleFragment(notification.getPostId());
                        setClickLoading(false);
                        break;
                    case TYPE_COMMENT:
                        startArticleFragment(notification.getPostId());
                        setClickLoading(false);
                        break;
                    case TYPE_FOLLOW:
                        startProfileFragment(notification.getTargetUid());
                        setClickLoading(false);
                        break;
                    case TYPE_MENTION_ARTICLE:
                        startArticleFragment(notification.getPostId());
                        setClickLoading(false);
                        break;
                    case TYPE_MENTION_COMMENT:
                        startArticleFragment(notification.getPostId());
                        setClickLoading(false);
                        break;
                    case TYPE_SHOP:
                        Intent in = ShopActivity.createIntent(getContext(), notification.getShopType(), notification.getOrderId());
                        startActivity(in);
                        setClickLoading(false);
                        break;
                }

            }

            @Override
            public void onItemProfileImageClick(Notification notification) {
                Log.i(TAG, "onItemProfileImageClick");
                startProfileFragment(notification.getTargetUid());
            }

            @Override
            public void onItemPhotoClick(Notification notification) {
                Log.i(TAG, "onItemPhotoClick");
                startArticleFragment(notification.getPostId());
            }
        });
        mAdapter.setOnItemLongClickListener(new NotificationListAdapter.OnItemLongClickListener() {
            final String TAG = "OnItemLongClickListener";

            @Override
            public void onItemLongClick(final int position) {
                Log.i(TAG, "onItemClick:position:" + position);

                if (!BuildConfig.DEBUG) return;

                final Notification notification = mNotifications.get(position);
                Log.d(TAG, "onSelect:delete_notification:comment:" + notification.toMap());

                DialogListFragment dialogFragment = DialogListFragment.newInstance(
                        new String[]{getString(R.string.delete)},
                        new DialogListFragment.OnItemSelectListener() {
                            final String TAG = "OnItemSelectListener";

                            @Override
                            public void onSelect(int index) {
                                Log.i(TAG, "onSelect");

                                final String notificationId = notification.getKey();
                                NotificationUtil.delete(notificationId).continueWith(new Continuation<Void, Void>() {
                                    @Override
                                    public Void then(@NonNull Task<Void> task) throws Exception {
                                        if (!task.isSuccessful()) {
                                            Log.d(TAG, "onSelect:delete_notification:ERROR");
                                            CommonUtil.showSnackbar(getActivity(), getString(R.string.msg_error_failed_delete_comment));
                                            return null;
                                        }
                                        // java.lang.ArrayIndexOutOfBoundsException: length=10; index=-1
                                        final int notificationIndex = mNotifications.indexOf(notification);
                                        mNotifications.remove(notificationIndex);
                                        mAdapter.notifyItemRemoved(notificationIndex);
                                        return null;
                                    }
                                }).continueWith(new Continuation<Void, Void>() {
                                    @Override
                                    public Void then(@NonNull Task<Void> task) throws Exception {
                                        if (!task.isSuccessful()) {
                                            Log.w(TAG, "onSelect:delete_comment:ERROR:", task.getException());
                                        }
                                        return null;
                                    }
                                });
                            }
                        }
                );
                dialogFragment.show(getFragmentManager(), null);
            }
        });
        mAdapter.setOnLoadMoreListener(new NotificationListAdapter.OnLoadMoreListener() {
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
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    private void startArticleFragment(String postId) {
        ArticleFragment articleFragment = ArticleFragment.newInstance(postId);
        ((PageNotificationFragment) getParentFragment()).getRootFragment().add(articleFragment);
    }

    private void startArticleFragment(Post post) {
        ArticleFragment articleFragment = ArticleFragment.newInstance(post);
        ((PageNotificationFragment) getParentFragment()).getRootFragment().add(articleFragment);
    }

    private void startProfileFragment(String userId) {
        ProfileFragment profileFragment = ProfileFragment.newInstance(userId);
        ((PageNotificationFragment) getParentFragment()).getRootFragment().add(profileFragment);
    }

    private Task<Void> getNotifications() {
        Log.i(TAG, "getNotifications");

        mAdapter.setLoadingDataset(true);

        String maxKey = null;
        if (!mNotifications.isEmpty())
            maxKey = mNotifications.get(mNotifications.size() - 1).getKey();

        return NotificationUtil.getNotifications(mArticleType, COUNT_NOTIFICATION, maxKey).continueWith(new Continuation<List<Notification>, Void>() {
            @Override
            public Void then(@NonNull Task<List<Notification>> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getNotifications:ERROR");
                    throw task.getException();
                }

                mAdapter.setLoadingDataset(false);

                // TODO: 여기서 loading를 삭제해버리면 삭제된 직후 아이템이 insert 되기 때문에 살짝 깜빡거리는 것처럼 보이는 이슈가 있다.
                mAdapter.hideLoading();

                List<Notification> notifications = task.getResult();

                if (notifications.size() < COUNT_NOTIFICATION) mAdapter.setLastDataReached(true);

                if (mNotifications.isEmpty() && notifications.isEmpty()) {
                    Log.d(TAG, "getNotifications:current_dataset_and_new_dataset_are_empty");
                    mAdapter.notifyDataSetChanged();
                }

                for (Notification notification : notifications) {
                    Log.d(TAG, "getNotifications:notification:" + notification.toMap());

                    // 임시 패치(이렇게 하면 과거의 알림은 모두 불러올 수가 없다)
                    // if (notification.getMessage() == null) continue;

                    mNotifications.add(notification);
                    int currentIndex = mNotifications.indexOf(notification);
                    mAdapter.notifyItemInserted(currentIndex);
                }

                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getNotifications:ERROR:" + task.getException());
                    throw task.getException();
                }
                Log.d(TAG, "getNotifications:SUCCESS");
                return null;
            }
        });
    }
}