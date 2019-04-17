package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivityX extends BaseActivity {
    public static final String TAG = "SettingsActivity";

    @BindView(R.id.feed_content_input)
    EditText mETContentInput;

    @BindView(R.id.main_image_thumb)
    ImageView mIVMainImageThumb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        // mImageUris = (ArrayList<Uri>) getIntent().getSerializableExtra(Constants.EXTRA_IMAGE_URIS);
        // mMainImageUri = mImageUris.iterator().next();

        initView();
    }

    @Override
    protected void setupToolbar() {
        setSupportActionBar(mToolbar);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // mActionBar.setHomeButtonEnabled(true);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setSystemBar(true);

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

    @Override
    protected void initView() {
        // mIvMainImageThumb.setImageURI(mMainImageUri);
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
                // insert();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static Intent createIntent(@NonNull Context context, @NonNull ArrayList<Uri> imageUris) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, SettingsActivityX.class);
        intent.putExtra(Constants.EXTRA_IMAGE_URIS, imageUris);
        return intent;
    }
}