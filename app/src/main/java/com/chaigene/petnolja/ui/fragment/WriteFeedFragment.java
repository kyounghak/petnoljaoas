package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.hashtag.AtSignHelper;
import com.chaigene.petnolja.hashtag.HashTagHelper;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.activity.WriteActivity;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;
import static com.chaigene.petnolja.model.FIRUser.TYPE_ADMIN;
import static com.chaigene.petnolja.model.FIRUser.TYPE_MAKER;
import static com.chaigene.petnolja.model.FIRUser.TYPE_MANAGER;
import static com.chaigene.petnolja.model.FIRUser.TYPE_USER;
import static com.chaigene.petnolja.ui.activity.WriteActivity.ACTION_STATUS_INSERT;
import static com.chaigene.petnolja.ui.activity.WriteActivity.ACTION_STATUS_MODIFY;

public class WriteFeedFragment extends BaseFragment implements HashTagHelper.OnHashTagClickListener, AtSignHelper.OnAtSignClickListener {
    public static final String TAG = "WriteFeedFragment";

    @BindView(R.id.photo_1)
    ImageView mIvPhoto1;

    @BindView(R.id.photo_2)
    ImageView mIvPhoto2;

    @BindView(R.id.photo_3)
    ImageView mIvPhoto3;

    @BindView(R.id.photo_4)
    ImageView mIvPhoto4;

    @BindView(R.id.photo_5)
    ImageView mIvPhoto5;

    @BindView(R.id.content_input)
    EditText mEtContentInput;

    @BindView(R.id.trade_tab)
    View mVTradeTab;

    @BindView(R.id.trade_switch)
    SwitchCompat mSwTrade;

    private HashTagHelper mHashTagHelper;
    private AtSignHelper mAtSignHelper;

    public static WriteFeedFragment newInstance() {
        WriteFeedFragment fragment = new WriteFeedFragment();

        // Bundle args = new Bundle();
        // fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_write_feed, container, false);
        ButterKnife.bind(this, mView);

        initView();

        return mView;
    }

