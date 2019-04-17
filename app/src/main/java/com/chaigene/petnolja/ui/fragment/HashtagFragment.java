package com.chaigene.petnolja.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.chaigene.petnolja.util.OldArticleUtil;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.ArticleGridAdapter;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.view.GridSpaceItemDecoration;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_HASHTAG;

// TODO: 번들을 통해 "HashTag" 스트링 값을 전달 받아야 한다.
public class HashtagFragment extends ChildFragment {
    public static final String TAG = "HashtagFragment";

    @BindView(R.id.result_text)
    TextView mTvResultText;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private GridLayoutManager mManager;
    private ArticleGridAdapter mAdapter;
    private int mGridItemCount;

    private String mHashTag;
    private int mHashtagCount;
    private List<Post> mPosts;

    public static HashtagFragment newInstance(@NonNull Bundle extras) {
        String hashtag = extras.getString(EXTRA_TARGET_HASHTAG);
        return newInstance(hashtag);
    }

    public static HashtagFragment newInstance(@NonNull String hashTag) {
        HashtagFragment fragment = new HashtagFragment();

        Bundle args = new Bundle();
        args.putString(Constants.EXTRA_TARGET_HASHTAG, hashTag);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);

        mHashTag = bundle.getString(Constants.EXTRA_TARGET_HASHTAG);
        Log.d(TAG, "readBundle:hashTag:" + mHashTag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_hashtag, container, false);
        ButterKnife.bind(this, mView);

        // initView();
        /*getArticles(true).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                return null;
            }
        });*/

