package com.chaigene.petnolja.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import com.chaigene.petnolja.ui.activity.OldMainActivity;
import com.chaigene.petnolja.util.OldArticleUtil;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.ArticleGridAdapter;
import com.chaigene.petnolja.event.WriteArticleEvent;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.view.GridSpaceItemDecoration;
import com.chaigene.petnolja.util.CommonUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.ARTICLE_SCOPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_SCOPE_PROFILE;
import static com.chaigene.petnolja.Constants.ARTICLE_SCOPE_SAVE;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;
import static com.chaigene.petnolja.Constants.EXTRA_ARTICLE_SCOPE;
import static com.chaigene.petnolja.Constants.EXTRA_ARTICLE_TYPE;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_USER_ID;
import static com.chaigene.petnolja.Constants.PAGE_EXPLORE;

public class TabArticleFragment extends TabFragment {
    public static final String TAG = "TabArticleFragment";

    private int mArticleScope;
    private int mArticleType;
    private String mTargetUid;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty_view)
    View mEmptyView;

    @BindView(R.id.empty_label_text)
    TextView mTvEmptyLabel;

    @BindView(R.id.empty_guide_button)
    Button mBtnEmptyGuide;

    private GridLayoutManager mManager;
    private ArticleGridAdapter mAdapter;
    private int mGridItemCount;

    private List<Post> mPosts;
    private boolean mClickLoading;

    private void setClickLoading(boolean clickLoading) {
        this.mClickLoading = clickLoading;
    }

    private boolean isClickLoading() {
        return this.mClickLoading;
    }

    public static TabArticleFragment newInstance(@IntRange(from = ARTICLE_SCOPE_ALL, to = ARTICLE_SCOPE_SAVE) int articleScope,
                                                 @IntRange(from = ARTICLE_TYPE_ALL, to = ARTICLE_TYPE_TALENT) int articleType,
                                                 @Nullable String targetUid) {
        // Log.i(TAG, "newInstance");

        if (articleScope != ARTICLE_SCOPE_ALL && articleScope != ARTICLE_SCOPE_PROFILE && articleScope != ARTICLE_SCOPE_SAVE)
            throw new IllegalArgumentException("Unexpected article scope retrived.");

        if (articleScope == ARTICLE_SCOPE_PROFILE && targetUid == null)
            throw new IllegalArgumentException("Profile scope needs specific target UID.");

        if (articleType != ARTICLE_TYPE_ALL && articleType != ARTICLE_TYPE_FEED && articleType != ARTICLE_TYPE_TALENT)
            throw new IllegalArgumentException("Unexpected article type retrived.");

        TabArticleFragment fragment = new TabArticleFragment();

        Bundle args = new Bundle();
        args.putInt(EXTRA_ARTICLE_SCOPE, articleScope);
        args.putInt(EXTRA_ARTICLE_TYPE, articleType);
        args.putString(EXTRA_TARGET_USER_ID, targetUid);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        Log.i(TAG, "readBundle");
        mArticleScope = bundle.getInt(EXTRA_ARTICLE_SCOPE);
        mArticleType = bundle.getInt(EXTRA_ARTICLE_TYPE);
        mTargetUid = bundle.getString(EXTRA_TARGET_USER_ID);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
    }

    // onCreate ->

    // onCreateView ->

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Log.i(TAG, "onActivityCreated");
    }

    @Override
    public void onStart() {
        // Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Log.i(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        // Log.i(TAG, "onPause");
    }

    @Override
    public void onStop() {
        // Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Log.i(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Log.i(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Log.i(TAG, "onDetach");
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // Log.i(TAG, "setUserVisibleHint:" + isVisibleToUser);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_tab_article, container, false);
        ButterKnife.bind(this, mView);
        initView();

        showLoadingDialog();
        generateGridItemCount().continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                return getArticles(true);
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "asyncTask:getArticles:ERROR", task.getException());
                    return null;
                }
                // Log.d(TAG, "getArticles:SUCCESS");
                return null;
            }
        });
        return mView;
    }

    @Override
    public void onRefresh() {
        Log.i(TAG, "onRefresh");
        super.onRefresh();
        mPosts.clear();
        mAdapter.notifyDataSetChanged();
        showLoadingDialog();
        getArticles(true).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "ERROR:");
                }
                return null;
            }
        });
    }

    @Override
    protected void initView() {
        super.initView();
        setupEmptyView();
        setupRecyclerView();
    }

    private void setupEmptyView() {
        if (mArticleScope == ARTICLE_SCOPE_PROFILE) {
            mTvEmptyLabel.setText("당신만의 메이커 공간을\n꾸며보세요.");
            mBtnEmptyGuide.setText("메이커 글쓰기");
        }
        if (mArticleScope == ARTICLE_SCOPE_SAVE) {
            mTvEmptyLabel.setText("마음에 드는 글을\n담아보세요.");
            mBtnEmptyGuide.setText("담으러 가기");
        }
    }

    @OnClick(R.id.empty_guide_button)
    void showWriteTab() {
        if (mArticleScope == ARTICLE_SCOPE_PROFILE) {
            EventBus.getDefault().post(new WriteArticleEvent());
        }
        if (mArticleScope == ARTICLE_SCOPE_SAVE) {
            ((OldMainActivity) getActivity()).getViewPager().setCurrentItem(PAGE_EXPLORE);
        }
    }

    private Task<Void> generateGridItemCount() {
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mGridItemCount = CommonUtil.getGridItemCount(getContext(), mRecyclerView.getHeight());
                tcs.setResult(null);
            }
        });
        return tcs.getTask();
    }

    private void setupRecyclerView() {
        // Log.i(TAG, "setupRecyclerView");

        mManager = new GridLayoutManager(getContext(), CommonUtil.getGridSpanCount(getContext()));
        // mManager.setAutoMeasureEnabled(true);
        // mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setLayoutManager(mManager);

        GridSpaceItemDecoration gridSpaceItemDecoration = new GridSpaceItemDecoration(3, CommonUtil.dpToPx(getContext(), 2));
        mRecyclerView.addItemDecoration(gridSpaceItemDecoration);

        // Source: http://stackoverflow.com/a/28828749/4729203
        mRecyclerView.setHasFixedSize(true);
        // NestedScrollView에 감싸여져 있다면 false로 설정 해야한다.
        // mRecyclerView.setNestedScrollingEnabled(false);
        // mRecyclerView.setNestedScrollingEnabled(true);

        // Divider
        // DividerItemDecoration divider = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        // divider.setDrawable(CommonUtil.getDrawable(getContext(), R.drawable.shape_divider_notification));
        // mRecyclerView.addItemDecoration(divider);

        mPosts = new ArrayList<>();
        mAdapter = new ArticleGridAdapter(getContext(), mArticleType, mPosts, mRecyclerView);
        if (mArticleScope == ARTICLE_SCOPE_PROFILE && mTargetUid.equals(AuthManager.getUserId())) {
            mAdapter.setEmptyView(mEmptyView);
        }
        if (mArticleScope == ARTICLE_SCOPE_SAVE) {
            mAdapter.setEmptyView(mEmptyView);
        }
        mAdapter.setOnItemClickListener(new ArticleGridAdapter.OnItemClickListener() {
            final String TAG = "OnItemClickListener";

            @Override
            public void onItemClick(int position, Post post) {
                Log.i(TAG, "onItemClick");

                // if (isClickLoading()) return;
                // setClickLoading(true);

                startArticleFragment(post);
            }
        });
        mAdapter.setOnLoadMoreListener(new ArticleGridAdapter.OnLoadMoreListener() {
            final String TAG = "OnLoadMoreListener";

            @Override
            public void onLoadMore() {
                Log.i(TAG, "onLoadMore");

                // 로딩을 보여준다.
                getArticles(false).continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(@NonNull Task<Void> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "onLoadMore:getArticles:ERROR");
                        }
                        mAdapter.setLoading(false);
                        return null;
                    }
                });
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    // TODO: 뷰페이저 내의 프래그먼트에서 호출하려면 child fragment manager를 가져올 수 있어야 한다.
    // 액티비티 내에서 가져오는 거라면 support fragment manager를 가져와야 한다.
    // 액티비티에서 가져올 때는 getParentFragment가 null을 반환한다.
    private void startArticleFragment(Post post) {
        ArticleFragment articleFragment = ArticleFragment.newInstance(post);
        startFragment(articleFragment);
    }

    private Task<Void> getArticles(boolean isRefresh) {
        Log.i(TAG, "getArticles");

        mAdapter.setLoadingDataset(true);

        if (isRefresh) {
            mPosts.clear();
            mAdapter.notifyDataSetChanged();
        }

        String maxKey = null;
        if (!mPosts.isEmpty()) maxKey = mPosts.get(mPosts.size() - 1).getKey();

        Task<List<Post>> getTask = null;
        if (mArticleScope == ARTICLE_SCOPE_ALL) {
            getTask = OldArticleUtil.getPosts(mArticleType, mGridItemCount, maxKey);
        } else if (mArticleScope == ARTICLE_SCOPE_PROFILE) {
            getTask = OldArticleUtil.getUserPosts(mTargetUid, mArticleType, mGridItemCount, maxKey);
        } else if (mArticleScope == ARTICLE_SCOPE_SAVE) {
            getTask = OldArticleUtil.getUserSaves(mTargetUid, mGridItemCount, maxKey);
        }

        return getTask.continueWith(new Continuation<List<Post>, Void>() {
            @Override
            public Void then(@NonNull Task<List<Post>> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getArticles:ERROR", task.getException());
                    throw task.getException();
                }

                mAdapter.setLoadingDataset(false);

                // TODO: 여기서 loading를 삭제해버리면 삭제된 직후 아이템이 insert 되기 때문에 살짝 깜빡거리는 것처럼 보이는 이슈가 있다.
                // mAdapter.hideLoading();

                List<Post> posts = task.getResult();

                int count = 0;
                if (mArticleScope == ARTICLE_SCOPE_ALL) {
                    count = mGridItemCount;
                } else if (mArticleScope == ARTICLE_SCOPE_PROFILE) {
                    count = mGridItemCount;
                } else if (mArticleScope == ARTICLE_SCOPE_SAVE) {
                    count = mGridItemCount;
                }

                if (posts.size() < count) mAdapter.setLastDataReached(true);

                if (mPosts.isEmpty() && posts.isEmpty()) {
                    Log.d(TAG, "getArticles:current_dataset_and_new_dataset_are_empty");
                    mAdapter.notifyDataSetChanged();
                }

                Log.d(TAG, "getArticles:posts:size:" + posts.size());
                for (Post post : posts) {
                    // Log.d(TAG, "getArticleset:loop:index:" + index + "|post:" + post.toMap());
                    mPosts.add(post);
                    int index = mPosts.indexOf(post);
                    mAdapter.notifyItemInserted(index);
                }
                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getArticles:ERROR", task.getException());
                    throw task.getException();
                }
                Log.d(TAG, "getArticles:SUCCESS");
                return null;
            }
        });
    }
}