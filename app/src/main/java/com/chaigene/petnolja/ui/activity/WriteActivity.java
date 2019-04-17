package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.chaigene.petnolja.model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.dialog.DialogConfirmFragment;
import com.chaigene.petnolja.ui.fragment.WriteFeedFragment;
import com.chaigene.petnolja.ui.fragment.WriteTalentFragment;
import com.chaigene.petnolja.util.OldArticleUtil;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;
import static com.chaigene.petnolja.Constants.EXTRA_ACTION_STATUS;
import static com.chaigene.petnolja.Constants.EXTRA_IMAGE_URIS;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_POST_ID;

// TODO: Fragment를 사용하거나 특정 EditText 항목의 Visibility를 변경하는 식으로 액티비티를 구성해야 한다.
// 태그를 사용하기 위해서 chips-android 라이브러리를 커스터마이징 한다.
// 액티비티를 실행함과 동시에 Intent를 통해서 첨부할 이미지 Uri 리스트를 넘겨받아야 한다.
// 글 올리기를 클릭했을시 로딩을 보여주며 Foreground에서 이미지를 먼저 업로드하고 글을 다음에 업로드한다.
// 액티비티가 시작된 뒤에 어떤 글을 쓸 것인지를 물어보는게 좋지 않을까?
public class WriteActivity extends BaseActivity {
    public static final String TAG = "WriteActivity";

    private static final String FRAGMENT_TAG_FEED = String.valueOf(ARTICLE_TYPE_FEED);
    private static final String FRAGMENT_TAG_TALENT = String.valueOf(ARTICLE_TYPE_TALENT);

    public static final int ACTION_STATUS_INSERT = 0;
    public static final int ACTION_STATUS_MODIFY = 1;

    WriteFeedFragment mWriteFeedFragment;
    WriteTalentFragment mWriteTalentFragment;

    private int mArticleType;
    private int mUserType;
    private List<Uri> mImageUris;
    private String mContent;
    private List<String> mHashtags;
    private List<String> mMentions;
    private int mProductType;
    private String mProductTitle;
    private String mProductPrice;
    private String mShippingPrice;
    private String mProductServices;

    private String mPostId;
    private Post mPost;

