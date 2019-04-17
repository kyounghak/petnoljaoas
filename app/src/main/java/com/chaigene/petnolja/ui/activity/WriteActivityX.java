package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.ArrayRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.AppCompatSpinner;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.hashtag.AtSignHelper;
import com.chaigene.petnolja.hashtag.HashTagHelper;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.ui.dialog.DialogListFragment;
import com.chaigene.petnolja.util.OldArticleUtil;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;

// TODO: Fragment를 사용하거나 특정 EditText 항목의 Visibility를 변경하는 식으로 액티비티를 구성해야 한다.
// 태그를 사용하기 위해서 chips-android 라이브러리를 커스터마이징 한다.
// 액티비티를 실행함과 동시에 Intent를 통해서 첨부할 이미지 Uri 리스트를 넘겨받아야 한다.
// 글 올리기를 클릭했을시 로딩을 보여주며 Foreground에서 이미지를 먼저 업로드하고 글을 다음에 업로드한다.
// 액티비티가 시작된 뒤에 어떤 글을 쓸 것인지를 물어보는게 좋지 않을까?

public class WriteActivityX extends BaseActivity {
    public static final String TAG = "WriteActivity";

    @BindView(R.id.feed_content_input)
    EditText mEtContentInput;

    @BindView(R.id.main_image_thumb)
    ImageView mIVMainImageThumb;

    AppCompatSpinner mSpinner;

    private int mArticleType;
    @Deprecated
    private boolean mIsTalent;

    private ArrayList<Uri> mImageUris;
    private Uri mMainImageUri;

    // Modify
    private boolean mIsModify;
    private Post mPost;

    // 여러개의 이미지가 선택되었는지를 반환한다.
    private boolean mIsMultipleSelection;

