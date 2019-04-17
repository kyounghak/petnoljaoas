package com.chaigene.petnolja.ui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.CommentListAdapter;
import com.chaigene.petnolja.hashtag.AtSignHelper;
import com.chaigene.petnolja.hashtag.HashTagHelper;
import com.chaigene.petnolja.image.glide.targets.ArticleProgressTarget;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.ConfigManager;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.Comment;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.model.User;
import com.chaigene.petnolja.ui.activity.CardRegistrationActivity;
import com.chaigene.petnolja.ui.activity.ChatActivity;
import com.chaigene.petnolja.ui.activity.IDVerificationActivity;
import com.chaigene.petnolja.ui.activity.ImageDetailActivity;
import com.chaigene.petnolja.ui.activity.OrderSummaryActivity;
import com.chaigene.petnolja.ui.activity.SecurityPinActivity;
import com.chaigene.petnolja.ui.activity.WriteActivity;
import com.chaigene.petnolja.ui.dialog.DialogConfirmFragment;
import com.chaigene.petnolja.ui.dialog.DialogListFragment;
import com.chaigene.petnolja.ui.view.CircleProgressBar;
import com.chaigene.petnolja.util.AbuseUtil;
import com.chaigene.petnolja.util.OldArticleUtil;
import com.chaigene.petnolja.util.CommentUtil;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.ShopUtil;
import com.chaigene.petnolja.util.UserUtil;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.sephiroth.android.library.tooltip.Tooltip;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;
import static com.chaigene.petnolja.Constants.CHAT_ACTIVITY;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_POST;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_POST_ID;
import static com.chaigene.petnolja.Constants.ORDER_SUMMARY_ACTIVITY;
import static com.chaigene.petnolja.Constants.WRITE_ACTIVITY;
import static com.chaigene.petnolja.manager.AuthManager.getUserId;
import static com.chaigene.petnolja.ui.activity.CardRegistrationActivity.ACTION_STATUS_INITIAL_REGISTER;
import static com.chaigene.petnolja.ui.activity.IDVerificationActivity.ACTION_STATUS_CARD_REGISTER;

// TODO: 스택으로 쌓일 수 있는 구조의 프래그먼트 중 하나.
// 프로필 페이지, 글 페이지, 해쉬태그 페이지 등
public class ArticleFragment extends ChildFragment implements HashTagHelper.OnHashTagClickListener, AtSignHelper.OnAtSignClickListener {
    private static final String TAG = "ArticleFragment";

    private static final DecelerateInterpolator DECCELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(2);
    private static final AnticipateInterpolator ANTICIPATE_INTERPOLATOR = new AnticipateInterpolator();

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @BindView(R.id.article_content)
    NestedScrollView mSvArticleContent;

    @BindView(R.id.profile_image)
    ImageView mCivUserProfileImage;

    @BindView(R.id.nickname_text)
    TextView mTvNickname;

    @BindView(R.id.buy_button)
    TextView mTvBuyBtn;

    @BindView(R.id.follow_button)
    ImageView mIvFollowBtn;

    @BindView(R.id.option_button)
    ImageView mIvOptionBtn;

    @BindView(R.id.main_photo)
    ImageView mIvMainPhoto;

    @BindView(R.id.progress_bar)
    CircleProgressBar mVProgressBar;

    /*@BindView(R.id.progress_text)
    TextView mTvProgress;*/

    @BindView(R.id.like_button)
    ImageView mBtnLike;

    @BindView(R.id.like_count_text)
    TextView mTvLikeCount;

    @BindView(R.id.save_button)
    ImageView mBtnSave;

    @BindView(R.id.save_count_text)
    TextView mTvSaveCount;

    // 일상
    @BindView(R.id.feed_container)
    ViewGroup mVgFeedContainer;

    @BindView(R.id.feed_content_text)
    TextView mTvFeedContentText;

    @BindView(R.id.feed_date_text)
    TextView mTvFeedDateText;

    // 재능
    @BindView(R.id.talent_container)
    ViewGroup mVgTalentContainer;

    @BindView(R.id.talent_product_title_text)
    TextView mTvTalentProductTitleText;

    @BindView(R.id.talent_content_text)
    TextView mTvTalentContentText;

    @BindView(R.id.talent_product_services_text)
    TextView mTvTalentProductServicesText;

    @BindView(R.id.talent_date_text)
    TextView mTvTalentDateText;

    @BindView(R.id.comment_count_text)
    TextView mTvCommentCount;

    @BindView(R.id.send_button)
    Button mBtnSend;

    @BindView(R.id.comment_input)
    EditText mEtCommentInput;

    @BindView(R.id.comment_list)
    RecyclerView mRVCommentList;

    private boolean mIsInitializedView;

    public boolean isInitializedView() {
        return mIsInitializedView;
    }

    public void setInitializedView(boolean initializedView) {
        this.mIsInitializedView = initializedView;
    }

    private String mTargetPostId;
    private Post mTargetPost;

    private HashTagHelper mHashTagHelper;
    private AtSignHelper mAtSignHelper;

    private LinearLayoutManager mManager;
    private CommentListAdapter mAdapter;
    private List<Comment> mComments;

    private Tooltip.TooltipView mTooltipView;
    private boolean mShouldShowCommentMentionGuide;

    private Intent mResultIntent;

