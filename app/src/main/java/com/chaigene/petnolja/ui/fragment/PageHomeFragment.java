package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chaigene.petnolja.model.User;
import com.chaigene.petnolja.ui.dialog.DialogConfirmFragment;
import com.chaigene.petnolja.util.OldArticleUtil;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.ArticleListAdapter;
import com.chaigene.petnolja.event.ChatBadgeEvent;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.model.Comment;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.activity.ChatActivity;
import com.chaigene.petnolja.ui.activity.ChatRoomActivity;
import com.chaigene.petnolja.ui.activity.SearchActivity;
import com.chaigene.petnolja.ui.activity.ShopActivity;
import com.chaigene.petnolja.ui.activity.WriteActivity;
import com.chaigene.petnolja.util.AbuseUtil;
import com.chaigene.petnolja.util.ChatUtil;
import com.chaigene.petnolja.util.CommentUtil;
import com.chaigene.petnolja.util.CommonUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.ARTICLE_FRAGMENT;
import static com.chaigene.petnolja.Constants.CHAT_ACTIVITY;
import static com.chaigene.petnolja.Constants.COUNT_ARTICLE_TIMELINE;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_POST_ID;
import static com.chaigene.petnolja.Constants.WRITE_ACTIVITY;

// TODO: ArticleActivity에서 해당 사용자 프로필에 진입할 경우
// 액티비티를 종료하고 프래그먼트로 스택을 쌓아야 한다.
public class PageHomeFragment extends ChildFragment {
    public static final String TAG = "PageHomeFragment";