    private HashTagHelper mHashTagHelper;
    private AtSignHelper mAtSignHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_x);
        ButterKnife.bind(this);

        readIntent();

        mArticleType = ARTICLE_TYPE_FEED;
        DialogListFragment dialogFragment = DialogListFragment.newInstance(
                new String[]{"일상", "재능"},
                new DialogListFragment.OnItemSelectListener() {
                    @Override
                    public void onSelect(int index) {
                        mSpinner.setSelection(index, true);
                    }
                }
        );
        dialogFragment.show(getSupportFragmentManager(), null);

        initView();
    }

    // 필드 여부에 따라서 새 글인지 수정인지 구분해준다.
    @Override
    protected void readIntent() {
        mIsModify = getIntent().getBooleanExtra(Constants.EXTRA_IS_MODIFY, false);
        if (!mIsModify) {
            mImageUris = (ArrayList<Uri>) getIntent().getSerializableExtra(Constants.EXTRA_IMAGE_URIS);
            mMainImageUri = mImageUris.iterator().next();
        } else {
            mPost = (Post) getIntent().getSerializableExtra(Constants.EXTRA_TARGET_POST);
        }
    }

    public static Intent createWriteIntent(@NonNull Context context, @NonNull ArrayList<Uri> imageUris) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, WriteActivityX.class);
        intent.putExtra(Constants.EXTRA_IMAGE_URIS, imageUris);
        return intent;
    }

    // TODO: 글을 수정 할 때는 로컬에 이미지가 존재하지 않기 때문에(수정할 수도 없음)
    // Glide를 통해 다운로드 해줘야 한다.
    // 그냥 Post 객체 자체를 넘겨받으면 간편해진다.
    public static Intent createModifyIntent(@NonNull Context context, @NonNull Post post) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, WriteActivityX.class);
        intent.putExtra(Constants.EXTRA_IS_MODIFY, true);
        intent.putExtra(Constants.EXTRA_TARGET_POST, post);
        return intent;
    }

    // TODO: 다른 툴바를 사용해야 한다.
    // 현재 액티비티의 툴바에서는 타이틀 영역이 Spinner로 이루어져있어야 한다.
    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        // setSupportActionBar(mToolbar);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // mActionBar.setHomeButtonEnabled(true);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        setupSpinner();
        setupBackButton();
    }

    private void setupBackButton() {
        TextView toolbarBackText = findViewById(R.id.toolbar_back_text);
        toolbarBackText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "setupToolbar:back_button_pressed");
            }
        });
    }

    private void setupSpinner() {
        mSpinner = (AppCompatSpinner) getToolbar().findViewById(R.id.toolbar_title_spinner);
        final SpinnerAdapter<CharSequence> adapter =
                SpinnerAdapter.createFromResource(
                        this,
                        R.layout.item_spinner_toolbar,
                        R.array.article_types
                );
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown_toolbar);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            final String TAG = "OnItemSelectedListener";

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.setSelectedPosition(position);
                switch (position) {
                    case 0:
                        Log.d(TAG, "onItemSelected:ARTICLE_TYPE_FEED");
                        mArticleType = ARTICLE_TYPE_FEED;
                        break;
                    case 1:
                        Log.d(TAG, "onItemSelected:ARTICLE_TYPE_TALENT");
                        mArticleType = ARTICLE_TYPE_TALENT;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // 글 수정일 때 해야하는 작업은 이미지 다운로드 및 콘텐츠 삽입하기
    @Override
    protected void initView() {
        if (!mIsModify) {
            mIVMainImageThumb.setImageURI(mMainImageUri);
        } else {
            mEtContentInput.setText(mPost.getContent());
            List<String> photos = mPost.getPhotosList();
            int index = 0;
            for (String filename : photos) {
                downloadPhoto(index, filename);
                index++;
            }
        }
        mHashTagHelper = CommonUtil.createDefaultHashTagHelper(this, null);
        mHashTagHelper.handle(mEtContentInput);
        mAtSignHelper = CommonUtil.createDefaultAtSignHelper(this, null);
        mAtSignHelper.handle(mEtContentInput);
    }

    private void initFeedView() {

    }

    private void initTalentView() {

    }

    private void releaseFeedView() {

    }

    private void releaseTalentView() {

    }

    // TODO: 몇번째 인덱스냐에 따라서 다른 이미지뷰에 삽입해야 한다.
    private void downloadPhoto(int index, String filename) {
        switch (index) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d/resultCode:%d", requestCode, resultCode));
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_write, menu);

        /*getToolbar().setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_confirm:
                insertArticle();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void insertArticle() {
        showLoadingDialog();
        CommonUtil.clearFocus(mEtContentInput);
        CommonUtil.hideKeyboard(this);
        String content = mEtContentInput.getText().toString();
        List<String> hashtags = mHashTagHelper.getAllHashTags();
        List<String> mentions = mAtSignHelper.getAllAtSigns();

        OldArticleUtil.insert(mArticleType, mImageUris, content, hashtags, mentions).continueWith(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "insert:ERROR");
                    if (task.getException().getMessage().equals("Firebase Database error: Permission denied")) {
                        CommonUtil.showSnackbar(WriteActivityX.this, R.string.msg_failed_to_upload_article);
                    }
                    dismissDialog();
                    return null;
                }
                Log.d(TAG, "insert:SUCCESS");
                dismissDialog();
                // TODO: 종료하고 홈타임라인으로 이동하는 것이 좋을 것 같다.
                setResult(RESULT_OK);
                finish();
                return null;
            }
        });
    }

    private static class SpinnerAdapter<T> extends ArrayAdapter<CharSequence> {
        public static final String TAG = "SpinnerAdapter";

        private Context mContext;
        private LayoutInflater mInflater;
        private int mDropdownResId;

        private List<CharSequence> mDataItems;
        private int mSelectedPosition;

        public static SpinnerAdapter<CharSequence> createFromResource(
                @NonNull Context context,
                @LayoutRes int textViewResId,
                @ArrayRes int textArrayResId) {
            CharSequence[] strings = context.getResources().getTextArray(textArrayResId);
            return new SpinnerAdapter<>(context, textViewResId, strings);
        }

        SpinnerAdapter(@NonNull Context context, @LayoutRes int resource, CharSequence[] objects) {
            super(context, resource, objects);
            mContext = context.getApplicationContext();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mDataItems = new ArrayList<>();
            mDataItems.addAll(Arrays.asList(objects));
            mSelectedPosition = 0;
        }

        public void setDropDownViewResource(@LayoutRes int resource) {
            mDropdownResId = resource;
        }

        @Override
        public void addAll(Collection<? extends CharSequence> collection) {
            super.addAll(collection);
            mDataItems.addAll(mDataItems);
        }

        @Override
        public CharSequence getItem(int position) {
            return mDataItems.get(position);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            Log.i(TAG, "getDropDownView:position:" + position);

            View row = mInflater.inflate(mDropdownResId, null);

            TextView tvTitle = (TextView) row.findViewById(R.id.item_title);
            tvTitle.setText(getItem(position));

            if (position == mSelectedPosition)
                tvTitle.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));

            return row;
        }

        public void setSelectedPosition(int position) {
            Log.i(TAG, "setSelectedPosition:position:" + position);
            mSelectedPosition = position;
        }
    }
}