        setupRecyclerView();
        showLoadingDialog();
        generateGridItemCount().continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                return asyncTask();
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "asyncTask:getArticles:ERROR:" + task.getException().getMessage());
                    return null;
                }
                // Log.d(TAG, "getArticles:SUCCESS");
                initView();
                return null;
            }
        });

        return mView;
    }

    private Task<Void> asyncTask() {
        Log.i(TAG, "asyncTask");
        DatabaseReference hashtagCountRef = DatabaseManager.getArticleHashtagCountRef().child(mHashTag);
        final Task<Integer> hashtagCountTask = DatabaseManager.getInteger(hashtagCountRef);
        final Task<Void> getArticlesTask = getArticles(true);
        return Tasks.whenAll(hashtagCountTask, getArticlesTask).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!hashtagCountTask.isSuccessful()) {
                    Log.w(TAG, "asyncTask:hashtagCountTask:ERROR:" + hashtagCountTask.getException().getMessage());
                    throw hashtagCountTask.getException();
                }
                mHashtagCount = hashtagCountTask.getResult();
                if (!getArticlesTask.isSuccessful()) {
                    Log.w(TAG, "asyncTask:getArticlesTask:ERROR:" + getArticlesTask.getException().getMessage());
                    throw getArticlesTask.getException();
                }
                return null;
            }
        });
        /*return Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                DatabaseReference hashtagCountRef = DatabaseManager.getArticleHashtagCountRef().child(mHashTag);
                Task<Integer> hashtagCountTask = DatabaseManager.getInteger(hashtagCountRef);
                mHashtagCount = Tasks.await(hashtagCountTask);
                if (!hashtagCountTask.isSuccessful()) {
                    Log.w(TAG, "hashtagCountTask:ERROR:" + hashtagCountTask.getException().getMessage());
                    throw hashtagCountTask.getException();
                }

                Task<Void> getArticlesTask = getArticles(true);
                Tasks.await(getArticlesTask);
                if (!getArticlesTask.isSuccessful()) {
                    Log.w(TAG, "getArticlesTask:ERROR:" + getArticlesTask.getException().getMessage());
                    throw getArticlesTask.getException();
                }

                return null;
            }
        });*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // showToolbarIcon();
    }

    @Override
    protected void initView() {
        super.initView();
        mTvResultText.setText(getString(R.string.label_search_result, mHashTag, mHashtagCount));
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        // TODO: 어떤 해쉬태그를 위한 화면인지는 어떻게 보여줄 것인가?
        // 툴바를 통해서 보여줄 것인가?
        // 인스타그램을 참조해서 만든다.
        // 일단은 툴바 타이틀로 보여주기로 하자.
        setToolbarTitle(CommonUtil.format("#%s", mHashTag));
        setToolbarTitleAlign(Gravity.START);
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
        Log.i(TAG, "setupRecyclerView");

        mManager = new GridLayoutManager(getContext(), CommonUtil.getGridSpanCount(getContext()));
        // mManager.setAutoMeasureEnabled(true);
        mRecyclerView.setLayoutManager(mManager);

        // mRecyclerView.setPadding(2, 2, 2, 2);

        GridSpaceItemDecoration gridSpaceItemDecoration = new GridSpaceItemDecoration(3, CommonUtil.dpToPx(getContext(), 2));
        mRecyclerView.addItemDecoration(gridSpaceItemDecoration);

        // Source: http://stackoverflow.com/a/28828749/4729203
        mRecyclerView.setHasFixedSize(true);

        // 매끄러운 스크롤을 위해서 설정해야 한다.
        // mRecyclerView.setNestedScrollingEnabled(false);

        // false로 하게 되면 하단 부분이 짤리게 된다.
        // mRecyclerView.setNestedScrollingEnabled(true);

        mPosts = new ArrayList<>();
        mAdapter = new ArticleGridAdapter(getContext(), ARTICLE_TYPE_ALL, mPosts, mRecyclerView);
        mAdapter.setOnItemClickListener(new ArticleGridAdapter.OnItemClickListener() {
            final String TAG = "OnItemClickListener";

            @Override
            public void onItemClick(int position, Post post) {
                Log.i(TAG, "onItemClick");
                // if (isClickLoading()) return;
                // setClickLoading(true);
                // startArticleFragment(post);

                String postId = post.getKey();
                startArticleFragment(postId);
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
    private void startArticleFragment(String postId) {
        ArticleFragment articleFragment = ArticleFragment.newInstance(postId);
        getRootFragment().add(articleFragment);
    }

    private Task<Void> getArticles(boolean isRefresh) {
        Log.i(TAG, "getArticles");

        if (isRefresh) {
            mPosts.clear();
            mAdapter.notifyDataSetChanged();
        }

        String maxKey = null;
        if (!mPosts.isEmpty()) maxKey = mPosts.get(mPosts.size() - 1).getKey();

        return OldArticleUtil.getHashtagPosts(mHashTag, mGridItemCount, maxKey).continueWith(new Continuation<List<Post>, Void>() {
            @Override
            public Void then(@NonNull Task<List<Post>> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getArticles:getTimelinePosts:ERROR", task.getException());
                    throw task.getException();
                }

                // TODO: 여기서 loading를 삭제해버리면 삭제된 직후 아이템이 insert 되기 때문에 살짝 깜빡거리는 것처럼 보이는 이슈가 있다.
                // mAdapter.hideLoading();

                List<Post> posts = task.getResult();

                if (posts.size() < mGridItemCount) mAdapter.setLastDataReached(true);

                for (Post post : posts) {
                    Log.d(TAG, "getArticles:post:" + post.toMap());
                    mPosts.add(post);
                    final int currentIndex = mPosts.indexOf(post);
                    mAdapter.notifyItemInserted(currentIndex);
                }
                return null;
            }
        });
    }

    // 일단 메뉴가 없다고 가정.
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // inflater.inflate(R.menu.menu_fragment_article, menu);
        // TODO: 네비게이션 버튼 클릭했을 때 finish 해준다.
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

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.i(TAG, "onPrepareOptionsMenu");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*int id = item.getItemId();
        switch (id) {
            case R.id.action_login: {
                return true;
            }
        }*/
        return super.onOptionsItemSelected(item);
    }

    private void finish() {
        Log.i(TAG, "finish");
        getActivity().onBackPressed();
        // getRootFragment().pop();
        // getActivity().getSupportFragmentManager().popBackStack();
        // getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }
}