    @BindView(R.id.container)
    SwipeRefreshLayout mRefreshContainer;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty_view)
    View mEmptyView;

    private LinearLayoutManager mManager;
    private ArticleListAdapter mAdapter;
    private List<Post> mPosts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mView = inflater.inflate(R.layout.page_fragment_home, container, false);
        ButterKnife.bind(this, mView);

        // For debugging.
        FirebaseDatabase.getInstance().getReference(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue(Boolean.class)) {
                    Log.d(TAG, "Firebase CONNECTED");
                } else {
                    Log.d(TAG, "Firebase NOT CONNECTED");
                    // Ref: https://firebase.google.com/docs/reference/android/com/google/firebase/database/FirebaseDatabase.html#purgeOutstandingWrites()
                    FirebaseDatabase.getInstance().purgeOutstandingWrites();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "onCancelled: ", error.toException());
            }
        });

        initView();
        asyncTask();

        mRefreshContainer.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    final String TAG = "OnRefreshListener";

                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh");
                        refresh();
                    }
                }
        );
        return mView;
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        Log.i(TAG, "setupToolbar");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "onDestroyView");
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
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.i(TAG, "onHiddenChanged:hidden:" + hidden);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG, "setUserVisibleHint:isVisibleToUser:" + isVisibleToUser);
    }

    /*@Override
    protected void onBackStackChanged() {
        super.onBackStackChanged();
        Log.i(TAG, "onBackStackChanged");
    }*/

    private void asyncTask() {
        Log.i(TAG, "asyncTask");
        showLoadingDialog();
        getArticles(true).continueWith((Continuation<Void, Void>) task -> {
            dismissDialog();
            if (!task.isSuccessful()) {
                Log.w(TAG, "asyncTask:getArticles:ERROR", task.getException());
                return null;
            }
            // Log.d(TAG, "getArticles:SUCCESS");
            setInitializedAsyncTask(true);
            return null;
        });
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
        mManager = new LinearLayoutManager(getContext().getApplicationContext());
        mRecyclerView.setLayoutManager(mManager);

        mPosts = new ArrayList<>();
        mAdapter = new ArticleListAdapter(getContext(), mPosts, mRecyclerView);
        mAdapter.setEmptyView(mEmptyView);
        mAdapter.setOnItemClickListener(new ArticleListAdapter.OnItemClickListener() {
            final String TAG = "OnItemClickListener";

            @Override
            public void onItemClick(View view, int position) {
                Log.i(TAG, "onItemClick:position:" + position);
                // Post post = mPosts.getUserPosts(position);
                // startArticleFragment(post);
            }

            @Override
            public void onItemNicknameClick(String uid, int position) {
                Log.i(TAG, "onItemNicknameClick:uid:" + uid + "|position:" + position);
                startProfileFragmentByUserId(uid);
            }

            @Override
            public void onItemFollowButtonClick(View view, final int position) {
                Log.i(TAG, "onItemFollowButtonClick:position:" + position);

                // TODO: 이미 팔로우 중입니다. / 이미 언팔로우 하셨습니다.
                // 대상 유저와의 follow 관계는 어디에 저장하고 있어야 할까? => 로컬에 저장하지 않고 실시간으로 체크한다.
                // 하지만 RecyclerView에서 보여주려면 값이 저장되어 있어야 한다.
                // OldUser 객체에 담아주자.
                // 저장해주는 시점은 리스트를 불러올 때 비동기로 follow 테이블 값을 가져온다.
                // 하지만 이렇게 되면 불필요한 반복적인 네트워킹이 발생할 가능성이 크다.
                // 메모리 상에 해당 유저와의 팔로잉 관게를 Map 형태로 저장해두고
                // 특정 값이 없을 경우 네트워크에서 불러오고 저장하는 방식이 필요하다.
                // UserUtil.follow();

                // TODO: 버튼을 토글해주어야 한다.
                /*ListDialogFragment dialogFragment = ListDialogFragment.newInstance(
                        new String[]{"팔로우 하기", "취소"},
                        new ListDialogFragment.OnItemSelectListener() {
                            @Override
                            public void onSelect(int index) {
                                if (index == 0) {
                                    OldUser user = mPosts.getUserPosts(position).getOldUser();
                                    String uid = user.getId();
                                    boolean isFollowing = user.isFollowing();
                                    if (isFollowing) {
                                        UserUtil.unfollow(uid).continueWith(new Continuation<Void, Void>() {
                                            @Override
                                            public Void then(@NonNull Task<Void> task) throws Exception {
                                                return null;
                                            }
                                        });
                                    } else {
                                        UserUtil.follow(uid).continueWith(new Continuation<Void, Void>() {
                                            @Override
                                            public Void then(@NonNull Task<Void> task) throws Exception {
                                                return null;
                                            }
                                        });
                                    }
                                }
                                if (index == 1) {
                                    // TODO: 취소
                                }
                            }
                        }
                );
                dialogFragment.show(getFragmentManager(), null);*/

                // FIXME
                // updateFollow(view, position);
            }

            @Override
            public void onItemPhotoClick(View view, int position) {
                Log.i(TAG, "onItemPhotoClick:position:" + position);
                Post post = mPosts.get(position);
                startArticleFragment(post);
            }

            @Override
            public void onItemLikeButtonClick(ArticleListAdapter.ArticleViewHolder viewHolder, Post post) {
                int position = mPosts.indexOf(post);
                Log.i(TAG, "onItemLikeButtonClick:position:" + position);
                updateLike(viewHolder, post);
            }

            @Override
            public void onItemSaveButtonClick(ArticleListAdapter.ArticleViewHolder viewHolder, Post post) {
                int position = mPosts.indexOf(post);
                Log.i(TAG, "onItemSaveButtonClick:position:" + position);
                updateSave(viewHolder, post);
            }

            @Override
            public void onItemCommentButtonClick(View view, int position) {
                Log.i(TAG, "onItemCommentButtonClick:position:" + position);
                Post post = mPosts.get(position);
                startArticleFragment(post);
            }

            @Override
            public void onItemReportButtonClick(Post post) {
                Log.i(TAG, "onItemReportButtonClick:post:" + (post != null ? post.toMap() : null));
                final String postId = post.getKey();
                final String targetUid = post.getUser().getId();
                final String targetNickname = post.getUser().getNickname();
                DialogConfirmFragment dialogConfirmFragment = DialogConfirmFragment.newInstance(
                        "해당 게시물을 신고하시겠습니까?",
                        new DialogConfirmFragment.OnSelectListener() {
                            @Override
                            public void onConfirm() {
                                super.onConfirm();
                                showLoadingDialog();
                                AbuseUtil.reportArticle(postId, targetUid, targetNickname).continueWith((Continuation<Void, Void>) task -> {
                                    dismissDialog();
                                    if (!task.isSuccessful()) {
                                        Log.w(TAG, "onItemReportButtonClick:reportArticle:ERROR:" + task.getException().getMessage());
                                        CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.");
                                        return null;
                                    }
                                    CommonUtil.showSnackbar(getActivity(), "신고해주셔서 감사합니다. 판다팀에서 검토 뒤 적절한 제재를 취하겠습니다.");
                                    return null;
                                });

                            }

                            @Override
                            public void onDeny() {
                                super.onDeny();
                            }
                        }
                );
                dialogConfirmFragment.show(getFragmentManager(), null);
            }

            @Override
            public void onItemMessageButtonClick(View view, int position) {
                Log.i(TAG, "onItemMessageButtonClick:position:" + position);
                // ChatRoomActivity.

                // TODO: ChatActivity를 바로 시작해야한다.
                // Request code 혹은 Result code는 필요없다?
                // Request code를 전달해서 챗방에서 나올 때 ChatRoomActivity를 보여주는 것도 괜찮을 것 같다.
                // 일단은 리턴 코드 없이 시작해보자.

                User targetUser = mPosts.get(position).getUser();
                Log.d(TAG, "onItemMessageButtonClick:targetUser:" + targetUser.toMap());
                Intent intent = ChatActivity.createIntent(getContext(), targetUser);
                // intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(intent, CHAT_ACTIVITY);
                // getActivity().overridePendingTransition(0, 0);
            }

            @Override
            public void onItemModifyButtonClick(View view, int position) {
                Log.i(TAG, "onItemModifyButtonClick:position:" + position);
                // 글을 수정한다.
                String postId = mPosts.get(position).getKey();
                Intent in = WriteActivity.createModifyIntent(getContext(), postId);
                startActivityForResult(in, WRITE_ACTIVITY);
            }

            @Override
            public void onItemDeleteButtonClick(View view, final int position) {
                Log.i(TAG, "onItemDeleteButtonClick:position:" + position);
                DialogConfirmFragment deleteDialog = DialogConfirmFragment.newInstance(
                        "삭제하시겠습니까?",
                        new DialogConfirmFragment.OnSelectListener() {
                            @Override
                            public void onConfirm() {
                                super.onConfirm();
                                /*deleteArticle(mPosts.get(position).getKey()).continueWithTask(new Continuation<Void, Task<Void>>() {
                                    @Override
                                    public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                                        mPosts.remove(position);
                                        mAdapter.notifyItemRemoved(position);
                                        return null;
                                    }
                                });*/
                                deleteArticle(mPosts.get(position)).continueWithTask((Continuation<Void, Task<Void>>) task -> {
                                    mPosts.remove(position);
                                    mAdapter.notifyItemRemoved(position);
                                    CommonUtil.showSnackbar(getActivity(), "글이 삭제되었습니다.");
                                    return null;
                                });
                            }

                            @Override
                            public void onDeny() {
                                super.onDeny();
                            }
                        }
                );
                deleteDialog.show(getFragmentManager(), null);
            }

            @Override
            public void onItemHashtagClick(String hashtag) {
                Log.i(TAG, "onItemHashtagClick:hashtag:" + hashtag);
                startHashtagFragment(hashtag);
            }

            @Override
            public void onItemAtSignClick(String atSign) {
                Log.i(TAG, "onItemHashtagClick:atSign:" + atSign);
                startProfileFragmentByNickname(atSign);
            }
        });
        mAdapter.setOnLoadMoreListener(new ArticleListAdapter.OnLoadMoreListener() {
            final String TAG = "OnLoadMoreListener";

            @Override
            public void onLoadMore() {
                Log.i(TAG, "onLoadMore");

                // 로딩을 보여준다.
                getArticles(false).continueWith((Continuation<Void, Void>) task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "onLoadMore:getNotifications:ERROR");
                    }
                    mAdapter.setLoading(false);
                    return null;
                });
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    void updateLike(final ArticleListAdapter.ArticleViewHolder viewHolder, final Post post) {
        Log.i(TAG, "updateLike");
        viewHolder.ivLikeBtn.setEnabled(false);
        final boolean isActivated = viewHolder.ivLikeBtn.isActivated();
        viewHolder.ivLikeBtn.setActivated(!isActivated);
        final String targetPostId = post.getKey();
        final int oldlikeCount = post.getLikeCount();
        int newLikeCount = !isActivated ? oldlikeCount + 1 : oldlikeCount - 1 > 0 ? oldlikeCount - 1 : 0;
        Log.d(TAG, "updateLike:newLikeCount:" + newLikeCount);
        viewHolder.tvLikeCount.setText(String.valueOf(newLikeCount));
        Task<Integer> likeTask = !isActivated ? OldArticleUtil.like(targetPostId) : OldArticleUtil.unlike(targetPostId);
        likeTask.continueWith((Continuation<Integer, Void>) task -> {
            viewHolder.ivLikeBtn.setEnabled(true);
            if (!task.isSuccessful()) {
                Log.w(TAG, "updateLike:ERROR:" + task.getException().getMessage());
                viewHolder.ivLikeBtn.setActivated(isActivated);
                viewHolder.tvLikeCount.setText(String.valueOf(oldlikeCount));
                return null;
            }
            Log.d(TAG, "updateLike:isActivated:" + viewHolder.ivLikeBtn.isActivated());
            String myUid = AuthManager.getUserId();
            if (post.getLikes().containsKey(myUid)) {
                post.getLikes().remove(myUid);
            } else {
                post.getLikes().put(myUid, true);
            }
            int likeCount = task.getResult();
            viewHolder.tvLikeCount.setText(String.valueOf(likeCount));
            post.setLikeCount(likeCount);
            viewHolder.tvLikeCount.setText(String.valueOf(likeCount));
            // 일단은 어댑터 데이터셋을 업데이트 해주기는 해야할 것 같다. (해당 cachedPost가 존재할 때만)
            /*Post cachedPost = OldArticleUtil.getInstance().loadCachedPost(targetPostId);
            Log.d(TAG, "updateLike:cachedPost:" + (cachedPost != null ? cachedPost.toMap() : null));
            if (cachedPost == null) return null;
            int index = mPosts.indexOf(targetPost);
            mPosts.set(index, cachedPost);*/
            return null;
        });
    }

    void updateSave(final ArticleListAdapter.ArticleViewHolder viewHolder, final Post post) {
        Log.i(TAG, "updateSave");
        viewHolder.ivSaveBtn.setEnabled(false);
        final boolean isActivated = viewHolder.ivSaveBtn.isActivated();
        viewHolder.ivSaveBtn.setActivated(!isActivated);
        final String targetPostId = post.getKey();
        final int oldSaveCount = post.getSaveCount();
        int newSaveCount = !isActivated ? oldSaveCount + 1 : oldSaveCount - 1 > 0 ? oldSaveCount - 1 : 0;
        Log.d(TAG, "updateSave:newSaveCount:" + newSaveCount);
        viewHolder.tvSaveCount.setText(String.valueOf(newSaveCount));
        Task<Integer> saveTask = !isActivated ? OldArticleUtil.save(targetPostId) : OldArticleUtil.unsave(targetPostId);
        saveTask.continueWith((Continuation<Integer, Void>) task -> {
            viewHolder.ivSaveBtn.setEnabled(true);
            if (!task.isSuccessful()) {
                Log.w(TAG, "updateSave:ERROR:" + task.getException().getMessage());
                viewHolder.ivSaveBtn.setActivated(isActivated);
                viewHolder.tvSaveCount.setText(String.valueOf(oldSaveCount));
                return null;
            }
            Log.d(TAG, "updateSave:isActivated:" + viewHolder.ivSaveBtn.isActivated());
            String myUid = AuthManager.getUserId();
            if (post.getSaves().containsKey(myUid)) {
                post.getSaves().remove(myUid);
            } else {
                post.getSaves().put(myUid, true);
            }
            int saveCount = task.getResult();
            viewHolder.tvSaveCount.setText(String.valueOf(saveCount));
            post.setSaveCount(saveCount);
            viewHolder.tvSaveCount.setText(String.valueOf(saveCount));
            return null;
        });
    }

    // FIXME
    /*private void updateFollow(final View view, int index) {
        view.setEnabled(false);
        showLoadingDialog();
        String targetUid = mPosts.get(index).getUser().getId();
        Task<Void> followTask;
        final boolean isActivated = view.isActivated();
        if (!isActivated) {
            followTask = UserUtil.follow(targetUid);
        } else {
            followTask = UserUtil.unfollow(targetUid);
        }
        followTask.continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "follow:ERROR");
                    dismissDialog();
                    view.setEnabled(true);
                    return null;
                }
                dismissDialog();
                view.setActivated(!isActivated);
                view.setEnabled(true);

                Log.d(TAG, "updateFollow:isActivated:" + view.isActivated());

                return null;
            }
        });
    }*/

    private Task<Void> getArticles(boolean isRefresh) {
        Log.i(TAG, "getArticles");

        mAdapter.setLoadingDataset(true);

        if (isRefresh) {
            mPosts.clear();
            mAdapter.notifyDataSetChanged();
        }

        String maxKey = null;
        if (!mPosts.isEmpty()) maxKey = mPosts.get(mPosts.size() - 1).getKey();

        return OldArticleUtil.getTimelinePosts(COUNT_ARTICLE_TIMELINE, maxKey, isRefresh).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "getArticles:getTimelinePosts:ERROR", task.getException());
                throw task.getException();
            }

            mAdapter.setLoadingDataset(false);

            // TODO: 여기서 loading를 삭제해버리면 삭제된 직후 아이템이 insert 되기 때문에 살짝 깜빡거리는 것처럼 보이는 이슈가 있다.
            mAdapter.hideLoading();

            List<Post> posts = task.getResult();
            Log.d(TAG, "getArticles:count:" + posts.size());

            if (posts.size() < COUNT_ARTICLE_TIMELINE) mAdapter.setLastDataReached(true);

            if (mPosts.isEmpty() && posts.isEmpty()) {
                Log.d(TAG, "getArticles:current_dataset_and_new_dataset_are_empty");
                mAdapter.notifyDataSetChanged();
            }

            for (Post post : posts) {
                Log.d(TAG, "getArticles:post:" + post.toMap());
                mPosts.add(post);
                int index = mPosts.indexOf(post);
                mAdapter.notifyItemInserted(index);

                getComments(post);
            }

            return null;
        });
    }

    private Task<Void> getComments(final Post post) {
        Log.i(TAG, "getComments");

        int index = mPosts.indexOf(post);
        String postId = mPosts.get(index).getKey();

        final Task<Integer> getCountTask = CommentUtil.getCount(postId);
        final Task<List<Comment>> getTask = CommentUtil.get(postId, 3, null);
        return Tasks.whenAll(getCountTask, getTask).continueWith(task -> {
            if (!getCountTask.isSuccessful()) {
                Log.w(TAG, "getCountTask:ERROR:");
                throw getCountTask.getException();
            }
            if (!getTask.isSuccessful()) {
                Log.w(TAG, "getTask:ERROR:");
                throw getTask.getException();
            }

            int index1 = mPosts.indexOf(post);
            int commentCount = getCountTask.getResult();
            mPosts.get(index1).setCommentCount(commentCount);

            List<Comment> comments = getTask.getResult();
            mPosts.get(index1).setLatestComments(comments);
            mAdapter.notifyItemChanged(index1);

            return null;
        });
    }

    // 글을 삭제한다.
    @Deprecated
    private Task<Void> deleteArticle(final String postId) {
        Log.i(TAG, "delete:postId");
        showLoadingDialog();
        return OldArticleUtil.delete(postId).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "delete_article:ERROR:");
                // TODO: 에러메시지를 보여준다.
                dismissDialog();
                return null;
            }
            Log.d(TAG, "delete_article:SUCCESS");
            dismissDialog();
            return null;
        });
    }

    // 글을 삭제한다.
    private Task<Void> deleteArticle(final Post post) {
        Log.i(TAG, "delete");
        showLoadingDialog();
        return OldArticleUtil.delete(post).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "delete_article:ERROR:");
                // TODO: 에러메시지를 보여준다.
                dismissDialog();
                throw task.getException();
            }
            Log.d(TAG, "delete_article:SUCCESS");
            dismissDialog();
            return null;
        });
    }

    private void startArticleFragment(Post post) {
        ArticleFragment articleFragment = ArticleFragment.newInstance(post);
        articleFragment.setTargetFragment(this, ARTICLE_FRAGMENT);
        getRootFragment().add(articleFragment);
    }

    private void startProfileFragmentByUserId(String userId) {
        ProfileFragment profileFragment = ProfileFragment.newInstance(userId);
        // TODO: setTargetFragment를 호출해 줄 필요가 있을까?
        // 해당 유저를 팔로우하고 프래그먼트를 종료했을 때 필요성이 있기는 할 것 같다.
        // profileFragment.setTargetFragment(this, ARTICLE_FRAGMENT);
        getRootFragment().add(profileFragment);
    }

    private void startProfileFragmentByNickname(String nickname) {
        ProfileFragment profileFragment = ProfileFragment.newInstance(null, nickname);
        // TODO: setTargetFragment를 호출해 줄 필요가 있을까?
        // 해당 유저를 팔로우하고 프래그먼트를 종료했을 때 필요성이 있기는 할 것 같다.
        // profileFragment.setTargetFragment(this, ARTICLE_FRAGMENT);
        getRootFragment().add(profileFragment);
    }

    // TODO: 문제는 ArticleFragment가 ChildActivity에 붙었을 때의 상황이다.
    private void startHashtagFragment(String hashtag) {
        HashtagFragment hashtagFragment = HashtagFragment.newInstance(hashtag);
        getRootFragment().add(hashtagFragment);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));

        // 액티비티에서 처리하는게 더 나을 수도 있다.
        if (requestCode == CHAT_ACTIVITY) {
            if (resultCode == RESULT_OK) {
            }
            if (resultCode == RESULT_CANCELED) {
            }
            Intent intent = createIntent(ChatRoomActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }

        if (requestCode == ARTICLE_FRAGMENT) {
            if (resultCode == RESULT_OK) {
                if (data.getBooleanExtra("delete", false)) {
                    String deletedPostId = data.getStringExtra("postId");
                    Log.d(TAG, "onActivityResult:deleted_post_id:" + deletedPostId);
                    for (Post currentPost : mPosts) {
                        if (currentPost.getKey().equals(deletedPostId)) {
                            int index = mPosts.indexOf(currentPost);
                            mPosts.remove(index);
                            mAdapter.notifyItemRemoved(index);
                            CommonUtil.showSnackbar(getActivity(), "글이 삭제되었습니다.");
                            break;
                        }
                    }
                    return;
                }
            }
            if (resultCode == RESULT_CANCELED) {
            }
            Log.i(TAG, "onActivityResult:ARTICLE_FRAGMENT");

            for (Post currentPost : mPosts) {
                Post updatedPost = OldArticleUtil.getInstance().loadCachedPost(currentPost.getKey());
                if (updatedPost == null) continue;
                Log.d(TAG, "onActivityResult:loop:updatedPost:" + updatedPost.toMap());
                int index = mPosts.indexOf(currentPost);
                mPosts.set(index, updatedPost);
                mAdapter.notifyItemChanged(index);
            }
        }

        if (requestCode == WRITE_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "onActivityResult:WRITE_ACTIVITY");
                final String postId = data.getStringExtra(EXTRA_TARGET_POST_ID);
                showLoadingDialog();
                OldArticleUtil.getPost(postId, true).continueWith((Continuation<Post, Void>) task -> {
                    dismissDialog();
                    if (!task.isSuccessful()) {
                        return null;
                    }
                    Post modifiedPost = task.getResult();
                    for (Post post : mPosts) {
                        if (post.getKey().equals(postId)) {
                            int index = mPosts.indexOf(post);
                            mPosts.set(index, modifiedPost);
                            mAdapter.notifyItemChanged(index);
                        }
                    }
                    return null;
                });
            }
            if (resultCode == RESULT_CANCELED) {
            }
        }

        /*case WRITE_ACTIVITY:
        if (resultCode == RESULT_OK) {
            // CommonUtil.showSnackbar(getActivity(), "주문을 완료하였습니다.");
            mTargetPost = null;
            showLoadingDialog();
            asyncTask(true).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(@NonNull Task<Void> task) throws Exception {
                    dismissDialog();
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "onRefresh:asyncTask:ERROR:" + task.getException().getMessage());
                        return null;
                    }
                    initView();
                    return null;
                }
            });
        }
        if (resultCode == RESULT_CANCELED) {
        }
        break;*/
    }

    // private TextView mTvChatBadge;

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChatBadgeEvent(ChatBadgeEvent event) {
        Log.i(TAG, "onChatBadgeEvent");
        int badgeCount = event.badgeCount;
        ChatUtil.getInstance().saveTotalUnreadCount(badgeCount);
        updateBadge();
    }

    public void updateBadge() {
        final int badgeCount = ChatUtil.getInstance().loadTotalUnreadCount();
        Log.i(TAG, "updateBadge:badgeCount:" + badgeCount);
        MenuItem menuItemChat = getToolbar().getMenu().findItem(R.id.menu_chat);
        if (menuItemChat == null || menuItemChat.getActionView() == null) return;
        TextView tvBadge = menuItemChat.getActionView().findViewById(R.id.badge);
        if (tvBadge == null) return;
        tvBadge.setVisibility(badgeCount > 0 ? View.VISIBLE : View.GONE);
        tvBadge.setText(String.valueOf(badgeCount));
    }

    // 각 Page 프래그먼트의 onCreateOptionsMenu 메서드에서 좌우메뉴를 모두 각각 inflate 하도록 한다.
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.i(TAG, "onCreateOptionsMenu");

        if (isPrimaryFragment()) {
            // 우측 메뉴 생성: 채팅
            inflater.inflate(R.menu.menu_fragment_page_home, menu);

            final MenuItem menuItemChat = menu.findItem(R.id.menu_chat);
            View chatActionView = menuItemChat.getActionView();
            chatActionView.setOnClickListener(v -> onOptionsItemSelected(menuItemChat));

            updateBadge();

            // mTvChatBadge = chatActionView.findViewById(R.id.badge);

            /*Drawable chatIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_chat);
            menuItemChat.setIcon(chatIcon);*/

            // Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_chat);
            // Bitmap newIcon = resizeBitmapImageFn(icon, 80);
            // Drawable d = new BitmapDrawable(getResources(), newIcon);
            // menuItemChat.setIcon(d);

            // 좌측 메뉴 생성: 거래
            getToolbar().setNavigationIcon(R.drawable.ic_trade);
            getToolbar().setNavigationOnClickListener(view -> startActivity(createIntent(ShopActivity.class)));
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
        // Log.i(TAG, "onPrepareOptionsMenu");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected");

        switch (item.getItemId()) {
            /*case android.R.id.home:
                AlertDialogFragment dialogFragment = AlertDialogFragment.newInstance(
                        null,
                        "서비스 준비중입니다.",
                        new AlertDialogFragment.OnDoneListener() {
                            @Override
                            public void onDone() {
                                super.onDone();
                            }
                        }
                );
                dialogFragment.show(getFragmentManager(), null);
                return true;*/
            case R.id.menu_chat:
                Intent intent = createIntent(ChatRoomActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void refresh() {
        Log.d(TAG, "refresh");
        mRefreshContainer.setRefreshing(true);
        getArticles(true).continueWith((Continuation<Void, Void>) task -> {
            mRefreshContainer.setRefreshing(false);
            if (!task.isSuccessful()) {
                Log.w(TAG, "getArticles:ERROR");
                return null;
            }
            Log.d(TAG, "getArticles:SUCCESS");
            return null;
        });
    }
}