    private int mActionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);
        ButterKnife.bind(this);

        // Initial type은 '일상'이다.
        if (mActionStatus == ACTION_STATUS_INSERT) {
            mArticleType = ARTICLE_TYPE_FEED;
            showLoadingDialog();
            UserUtil.getUser(AuthManager.getUserId(), true).continueWith((Continuation<User, Void>) task -> {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    finish();
                    return null;
                }
                User myUser = task.getResult();
                setUserType(myUser.getType());
                initView();
                return null;
            });
        }

        if (mActionStatus == ACTION_STATUS_MODIFY) {
            showLoadingDialog();
            UserUtil.getUser(AuthManager.getUserId(), true).continueWithTask((Continuation<User, Task<Void>>) task -> {
                if (!task.isSuccessful()) {
                    dismissDialog();
                    Toast.makeText(getApplicationContext(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    finish();
                    return Tasks.forResult(null);
                }
                User myUser = task.getResult();
                setUserType(myUser.getType());
                return asyncTask();
            }).continueWith((Continuation<Void, Void>) task -> {
                dismissDialog();
                if (!task.isSuccessful()) {
                    finish();
                    return null;
                }
                setArticleType(getPost().getType());
                setContent(getPost().getContent());
                setProductTitle(getPost().getProductTitle());
                setProductPrice(getPost().getProductPrice());
                setShippingPrice(getPost().getShippingPrice());
                setProductServices(getPost().getProductServices());
                initView();
                return null;
            });
        }
    }

    // 필드 여부에 따라서 새 글인지 수정인지 구분해준다.
    @Override
    @SuppressWarnings("unchecked")
    protected void readIntent() {
        super.readIntent();
        mActionStatus = getIntent().getIntExtra(EXTRA_ACTION_STATUS, 0);
        if (mActionStatus == ACTION_STATUS_INSERT) {
            mImageUris = (ArrayList<Uri>) getIntent().getSerializableExtra(EXTRA_IMAGE_URIS);
        }
        if (mActionStatus == ACTION_STATUS_MODIFY) {
            mPostId = getIntent().getStringExtra(EXTRA_TARGET_POST_ID);
        }
        Log.i(TAG, "readIntent:action_status:" + mActionStatus);
    }

    private Task<Void> asyncTask() {
        return OldArticleUtil.getPost(mPostId, true).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            mPost = task.getResult();
            return null;
        });
    }

    @Override
    protected void initView() {
        super.initView();

        // 최초로 보여질 프래그먼트
        // 글 수정이 아니라면 uri 리스트만 넘겨주면 된다.
        showWriteFeedFragment();
    }

    // TODO: 다른 툴바를 사용해야 한다.
    // 현재 액티비티의 툴바에서는 타이틀 영역이 Spinner로 이루어져있어야 한다.
    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (mActionStatus == ACTION_STATUS_INSERT) {
            setToolbarTitle("새 게시물");
        }
        if (mActionStatus == ACTION_STATUS_MODIFY) {
            setToolbarTitle("게시물 수정");
        }
        setupBackButton();
    }

    private void setupBackButton() {
        TextView toolbarBackText = findViewById(R.id.toolbar_back_text);
        toolbarBackText.setOnClickListener(v -> {
            Log.d(TAG, "setupToolbar:back_button_pressed");
            if (find(FRAGMENT_TAG_FEED) != null && find(FRAGMENT_TAG_FEED).isVisible()) {
                manualFinish();
            }

            if (find(FRAGMENT_TAG_TALENT) != null && find(FRAGMENT_TAG_TALENT).isVisible()) {
                showWriteFeedFragment();
            }
        });
    }

    public void showWriteFeedFragment() {
        Log.i(TAG, "showFeedTab");

        Fragment writeFeedFragment = find(FRAGMENT_TAG_FEED);
        if (writeFeedFragment == null) {
            add(WriteFeedFragment.newInstance(), FRAGMENT_TAG_FEED);
        } else {
            show(writeFeedFragment);
        }

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment != null) hide(talentFragment);
    }

    public void showWriteTalentFragment() {
        Log.i(TAG, "showTalentTab");

        Fragment writeTalentFragment = find(FRAGMENT_TAG_TALENT);
        if (writeTalentFragment == null) {
            add(WriteTalentFragment.newInstance(), FRAGMENT_TAG_TALENT);
        } else {
            show(writeTalentFragment);
        }

        Fragment feedFragment = find(FRAGMENT_TAG_FEED);
        if (feedFragment != null) hide(feedFragment);
    }

    private Fragment find(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        Log.i(TAG, "find:tag:" + tag + "/result:" + fragment);
        return fragment;
    }

    protected void add(Fragment fragment, String tag) {
        Log.i(TAG, "add:" + fragment + " " + tag);

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commit();
    }

    public void pop() {
        Log.i(TAG, "pop");
        getSupportFragmentManager().popBackStack();
    }

    private void show(Fragment fragment) {
        Log.i(TAG, "show:" + fragment);

        getSupportFragmentManager()
                .beginTransaction()
                .show(fragment)
                .commit();
    }

    private void hide(Fragment fragment) {
        Log.i(TAG, "hide:" + fragment);

        getSupportFragmentManager()
                .beginTransaction()
                .hide(fragment)
                .commit();
    }

    public int getActionStatus() {
        return mActionStatus;
    }

    public void setActionStatus(int actionStatus) {
        this.mActionStatus = actionStatus;
    }

    public void setArticleType(int articleType) {
        Log.i(TAG, "setArticleType:" + articleType);
        this.mArticleType = articleType;
    }

    public int getArticleType() {
        return mArticleType;
    }

    public int getUserType() {
        return mUserType;
    }

    public void setUserType(int userType) {
        this.mUserType = userType;
    }

    public void setImageUris(List<Uri> imageUris) {
        this.mImageUris = imageUris;
    }

    public List<Uri> getImageUris() {
        return mImageUris;
    }

    public Post getPost() {
        return mPost;
    }

    public void setPost(Post post) {
        this.mPost = post;
    }

    public void setContent(String content) {
        Log.i(TAG, "setContent:content:" + content);
        this.mContent = content;
    }

    public String getContent() {
        return mContent;
    }

    public void setHashtags(List<String> hashtags) {
        this.mHashtags = hashtags;
    }

    public List<String> getHashtags() {
        return mHashtags;
    }

    public void setMentions(List<String> mentions) {
        this.mMentions = mentions;
    }

    public List<String> getMentions() {
        return mMentions;
    }

    public void setProductType(int productType) {
        this.mProductType = productType;
    }

    public int getProductType() {
        return mProductType;
    }

    public void setProductTitle(String productTitle) {
        this.mProductTitle = productTitle;
    }

    public String getProductTitle() {
        return mProductTitle;
    }

    public void setProductPrice(String productPrice) {
        this.mProductPrice = productPrice;
    }

    public String getProductPrice() {
        return mProductPrice;
    }

    public void setShippingPrice(String shippingPrice) {
        this.mShippingPrice = shippingPrice;
    }

    public String getShippingPrice() {
        return mShippingPrice;
    }

    public void setProductServices(String productServices) {
        this.mProductServices = productServices;
    }

    public String getProductServices() {
        return mProductServices;
    }

    @SuppressWarnings("ConstantConditions")
    public void updateArticle(int articleType,
                              List<Uri> imageUris,
                              String content,
                              List<String> hashtags,
                              List<String> mentions) {
        Log.i(TAG, "updateArticle");

        Task<Void> task = null;
        if (mActionStatus == ACTION_STATUS_INSERT) {
            task = OldArticleUtil.insert(articleType, imageUris, content, hashtags, mentions);
        }

        if (mActionStatus == ACTION_STATUS_MODIFY) {
            task = OldArticleUtil.modify(getPost().getKey(), articleType, content, hashtags, mentions);
        }

        showLoadingDialog();
        task.continueWith((Continuation<Void, Task<Void>>) task1 -> {
            dismissDialog();
            if (!task1.isSuccessful()) {
                Log.w(TAG, "insert:ERROR");
                // if (task.getException().getMessage().equals("Firebase Database error: Permission denied")) {
                // }
                CommonUtil.showSnackbar(WriteActivity.this, R.string.msg_failed_to_upload_article);
                return null;
            }
            // TODO: 종료하고 홈타임라인으로 이동하는 것이 좋을 것 같다.
            Intent in = new Intent();
            in.putExtra(EXTRA_TARGET_POST_ID, mPostId);
            setResult(RESULT_OK, in);
            finish();
            return null;
        });
    }

    @SuppressWarnings("ConstantConditions")
    public void updateArticle(int articleType,
                              List<Uri> imageUris,
                              String content,
                              List<String> hashtags,
                              List<String> mentions,
                              int productType,
                              String productTitle,
                              String productPrice,
                              String shippingPrice,
                              String productServices) {
        Log.i(TAG, "updateArticle");

        Task<Void> task = null;
        if (mActionStatus == ACTION_STATUS_INSERT) {
            task = OldArticleUtil.insert(
                    articleType,
                    imageUris,
                    content,
                    hashtags,
                    mentions,
                    productType,
                    productTitle,
                    productPrice,
                    shippingPrice,
                    productServices
            );
        }

        if (mActionStatus == ACTION_STATUS_MODIFY) {
            task = OldArticleUtil.modify(
                    getPost().getKey(),
                    articleType,
                    content,
                    hashtags,
                    mentions,
                    productType,
                    productTitle,
                    productPrice,
                    shippingPrice,
                    productServices
            );
        }

        showLoadingDialog();
        task.continueWith((Continuation<Void, Task<Void>>) task1 -> {
            dismissDialog();
            if (!task1.isSuccessful()) {
                Log.w(TAG, "updateArticle:ERROR", task1.getException());
                // if (task.getException().getMessage().equals("Firebase Database error: Permission denied")) {
                // }
                CommonUtil.showSnackbar(WriteActivity.this, R.string.msg_failed_to_upload_article);
                return null;
            }
            // TODO: 종료하고 홈타임라인으로 이동하는 것이 좋을 것 같다.
            Intent in = new Intent();
            in.putExtra(EXTRA_TARGET_POST_ID, mPostId);
            setResult(RESULT_OK, in);
            finish();
            return null;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d/resultCode:%d", requestCode, resultCode));
    }

    // TODO: 뒤로가기 구현
    @Override
    public void onBackPressed() {
        if (find(FRAGMENT_TAG_FEED) != null && find(FRAGMENT_TAG_FEED).isVisible()) {
            manualFinish();
        }

        if (find(FRAGMENT_TAG_TALENT) != null && find(FRAGMENT_TAG_TALENT).isVisible()) {
            showWriteFeedFragment();
        }
    }

    private void manualFinish() {
        DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                "이전 화면으로 돌아가면 현재 작업 내용은 복구할 수 없습니다. 돌아가시겠습니까?",
                new DialogConfirmFragment.OnSelectListener() {
                    @Override
                    public void onConfirm() {
                        super.onConfirm();
                        setResult(RESULT_CANCELED);
                        finish();
                    }

                    @Override
                    public void onDeny() {
                        super.onDeny();
                    }
                }
        );
        confirmDialog.show(getSupportFragmentManager(), null);
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_write, menu);
        return true;
    }*/

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_confirm:
                // updateArticle();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    /*public static Intent createIntent(Context context, int actionStatus) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, WriteActivity.class);
        intent.putExtra(ACTION_STATUS, actionStatus);
        return intent;
    }*/

    // 수정이 아닌 처음 글을 등록할 때 호출해야 하는 메서드.
    public static Intent createWriteIntent(@NonNull Context context, @NonNull ArrayList<Uri> imageUris) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, WriteActivity.class);
        intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_INSERT);
        intent.putExtra(EXTRA_IMAGE_URIS, imageUris);
        return intent;
    }

    // TODO: 글을 수정 할 때는 로컬에 이미지가 존재하지 않기 때문에(수정할 수도 없음)
    // Glide를 통해 다운로드 해줘야 한다.
    // 그냥 Post 객체 자체를 넘겨받으면 간편해진다. => (X) 절대 하면 안됨
    public static Intent createModifyIntent(@NonNull Context context, @NonNull String postId) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, WriteActivity.class);
        intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_MODIFY);
        intent.putExtra(EXTRA_TARGET_POST_ID, postId);
        return intent;
    }
}