    // newInstace를 전달해야 할 인자값들이 있을 때만 필요하다.
    // 왜냐면 프래그먼트가 재생성 될 때 빈 생성자를 호출하게되고 저장해뒀던 인자값들이 삭제되기 때문이다.
    // Source: http://stackoverflow.com/a/9245510/4729203
    public static ArticleFragment newInstance(@NonNull String targetPostId) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_TARGET_POST_ID, targetPostId);
        fragment.setArguments(args);
        return fragment;
    }

    public static ArticleFragment newInstance(@NonNull Post targetPost) {
        ArticleFragment fragment = new ArticleFragment();
        // Post 객체에서 코멘트를 삭제해준다.
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_TARGET_POST, targetPost);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        // EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        // EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        mResultIntent = new Intent();
        mTargetPostId = bundle.getString(EXTRA_TARGET_POST_ID);
        mTargetPost = (Post) bundle.getSerializable(EXTRA_TARGET_POST);
        Log.d(TAG, "readBundle:targetPost:" + (mTargetPost != null ? mTargetPost.toMap() : null));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_article, container, false);
        ButterKnife.bind(this, mView);

        // 이미지 최소 높이 지정
        mIvMainPhoto.setMinimumHeight(CommonUtil.getScreenWidth(getContext()));

        showLoadingDialog();
        asyncTask(false).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "asyncTask:ERROR:" + task.getException().getMessage());
                    finish();
                }
                initView();
                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "initView:ERROR:" + task.getException().getMessage());
                }
                return null;
            }
        });

        mRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    final String TAG = "OnRefreshListener";

                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh");
                        mRefreshLayout.setRefreshing(false);
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
                }
        );

        return mView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");

        // Intent intent = new Intent();
        // intent.putExtra(FRAGMENT_KEY, "Ok");
        // getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);

        if (getTargetFragment() != null)
            getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, mResultIntent);
    }

    // 팔로잉 여부를 가져와야 한다.
    private Task<Void> asyncTask(final boolean isRefresh) {
        Log.i(TAG, "asyncTask");
        final Executor executor = Executors.newSingleThreadExecutor();
        return Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // 절대 발생해서는 안되는 상황.
                if (mTargetPostId == null && mTargetPost == null) finish();

                if (mTargetPost == null) {
                    Task<Post> getPostTask = OldArticleUtil.getPost(mTargetPostId, isRefresh);
                    Post user = Tasks.await(getPostTask);
                    if (!getPostTask.isSuccessful()) {
                        Log.w(TAG, "asyncTask:getUserTask:ERROR:", getPostTask.getException());
                        throw getPostTask.getException();
                    }
                    mTargetPost = user;
                }

                if (mTargetPostId == null) {
                    mTargetPostId = mTargetPost.getKey();
                }

                Log.d(TAG, "asyncTask:post:" + mTargetPost.toMap().toString());

                Task<Integer> getCountTask = CommentUtil.getCount(mTargetPostId);
                Task<List<Comment>> getCommentsTask = CommentUtil.getComments(mTargetPostId);

                Tasks.await(Tasks.whenAll(getCountTask, getCommentsTask));

                if (!getCountTask.isSuccessful()) {
                    Exception getCountError = getCountTask.getException();
                    Log.w(TAG, "asyncTask:getCountTask:ERROR:" + getCountError.getMessage());
                    throw getCountError;
                }

                if (!getCommentsTask.isSuccessful()) {
                    Exception getCommentsError = getCommentsTask.getException();
                    Log.w(TAG, "asyncTask:getCommentsTask:ERROR:" + getCommentsError.getMessage());
                    throw getCommentsError;
                }

                int commentCount = getCountTask.getResult();
                Log.d(TAG, "asyncTask:getCountTask:commentCount:" + commentCount);
                mTargetPost.setCommentCount(commentCount);

                List<Comment> comments = getCommentsTask.getResult();
                mComments = new ArrayList<>();
                for (Comment comment : comments) {
                    Log.d(TAG, "asyncTask:getComments:comment:" + comment.toMap());
                    mComments.add(comment);
                }
                mTargetPost.setLatestComments(mComments);

                // asyncTask가 완료되면 캐쉬에 객체를 저장한다.
                OldArticleUtil.getInstance().saveCachedPost(mTargetPost);

                return null;
            }
        });
    }

    @Override
    protected void initView() {
        Log.i(TAG, "initView");
        super.initView();

        // 불필요한 코드
        // mSvArticleContent.setVisibility(View.VISIBLE);
        String targetUid = mTargetPost.getUser() != null ? mTargetPost.getUser().getId() : null;

        // 유저 정보
        if (mTargetPost.getUser() != null) {
            User user = mTargetPost.getUser();
            String signature = user.getSignature();
            /*Map<String, Boolean> userRegions = user.getRegions();
            String userRegion = null;
            if (userRegions != null && !userRegions.isEmpty()) {
                userRegion = userRegions.keySet().iterator().next();
            }*/
            UserUtil.downloadProfileImage(targetUid, signature, mCivUserProfileImage);
            mTvNickname.setText(mTargetPost.getUser().getNickname());
            // mCivUserProfileImage.setOnClickListener(this);
            // mTvNickname.setOnClickListener(this);
        } else {
            mTvNickname.setText("(알 수 없음)");
        }

        // 팔로우 버튼 처리
        if (targetUid != null && !targetUid.equals(getUserId())) {
            // FIXME
            /*if (mTargetPost.getUser().isFollowing()) {
                mIvFollowBtn.setActivated(true);
            } else {
                mIvFollowBtn.setActivated(false);
            }*/
        } else {
            mIvFollowBtn.setVisibility(View.GONE);
        }

        if (mTargetPost.getType() == ARTICLE_TYPE_FEED) {
            CommonUtil.showViews(mVgFeedContainer);
            CommonUtil.hideViews(mVgTalentContainer);
            if (!TextUtils.isEmpty(mTargetPost.getContent())) {
                mTvFeedContentText.setText(mTargetPost.getContent());
            } else {
                mTvFeedContentText.setVisibility(View.GONE);
            }
            HashTagHelper hashTagHelper = CommonUtil.createDefaultHashTagHelper(getContext(), this);
            hashTagHelper.handle(mTvFeedContentText);
            AtSignHelper atSignHelper = CommonUtil.createDefaultAtSignHelper(getContext(), this);
            atSignHelper.handle(mTvFeedContentText);

            // 시각
            String date = CommonUtil.getTimeAgo(getContext(), mTargetPost.getTimestamp(true));
            mTvFeedDateText.setText(date);
        }
        if (mTargetPost.getType() == ARTICLE_TYPE_TALENT) {
            CommonUtil.showViews(mVgTalentContainer);
            CommonUtil.hideViews(mVgFeedContainer);
            String productTitle = !TextUtils.isEmpty(mTargetPost.getProductTitle()) ?
                    mTargetPost.getProductTitle() : "무제";
            mTvTalentProductTitleText.setText(productTitle);

            if (!TextUtils.isEmpty(mTargetPost.getContent())) {
                mTvTalentContentText.setText(mTargetPost.getContent());
            } else {
                mTvTalentContentText.setVisibility(View.GONE);
            }
            HashTagHelper hashTagHelper = CommonUtil.createDefaultHashTagHelper(getContext(), this);
            hashTagHelper.handle(mTvTalentContentText);
            AtSignHelper atSignHelper = CommonUtil.createDefaultAtSignHelper(getContext(), this);
            atSignHelper.handle(mTvTalentContentText);

            String productServices = !TextUtils.isEmpty(mTargetPost.getProductServices()) ?
                    mTargetPost.getProductServices() : "메이커가 환불 및 보상정책을 입력하지 않았습니다.";
            mTvTalentProductServicesText.setText(productServices);

            // 시각
            String date = CommonUtil.getTimeAgo(getContext(), mTargetPost.getTimestamp(true));
            mTvTalentDateText.setText(date);
        }

        // 만약 좋아요를 한 상태라면
        if (mTargetPost.getLikes().containsKey(getUserId())) {
            mBtnLike.setActivated(true);
        }
        // 좋아요 갯수
        mTvLikeCount.setText(String.valueOf(mTargetPost.getLikeCount()));

        // 댓글 갯수
        mTvCommentCount.setText(String.valueOf(mTargetPost.getCommentCount()));

        // 만약 세이브를 한 상태라면
        if (mTargetPost.getSaves().containsKey(getUserId())) {
            mBtnSave.setActivated(true);
        }
        // 세이브 갯수
        mTvSaveCount.setText(String.valueOf(mTargetPost.getSaveCount()));

        // 이미지 다운로드
        downloadPhoto(mTargetPost.getKey(), mTargetPost.getPhotosList().get(0), mIvMainPhoto, mVProgressBar);

        mIvMainPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 리프레싱 할 때 null이 발생하는 경우가 있음
                if (mTargetPost == null) return;

                List<String> photos = mTargetPost.getPhotosList();
                ArrayList<String> targetPaths = new ArrayList<>();
                for (String filename : photos) {
                    StorageReference postsRef = StorageManager.getArticlePostsRef().child(mTargetPost.getKey()).child(filename);
                    String path = postsRef.getPath();
                    targetPaths.add(path);
                }
                ArrayList<String> regions = new ArrayList<>();
                Map<String, Boolean> regionsMap = mTargetPost.getRegions();
                if (regionsMap != null && !regionsMap.isEmpty()) {
                    regions.addAll(regionsMap.keySet());
                }
                Intent intent = ImageDetailActivity.createIntent(getContext(), regions, targetPaths, 0);
                startActivity(intent);
            }
        });

        if (mTargetPost.getType() == ARTICLE_TYPE_FEED) {
            mTvBuyBtn.setVisibility(View.GONE);
        }

        if (mTargetPost.getType() == ARTICLE_TYPE_TALENT) {
            mTvBuyBtn.setVisibility(View.VISIBLE);
        }

        if (!isInitializedView()) {
            setupScrollView();
            setupCommentRecyclerView();
            setupMessageInput();
        }
        setInitializedView(true);
    }

    @OnClick({R.id.like_button_container, R.id.like_button})
    void updateLike() {
        Log.i(TAG, "updateLike");
        mBtnLike.setEnabled(false);
        final boolean isActivated = mBtnLike.isActivated();
        mBtnLike.setActivated(!isActivated);
        int oldlikeCount = mTargetPost.getLikeCount();
        int newLikeCount = !isActivated ? oldlikeCount + 1 : oldlikeCount - 1 > 0 ? oldlikeCount - 1 : 0;
        Log.d(TAG, "updateLike:newLikeCount:" + newLikeCount);
        mTvLikeCount.setText(String.valueOf(newLikeCount));
        Task<Integer> likeTask = !isActivated ? OldArticleUtil.like(mTargetPostId) : OldArticleUtil.unlike(mTargetPostId);
        likeTask.continueWith(new Continuation<Integer, Void>() {
            @Override
            public Void then(@NonNull Task<Integer> task) throws Exception {
                mBtnLike.setEnabled(true);
                if (!task.isSuccessful()) {
                    Log.w(TAG, "updateLike:ERROR" + task.getException().getMessage());
                    mBtnLike.setActivated(isActivated);
                    return null;
                }
                Log.d(TAG, "updateLike:isActivated:" + mBtnLike.isActivated());
                String myUid = AuthManager.getUserId();
                if (mTargetPost.getLikes().containsKey(myUid)) {
                    mTargetPost.getLikes().remove(myUid);
                } else {
                    mTargetPost.getLikes().put(myUid, true);
                }
                int likeCount = task.getResult();
                mTargetPost.setLikeCount(likeCount);
                mTvLikeCount.setText(String.valueOf(likeCount));
                return null;
            }
        });
    }

    @OnClick({R.id.comment_button_container, R.id.comment_button})
    void focusComment() {
        Log.i(TAG, "focusComment");
        mEtCommentInput.requestFocus();
        CommonUtil.showKeyboard(getActivity());
    }

    @OnClick({R.id.save_button_container, R.id.save_button})
    void updateSave() {
        Log.i(TAG, "updateSave");
        mBtnSave.setEnabled(false);
        final boolean isActivated = mBtnSave.isActivated();
        mBtnSave.setActivated(!isActivated);
        int oldSaveCount = mTargetPost.getSaveCount();
        int newSaveCount = !isActivated ? oldSaveCount + 1 : oldSaveCount - 1 > 0 ? oldSaveCount - 1 : 0;
        Log.d(TAG, "updateSave:newSaveCount:" + newSaveCount);
        mTvSaveCount.setText(String.valueOf(newSaveCount));
        Task<Integer> saveTask = !isActivated ? OldArticleUtil.save(mTargetPostId) : OldArticleUtil.unsave(mTargetPostId);
        saveTask.continueWith(new Continuation<Integer, Void>() {
            @Override
            public Void then(@NonNull Task<Integer> task) throws Exception {
                mBtnSave.setEnabled(true);
                if (!task.isSuccessful()) {
                    Log.w(TAG, "updateSave:ERROR:" + task.getException().getMessage());
                    mBtnSave.setActivated(isActivated);
                    return null;
                }
                Log.d(TAG, "updateSave:isActivated:" + mBtnSave.isActivated());
                String myUid = AuthManager.getUserId();
                if (mTargetPost.getSaves().containsKey(myUid)) {
                    mTargetPost.getSaves().remove(myUid);
                } else {
                    mTargetPost.getSaves().put(myUid, true);
                }
                int saveCount = task.getResult();
                mTargetPost.setSaveCount(saveCount);
                mTvSaveCount.setText(String.valueOf(saveCount));
                return null;
            }
        });
    }

    /*@OnClick(R.id.follow_button)
    void updateFollow(final View view) {
        view.setEnabled(false);
        showLoadingDialog();
        String targetUid = mTargetPost.getUser().getId();
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
                // TODO: 팔로우 버튼을 바꿔줘야 한다.

                Log.d(TAG, "updateFollow:isActivated:" + view.isActivated());

                return null;
            }
        });
    }*/

    @OnClick(R.id.buy_button)
    void buy() {
        /*if (!AuthManager.getUserId().equals("z98ZqYCzvFSmkMyH8IacJYLUoWC2") && !BuildConfig.DEBUG) {
            DialogAlertFragment alertDialog = DialogAlertFragment.newInstance(
                    null,
                    "서비스 준비중입니다.",
                    new DialogAlertFragment.OnDoneListener() {
                        @Override
                        public void onDone() {
                            super.onDone();
                        }
                    }
            );
            alertDialog.show(getFragmentManager(), null);
            return;
        }*/

        DialogConfirmFragment dialogFragment = DialogConfirmFragment.newInstance(
                CommonUtil.numberFormat(mTargetPost.getProductPrice(), "원"),
                "구매를 진행하시겠습니까?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();

                        showLoadingDialog();
                        isEligibleForShopping().continueWith(new Continuation<Boolean, Void>() {
                            @Override
                            public Void then(@NonNull Task<Boolean> task) throws Exception {
                                dismissDialog();
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "buy:isEligibleForShopping:ERROR:" + task.getException().getMessage());
                                    return null;
                                }
                                boolean isEligible = task.getResult();
                                if (!isEligible) return null;

                                // TODO: SecurityPinActivity 시작
                                // TODO: 실제 프로세스는 결제 관련된 베이스 액티비티가 띄워지고 거기서
                                // startActivityForResult를 통해서 시작해야 한다.
                                int productType = 0;

                                Intent in = OrderSummaryActivity.createIntent(
                                        getContext(),
                                        mTargetPostId,
                                        productType,
                                        mTargetPost.getProductTitle(),
                                        mTargetPost.getProductPrice(),
                                        mTargetPost.getCoverPhoto(),
                                        new ArrayList<>(mTargetPost.getRegionsList()),
                                        mTargetPost.getShippingPrice(),
                                        mTargetPost.getUser().getId(),
                                        mTargetPost.getUser().getNickname()
                                );
                                startActivityForResult(in, ORDER_SUMMARY_ACTIVITY);
                                return null;
                            }
                        });
                    }

                    @Override
                    public void onDeny() {
                        super.onDeny();
                    }
                }
        );
        dialogFragment.show(getFragmentManager(), null);
    }

    /*@Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderCompleteEvent(OrderCompleteEvent event) {
        Log.i(TAG, "onOrderCompleteEvent");
    }*/

    private Task<Boolean> isEligibleForShopping() {
        Log.i(TAG, "isEligibleForShopping");
        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Task<Boolean> isPhoneVerifiedTask = UserUtil.isPhoneVerified();
                boolean isPhoneVerified = Tasks.await(isPhoneVerifiedTask);
                if (!isPhoneVerifiedTask.isSuccessful()) {
                    Exception isPhoneVerifiedError = isPhoneVerifiedTask.getException();
                    Log.w(TAG, "isEligibleForShopping:isPhoneVerifiedTask:ERROR:" + isPhoneVerifiedError.getMessage());
                    throw isPhoneVerifiedError;
                }
                Log.d(TAG, "isEligibleForShopping:isPhoneVerified:" + isPhoneVerified);
                if (!isPhoneVerified) {
                    onPhoneNotVerified();
                    return false;
                }

                Task<Boolean> isCardExistsTask = ShopUtil.isCardExists();
                boolean isCardExists = Tasks.await(isCardExistsTask);
                if (!isCardExistsTask.isSuccessful()) {
                    Exception isCardExistsError = isCardExistsTask.getException();
                    Log.w(TAG, "isEligibleForShopping:isCardExistsTask:ERROR:" + isCardExistsError.getMessage());
                    throw isCardExistsError;
                }
                Log.d(TAG, "isEligibleForShopping:isCardExists:" + isCardExists);
                if (!isCardExists) {
                    onCardNotExists();
                    return false;
                }

                Task<Boolean> isSecurityPinExistsTask = ShopUtil.isSecurityPinExists();
                boolean isSecurityPinExists = Tasks.await(isSecurityPinExistsTask);
                if (!isSecurityPinExistsTask.isSuccessful()) {
                    Exception isSecurityPinExistsError = isSecurityPinExistsTask.getException();
                    Log.w(TAG, "isEligibleForShopping:isSecurityPinExistsTask:ERROR:" + isSecurityPinExistsError.getMessage());
                    throw isSecurityPinExistsError;
                }
                Log.d(TAG, "isEligibleForShopping:isSecurityPinExists:" + isSecurityPinExists);
                if (!isSecurityPinExists) {
                    onSecurityPinNotExists();
                    return false;
                }

                return true;
            }
        });
    }

    private void onPhoneNotVerified() {
        Log.i(TAG, "onPhoneNotVerified");
        Intent in = IDVerificationActivity.createIntent(getContext(), ACTION_STATUS_CARD_REGISTER);
        startActivity(in);
    }

    private void onCardNotExists() {
        Log.i(TAG, "onCardNotExists");
        Intent in = CardRegistrationActivity.createIntent(getContext(), ACTION_STATUS_INITIAL_REGISTER);
        startActivity(in);
    }

    private void onSecurityPinNotExists() {
        Log.i(TAG, "onSecurityPinNotExists");
        Intent in = SecurityPinActivity.createIntent(getContext(), 0);
        startActivity(in);
    }

    @OnClick(R.id.option_button)
    void showOption(View view) {
        boolean isMyArticle = mTargetPost.getUser().getId().equals(AuthManager.getUserId());
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            final String TAG = "OnMenuItemClickListener";

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.i(TAG, "onMenuItemClick");
                switch (item.getItemId()) {
                    case R.id.action_report:
                        Log.d(TAG, "onMenuItemClick:action_report");
                        final String postId = mTargetPost.getKey();
                        final String targetUid = mTargetPost.getUser().getId();
                        final String targetNickname = mTargetPost.getUser().getNickname();
                        DialogConfirmFragment reportDialog = DialogConfirmFragment.newInstance(
                                "해당 게시물을 신고하시겠습니까?",
                                new DialogConfirmFragment.OnSelectListener() {
                                    @Override
                                    public void onConfirm() {
                                        super.onConfirm();
                                        showLoadingDialog();
                                        AbuseUtil.reportArticle(postId, targetUid, targetNickname).continueWith(new Continuation<Void, Void>() {
                                            @Override
                                            public Void then(@NonNull Task<Void> task) throws Exception {
                                                dismissDialog();
                                                if (!task.isSuccessful()) {
                                                    Log.w(TAG, "onItemReportButtonClick:reportArticle:ERROR:" + task.getException().getMessage());
                                                    CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.");
                                                    return null;
                                                }
                                                CommonUtil.showSnackbar(getActivity(), "신고해주셔서 감사합니다. 판다팀에서 검토 뒤 적절한 제재를 취하겠습니다.");
                                                return null;
                                            }
                                        });
                                    }

                                    @Override
                                    public void onDeny() {
                                        super.onDeny();
                                    }
                                }
                        );
                        reportDialog.show(getFragmentManager(), null);
                        break;
                    case R.id.action_message:
                        Log.d(TAG, "onMenuItemClick:action_message");
                        Intent intent = ChatActivity.createIntent(getContext(), mTargetPost.getUser());
                        startActivityForResult(intent, CHAT_ACTIVITY);
                        break;
                    case R.id.action_modify:
                        // 글을 수정한다.
                        Log.d(TAG, "onMenuItemClick:action_modify");
                        Intent in = WriteActivity.createModifyIntent(getContext(), mTargetPostId);
                        startActivityForResult(in, WRITE_ACTIVITY);
                        break;
                    case R.id.action_delete:
                        // 글을 삭제한다.
                        Log.d(TAG, "onMenuItemClick:action_delete");
                        DialogConfirmFragment deleteDialog = DialogConfirmFragment.newInstance(
                                "삭제하시겠습니까?",
                                new DialogConfirmFragment.OnSelectListener() {
                                    @Override
                                    public void onConfirm() {
                                        super.onConfirm();
                                        showLoadingDialog();
                                        OldArticleUtil.delete(mTargetPost).continueWith(new Continuation<Void, Void>() {
                                            @Override
                                            public Void then(@NonNull Task<Void> task) throws Exception {
                                                dismissDialog();
                                                if (!task.isSuccessful()) {
                                                    Log.w(TAG, "onMenuItemClick:action_delete:delete:ERROR:");
                                                    CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.");
                                                    return null;
                                                }
                                                mResultIntent.putExtra("delete", true);
                                                mResultIntent.putExtra("postId", mTargetPostId);
                                                finish();
                                                return null;
                                            }
                                        });
                                    }

                                    @Override
                                    public void onDeny() {
                                        super.onDeny();
                                    }
                                }
                        );
                        deleteDialog.show(getFragmentManager(), null);
                        break;
                }
                return true;
            }
        });
        // TODO: 나의 글이냐 타인의 글이냐에 따라서 메뉴가 달라져야 함.
        if (isMyArticle) {
            popupMenu.inflate(R.menu.menu_feed_popup_mine);
        } else {
            popupMenu.inflate(R.menu.menu_feed_popup_other);
        }

        MenuItem reportMenu = popupMenu.getMenu().findItem(R.id.action_report);
        if (reportMenu != null) {
            SpannableString s = new SpannableString(reportMenu.getTitle());
            s.setSpan(new ForegroundColorSpan(CommonUtil.getColor(getContext(), R.color.material_red_500)), 0, s.length(), 0);
            reportMenu.setTitle(s);
        }

        popupMenu.show();
    }

    private void setupMessageInput() {
        Log.i(TAG, "setupMessageInput");

        mHashTagHelper = CommonUtil.createDefaultHashTagHelper(getContext(), null);
        mHashTagHelper.handle(mEtCommentInput);
        mAtSignHelper = CommonUtil.createDefaultAtSignHelper(getContext(), null);
        mAtSignHelper.handle(mEtCommentInput);
        KeyboardVisibilityEvent.setEventListener(
                getActivity(),
                new KeyboardVisibilityEventListener() {
                    @Override
                    public void onVisibilityChanged(boolean b) {
                        View lastChild = mSvArticleContent.getChildAt(mSvArticleContent.getChildCount() - 1);
                        int bottom = lastChild.getBottom() + mSvArticleContent.getPaddingBottom();
                        int sy = mSvArticleContent.getScrollY();
                        int sh = mSvArticleContent.getHeight();
                        int delta = bottom - (sy + sh);
                        mSvArticleContent.smoothScrollBy(0, delta);
                    }
                }
        );
        mEtCommentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Log.i(TAG, "afterTextChanged:s:'" + s.toString() + "'");
                if (s.toString().trim().length() == 0) mBtnSend.setEnabled(false);
                else mBtnSend.setEnabled(true);
            }
        });
        mEtCommentInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            final String TAG = "OnEditorActionListener";

            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                // if (isProgressing) return false;
                Log.i(TAG, "onEditorAction:id:" + id);

                if (id == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    Log.i(TAG, "onEditorAction:id:IME_NULL|keyEvent:ACTION_DOWN");
                    insertComment();
                    return true;
                }

                if (id == EditorInfo.IME_ACTION_DONE) {
                    Log.i(TAG, "onEditorAction:id:IME_ACTION_DONE");
                    insertComment();
                    return true;
                }
                return false;
            }
        });
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertComment();
            }
        });
        CommonUtil.hideKeyboard(getActivity());
    }

    // TODO: 코멘트가 등록되면 서버단에서 /article/posts/$postId 여기에서 해당 postId에 대한 가장 최근의 코멘트 3개를 추출한 뒤 하위 값으로 삽입해준다.
    private void insertComment() {
        showLoadingDialog();

        CommonUtil.clearFocus(mEtCommentInput);
        CommonUtil.hideKeyboard(getActivity());

        final String content = mEtCommentInput.getText().toString().trim();
        List<String> hashtags = mHashTagHelper.getAllHashTags();
        List<String> mentions = mAtSignHelper.getAllAtSigns();

        mEtCommentInput.setText(null);

        CommentUtil.insert(mTargetPost.getKey(), content, hashtags, mentions).continueWith(new Continuation<Comment, Void>() {
            @Override
            public Void then(@NonNull Task<Comment> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "insertComment:ERROR", task.getException());
                    if (task.getException().getMessage().equals("Firebase Database error: Permission denied")) {
                        CommonUtil.showSnackbar(getActivity(), R.string.msg_failed_to_upload_comment);
                    }
                    dismissDialog();
                    return null;
                }
                Log.d(TAG, "insertComment:SUCCESS");
                dismissDialog();

                Comment comment = task.getResult();
                Log.d(TAG, "insertComment:comment:" + comment.toMap().toString());

                mComments.add(comment);
                mAdapter.notifyItemInserted(mComments.size() - 1);
                Tasks.call(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        mSvArticleContent.fullScroll(ScrollView.FOCUS_DOWN);
                        return null;
                    }
                });
                mTargetPost.setCommentCount(mTargetPost.getCommentCount() + 1);
                mTvCommentCount.setText(String.valueOf(mTargetPost.getCommentCount()));
                return null;
            }
        });
    }

    private void animateLikeBtn() {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator imgScaleDownXAnim = ObjectAnimator.ofFloat(mBtnLike, "scaleX", 1f, 0.2f);
        imgScaleDownXAnim.setDuration(200);
        imgScaleDownXAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator imgScaleDownYAnim = ObjectAnimator.ofFloat(mBtnLike, "scaleY", 1f, 0.2f);
        imgScaleDownYAnim.setDuration(200);
        imgScaleDownYAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator imgScaleUpXAnim = ObjectAnimator.ofFloat(mBtnLike, "scaleY", 0.2f, 1f);
        imgScaleUpXAnim.setDuration(200);
        imgScaleUpXAnim.setInterpolator(OVERSHOOT_INTERPOLATOR);

        ObjectAnimator imgScaleUpYAnim = ObjectAnimator.ofFloat(mBtnLike, "scaleX", 0.2f, 1f);
        imgScaleUpYAnim.setDuration(200);
        imgScaleUpYAnim.setInterpolator(OVERSHOOT_INTERPOLATOR);

        if (!mBtnLike.isActivated()) {
            imgScaleUpXAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mBtnLike.setImageResource(R.drawable.ic_like_active);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mBtnLike.setActivated(true);
                }
            });
        } else {
            imgScaleUpXAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mBtnLike.setImageResource(R.drawable.ic_like);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mBtnLike.setActivated(false);
                }
            });
        }
        animatorSet.playTogether(imgScaleDownXAnim, imgScaleDownYAnim);
        animatorSet.play(imgScaleUpXAnim).with(imgScaleUpYAnim).after(imgScaleDownXAnim);
        animatorSet.start();
    }