    // 글 수정일 때 해야하는 작업은 이미지 다운로드 및 콘텐츠 삽입하기
    protected void initView() {

        switch (getUserType()) {
            case TYPE_USER:
                mVTradeTab.setVisibility(View.GONE);
                break;
            case TYPE_MAKER:
            case TYPE_MANAGER:
            case TYPE_ADMIN:
                mVTradeTab.setVisibility(View.VISIBLE);
                break;
            default:
                mVTradeTab.setVisibility(View.GONE);
                break;
        }

        if (getActionStatus() == ACTION_STATUS_INSERT) {
            int index = 0;
            for (Uri imageUri : getImageUris()) {
                switch (index) {
                    case 0:
                        mIvPhoto1.setImageURI(imageUri);
                        break;
                    case 1:
                        mIvPhoto2.setImageURI(imageUri);
                        break;
                    case 2:
                        mIvPhoto3.setImageURI(imageUri);
                        break;
                    case 3:
                        mIvPhoto4.setImageURI(imageUri);
                        break;
                    case 4:
                        mIvPhoto5.setImageURI(imageUri);
                        break;
                    default:
                        // It should never happen.
                        break;
                }
                index++;
            }
        }

        if (getActionStatus() == ACTION_STATUS_MODIFY) {
            // mEtContentInput.setText(getContent());

            // 이미지 다운로드
            List<String> photos = getPost().getPhotosList();
            int index = 0;
            ImageView imageView = null;
            for (String filename : photos) {
                switch (index) {
                    case 0:
                        imageView = mIvPhoto1;
                        break;
                    case 1:
                        imageView = mIvPhoto2;
                        break;
                    case 2:
                        imageView = mIvPhoto3;
                        break;
                    case 3:
                        imageView = mIvPhoto4;
                        break;
                    case 4:
                        imageView = mIvPhoto5;
                        break;
                }
                downloadPhoto(imageView, getPost().getKey(), filename);
                index++;
            }
        }

        mHashTagHelper = CommonUtil.createDefaultHashTagHelper(getContext(), this);
        mHashTagHelper.handle(mEtContentInput);
        mAtSignHelper = CommonUtil.createDefaultAtSignHelper(getContext(), this);
        mAtSignHelper.handle(mEtContentInput);

        mSwTrade.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int articleType = !isChecked ? ARTICLE_TYPE_FEED : ARTICLE_TYPE_TALENT;
            setArticleType(articleType);
        });
    }

    @Override
    public void onHashTagClicked(String hashTag) {
        Log.i(TAG, "onHashTagClicked:" + hashTag);
        // startHashtagFragment(hashTag);
    }

    @Override
    public void onAtSignClicked(String atSign) {
        Log.i(TAG, "onAtSignClicked:" + atSign);
        // startHashtagFragment(atSign);
    }

    private void downloadPhoto(@NonNull ImageView view,
                               @NonNull String postId,
                               @NonNull String filename) {
        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId).child(filename);
        GlideManager.loadImage(postsRef, view, GlideManager.SCALE_TYPE_FIT_CENTER);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.i(TAG, "onHiddenChanged:hidden:" + hidden);
        if (!hidden) return;

        String content = mEtContentInput.getText().toString();
        setContent(content);

        List<String> hashtags = mHashTagHelper.getAllHashTags();
        setHashtags(hashtags);

        List<String> mentions = mAtSignHelper.getAllAtSigns();
        setMentions(mentions);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG, "setUserVisibleHint:isVisibleToUser:" + isVisibleToUser);
    }

    // 프래그먼트의 onPause 및 onResume은 액티비티의 onPause, onResume이 호출될 때만 발생된다.
    // Ref: https://stackoverflow.com/a/16252923/4729203
    @Override
    public void onPause() {
        super.onPause();
        String content = mEtContentInput.getText().toString();
        setContent(content);

        List<String> hashtags = mHashTagHelper.getAllHashTags();
        setHashtags(hashtags);
    }

    @Override
    public void onResume() {
        super.onResume();
        mEtContentInput.setText(getContent());
        if (getArticleType() == ARTICLE_TYPE_FEED) {
            mSwTrade.setChecked(false);
        } else if (getArticleType() == ARTICLE_TYPE_TALENT) {
            mSwTrade.setChecked(true);
        }
    }

    public void setActionStatus(int actionStatus) {
        ((WriteActivity) getActivity()).setActionStatus(actionStatus);
    }

    public int getActionStatus() {
        return ((WriteActivity) getActivity()).getActionStatus();
    }

    private void setArticleType(int articleType) {
        ((WriteActivity) getActivity()).setArticleType(articleType);
    }

    private int getArticleType() {
        return ((WriteActivity) getActivity()).getArticleType();
    }

    private void setUserType(int userType) {
        ((WriteActivity) getActivity()).setUserType(userType);
    }

    private int getUserType() {
        return ((WriteActivity) getActivity()).getUserType();
    }

    public void setImageUris(List<Uri> imageUris) {
        ((WriteActivity) getActivity()).setImageUris(imageUris);
    }

    public List<Uri> getImageUris() {
        return ((WriteActivity) getActivity()).getImageUris();
    }

    public void setContent(String content) {
        ((WriteActivity) getActivity()).setContent(content);
    }

    public Post getPost() {
        return ((WriteActivity) getActivity()).getPost();
    }

    public void setPost(Post post) {
        ((WriteActivity) getActivity()).setPost(post);
    }

    public String getContent() {
        return ((WriteActivity) getActivity()).getContent();
    }

    public void setHashtags(List<String> hashtags) {
        ((WriteActivity) getActivity()).setHashtags(hashtags);
    }

    public List<String> getHashtags() {
        return ((WriteActivity) getActivity()).getHashtags();
    }

    public void setMentions(List<String> mentions) {
        ((WriteActivity) getActivity()).setMentions(mentions);
    }

    public List<String> getMentions() {
        return ((WriteActivity) getActivity()).getMentions();
    }

    public void showWriteFeedFragment() {
        Log.i(TAG, "showFeedTab");
        ((WriteActivity) getActivity()).showWriteFeedFragment();
    }

    public void showWriteTalentFragment() {
        Log.i(TAG, "showTalentTab");
        ((WriteActivity) getActivity()).showWriteTalentFragment();
    }

    private void insertArticle() {
        Log.i(TAG, "insertArticle:article_type:" + getArticleType());
        CommonUtil.clearFocus(mEtContentInput);
        CommonUtil.hideKeyboard(getActivity());

        String content = mEtContentInput.getText().toString();
        List<String> hashtags = mHashTagHelper.getAllHashTags();
        List<String> mentions = mAtSignHelper.getAllAtSigns();
        ((WriteActivity) getActivity()).updateArticle(getArticleType(), getImageUris(), content, hashtags, mentions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d/resultCode:%d", requestCode, resultCode));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getToolbar().getMenu().clear();
        inflater.inflate(R.menu.menu_fragment_write_feed, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_next: {
                // TODO: 일상글이면 글등록을 재능글이면 판매프래그먼트로 넘어간다.
                if (getArticleType() == ARTICLE_TYPE_FEED) {
                    insertArticle();
                } else if (getArticleType() == ARTICLE_TYPE_TALENT) {
                    Log.d(TAG, "onOptionsItemSelected:ARTICLE_TYPE_TALENT:content:" + getContent());
                    showWriteTalentFragment();
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}