// Map<RecyclerView.ViewHolder, AnimatorSet> heartAnimationsMap = new HashMap<>();
    /*private void animateHeartButton() {
        AnimatorSet animatorSet = new AnimatorSet();

        *//*ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(mBtnLike, "rotation", 0f, 360f);
        rotationAnim.setDuration(300);
        rotationAnim.setInterpolator(ACCELERATE_INTERPOLATOR);*//*

        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(mBtnLike, "scaleX", 0.2f, 1f);
        bounceAnimX.setDuration(300);
        bounceAnimX.setInterpolator(ANTICIPATE_INTERPOLATOR);

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(mBtnLike, "scaleY", 0.2f, 1f);
        bounceAnimY.setDuration(300);
        bounceAnimY.setInterpolator(ANTICIPATE_INTERPOLATOR);
        bounceAnimY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBtnLike.setImageResource(R.drawable.ic_like_active);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // heartAnimationsMap.remove(holder);
                // dispatchChangeFinishedIfAllAnimationsEnded(holder);
            }
        });

        animatorSet.play(bounceAnimX).with(bounceAnimY);
        animatorSet.start();

        // heartAnimationsMap.put(holder, animatorSet);
    }*/

    /*private void dispatchChangeFinishedIfAllAnimationsEnded(RecyclerView.ViewHolder holder) {
        // if (heartAnimationsMap.containsKey(holder)) {
        //     return;
        // }
        // dispatchAnimationFinished(holder);
        // DefaultItemAnimator animator = new DefaultItemAnimator();
        // animator.dispatchAnimationFinished(holder);
        // TODO: 아래와 같은 형식으로 사용된다.
        // 멤버 필드에 선언해두고 애니메이션이 끝나면 끝났다고 알려줘야 한다.
        // rvFeed.setItemAnimator(new FeedItemAnimator());
    }*/

    private void downloadPhoto(@NonNull String postId,
                               @NonNull String filename,
                               @NonNull ImageView imageView,
                               @NonNull CircleProgressBar progressView) {
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        ArticleProgressTarget<Bitmap> target = new ArticleProgressTarget<>(
                getContext(),
                new BitmapImageViewTarget(imageView),
                imageView,
                progressView
        );
        GlideManager.loadImageWithTarget(
                postsRef,
                null,
                imageView,
                GlideManager.SCALE_TYPE_FIT_CENTER,
                target
        );
    }

    /*private Task<Void> downloadPhoto(ImageView view, String postId, String filename) {
        Log.i(TAG, "downloadPhoto");
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        return GlideManager.loadImage(postsRef, view);
    }*/

    private void setupScrollView() {
        Log.i(TAG, "setupScrollView");
        mSvArticleContent.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {

            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                Rect scrollBounds = new Rect();
                v.getHitRect(scrollBounds);
                if (mRVCommentList.getLocalVisibleRect(scrollBounds)) {
                    // Log.d(TAG, "setupScrollView:visible");
                    if (mShouldShowCommentMentionGuide) {
                        mShouldShowCommentMentionGuide = false;
                        showCommentMentionGuide();
                    }
                } else {
                    // Log.d(TAG, "setupScrollView:hidden");
                }
            }
        });
    }

    private void setupCommentRecyclerView() {
        Log.i(TAG, "setupCommentRecyclerView");

        mManager = new LinearLayoutManager(getContext().getApplicationContext());
        mRVCommentList.setLayoutManager(mManager);

        mAdapter = new CommentListAdapter(getContext(), mComments);

        // 댓글 멘션을 어떻게 사용하는지에 대한 가이드를 보여준다.
        // (약간 hacky 한 방법을 사용했기 때문에 추후에 변경할 필요가 있어 보인다.)
        // if (!ConfigManager.getInstance(getContext()).isGuideShownMentionComment() || BuildConfig.DEBUG) {
        if (!ConfigManager.getInstance(getContext()).isGuideShownMentionComment()) {
            LinearLayoutManager manager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false) {
                final String TAG = "LinearLayoutManager";
                boolean isShown = false;

                @Override
                public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                    super.onLayoutChildren(recycler, state);
                    Log.d(TAG, "onLayoutChildren:childCount:" + mRVCommentList.getChildCount());
                    // 댓글이 0개면 가이드를 보여주지 않는다.
                    if (mRVCommentList.getChildCount() == 0) return;
                    Comment firstComment = mComments.get(0);
                    // 내가 쓴 댓글이면 가이드를 보여주지 않는다.
                    if (firstComment.getUid().equals(AuthManager.getUserId())) return;
                    if (!isShown) {
                        isShown = true;
                        mShouldShowCommentMentionGuide = true;
                    }
                }
            };
            mRVCommentList.setLayoutManager(manager);
        }
        mAdapter.setOnItemClickListener(new CommentListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

            }

            @Override
            public void onItemNicknameClick(String userId, int position) {
                Log.i(TAG, "onItemNicknameClick:userId:" + userId + "|position:" + position);
                startProfileFragmentByUserId(userId);
            }

            @Override
            public void onItemHashtagClick(String hashtag) {
                Log.i(TAG, "onItemHashtagClick:hashtag:" + hashtag);
                startHashtagFragment(hashtag);
            }

            @Override
            public void onItemAtSignClick(String atSign) {
                Log.i(TAG, "onItemAtSignClick:atSign:" + atSign);
                startProfileFragmentByNickname(atSign);
            }
        });
        mAdapter.setOnItemLongClickListener(new CommentListAdapter.OnItemLongClickListener() {
            final String TAG = "OnItemLongClickListener";

            @Override
            public void onItemLongClick(final int position) {
                Log.i(TAG, "onItemClick:position:" + position);

                if (mTooltipView != null) mTooltipView.hide();

                final Comment comment = mComments.get(position);

                boolean isMyArticle = mTargetPost.getUser().getId().equals(AuthManager.getUserId());
                boolean isMyComment = comment.getUid().equals(AuthManager.getUserId());

                if (!isMyArticle && !isMyComment) return;

                Log.d(TAG, "onSelect:delete_comment:comment:" + comment.toMap());
                for (Comment c : mComments) {
                    Log.d(TAG, "onSelect:delete_comment:comments:loop:comment:" + c.toMap());
                }

                DialogListFragment dialogFragment = DialogListFragment.newInstance(
                        new String[]{getString(R.string.delete)},
                        new DialogListFragment.OnItemSelectListener() {
                            final String TAG = "OnItemSelectListener";

                            @Override
                            public void onSelect(int index) {
                                Log.i(TAG, "onSelect");

                                /*final int oldHeight = mRVCommentList.getHeight();
                                Log.d(TAG, "onSelect:oldHeight:" + oldHeight);
                                mRVCommentList.getLayoutParams().height = oldHeight;*/

                                // mRVCommentList.setLayoutFrozen(true);

                                final String commentId = comment.getKey();
                                CommentUtil.delete(mTargetPost.getKey(), commentId).continueWith(new Continuation<Void, Void>() {
                                    @Override
                                    public Void then(@NonNull Task<Void> task) throws Exception {
                                        if (!task.isSuccessful()) {
                                            Log.d(TAG, "onSelect:delete_comment:ERROR");
                                            CommonUtil.showSnackbar(getActivity(), getString(R.string.msg_error_failed_delete_comment));
                                            return null;
                                        }
                                        // java.lang.ArrayIndexOutOfBoundsException: length=10; index=-1
                                        final int commentIndex = mComments.indexOf(comment);

                                        for (Comment c : mComments) {
                                            Log.d(TAG, "onSelect:delete_comment:changed_comments:loop:comment:" + c.toMap());
                                        }
                                        Log.d(TAG, "onSelect:delete_comment:commentIndex:" + commentIndex);

                                        Iterator<Comment> iterator = mComments.iterator();
                                        //noinspection WhileLoopReplaceableByForEach
                                        while (iterator.hasNext()) {
                                            Comment comment = iterator.next();
                                            if (comment.getKey().equals(commentId))
                                                iterator.remove();
                                        }

                                        // mComments.remove(commentIndex);
                                        // This line below gives you the animation and also updates the list items after the deleted item
                                        // mAdapter.notifyItemRangeChanged(commentIndex, mComments.size());
                                        // mAdapter.notifyItemRemoved(commentIndex);

                                        // mAdapter.notifyItemRangeChanged(commentIndex, mComments.size());
                                        // mAdapter.notifyItemRemoved(commentIndex);
                                        mAdapter.notifyDataSetChanged();

                                        // mRVCommentList.invalidate();
                                        // mRVCommentList.requestLayout();

                                        // mRVCommentList.setLayoutFrozen(true);
                                        // mRVCommentList.setLayoutFrozen(false);

                                        // TODO: notifyItemRemoved를 호출했을 때 마지막 아이템이 사라졌다가 다시 나타나는 이슈가 있다.
                                        // 원인은 리싸이클러뷰의 높이가 고정값이 아닐 때 발생한다.

                                        /*View commentView = mRVCommentList.getChildAt(commentIndex);
                                        int newHeight = oldHeight - commentView.getHeight();
                                        Log.d(TAG, "onSelect:newHeight:" + newHeight);
                                        ValueAnimator animator = ValueAnimator.ofInt(oldHeight, newHeight);
                                        animator.setDuration(400);
                                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                            public void onAnimationUpdate(ValueAnimator animation) {
                                                Integer value = (Integer) animation.getAnimatedValue();
                                                mRVCommentList.getLayoutParams().height = value;
                                                mRVCommentList.requestLayout();
                                                mRVCommentList.setLayoutParams(new LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                        LinearLayout.LayoutParams.MATCH_PARENT
                                                ));
                                                mRVCommentList.requestLayout();
                                            }
                                        });
                                        animator.start();*/

                                        /*new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {

                                                *//*mRVCommentList.setLayoutParams(new LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                        LinearLayout.LayoutParams.MATCH_PARENT
                                                ));

                                                int newHeight = mRVCommentList.getHeight();
                                                int newMeasuredHeight = mRVCommentList.getMeasuredHeight();
                                                Log.d(TAG, "onSelect:newHeight:" + newHeight);
                                                Log.d(TAG, "onSelect:newMeasuredHeight:" + newMeasuredHeight);*//*
                                            }
                                        }, 500);*/

                                        // mAdapter.notifyItemRangeRemoved(commentIndex, 1);

                                        int commentCount = mTargetPost.getCommentCount() != 0 ? mTargetPost.getCommentCount() - 1 : 0;
                                        mTargetPost.setCommentCount(commentCount);
                                        mTvCommentCount.setText(String.valueOf(mTargetPost.getCommentCount()));
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

            @Override
            public void onItemNicknameLongClick(String mention) {
                Log.i(TAG, "onItemNicknameLongClick:mention:" + mention);
                mEtCommentInput.append(mention + " ");
                // mEtCommentInput.setSelection(mEtCommentInput.getText().length());
                mEtCommentInput.requestFocus(mEtCommentInput.getText().length());
                CommonUtil.showKeyboard(getActivity());
            }
        });
        mRVCommentList.setAdapter(mAdapter);
    }

    private void showCommentMentionGuide() {
        Log.d(TAG, "showCommentMentionGuide:comments:" + mComments);

        CommentListAdapter.CommentViewHolder holder =
                (CommentListAdapter.CommentViewHolder) mRVCommentList.findViewHolderForLayoutPosition(0);

        Log.d(TAG, "showCommentMentionGuide:holder:" + holder);

        int screenWidth = CommonUtil.getScreenWidth(getContext());
        mTooltipView = Tooltip.make(getContext(),
                new Tooltip.Builder(101)
                        .withStyleId(R.style.ToolTipLayoutCustomStyle)
                        .anchor(holder.vNicknameOverlay, Tooltip.Gravity.TOP)
                        .closePolicy(new Tooltip.ClosePolicy()
                                .insidePolicy(true, false)
                                .outsidePolicy(false, false), 0)
                        .activateDelay(800)
                        .showDelay(3000)
                        .text("댓글의 아이디를 길게 누르면 멘션 기능을 사용할 수 있어요")
                        .maxWidth((int) Math.round(screenWidth * 0.7))
                        .withArrow(true)
                        .withOverlay(true)
                        // .typeface(mYourCustomFont)
                        .floatingAnimation(Tooltip.AnimationBuilder.SLOW)
                        .withCallback(new Tooltip.Callback() {
                            @Override
                            public void onTooltipClose(Tooltip.TooltipView tooltipView, boolean b, boolean b1) {
                                tooltipView.remove();
                            }

                            @Override
                            public void onTooltipFailed(Tooltip.TooltipView tooltipView) {

                            }

                            @Override
                            public void onTooltipShown(Tooltip.TooltipView tooltipView) {
                                ConfigManager.getInstance(getContext()).setGuideShownMentionComment(true);
                            }

                            @Override
                            public void onTooltipHidden(Tooltip.TooltipView tooltipView) {

                            }
                        })
                        .build()
        );
        mTooltipView.show();
    }

    @OnClick({R.id.profile_image, R.id.nickname_text})
    void showProfile() {
        Log.i(TAG, "showProfile");
        startProfileFragmentByUserId(mTargetPost.getUser().getId());
    }

    @Override
    public void onHashTagClicked(String hashTag) {
        Log.i(TAG, "onHashTagClicked:" + hashTag);
        startHashtagFragment(hashTag);
    }

    @Override
    public void onAtSignClicked(String atSign) {
        Log.i(TAG, "onAtSignClicked:" + atSign);
        startProfileFragmentByNickname(atSign);
    }

    // TODO: 문제는 ArticleFragment가 ChildActivity에 붙었을 때의 상황이다.
    private void startHashtagFragment(String hashtag) {
        HashtagFragment hashtagFragment = HashtagFragment.newInstance(hashtag);
        RootFragment homeRootFragment = getRootFragment();
        homeRootFragment.add(hashtagFragment);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));
        switch (requestCode) {
            case WRITE_ACTIVITY:
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
                break;
            case ORDER_SUMMARY_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    CommonUtil.showSnackbar(getActivity(), "주문을 완료하였습니다.");
                }
                if (resultCode == RESULT_CANCELED) {
                }
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (isPrimaryFragment()) {
            inflater.inflate(R.menu.menu_fragment_article, menu);

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
        Log.i(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void finish() {
        Log.i(TAG, "finish");
        getActivity().onBackPressed();
        // getRootFragment().pop();
        // getActivity().getSupportFragmentManager().popBackStack();
        // getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }
}