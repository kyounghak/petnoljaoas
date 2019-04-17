package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.ui.view.ExtendedViewPager;
import com.chaigene.petnolja.ui.view.TouchImageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.EXTRA_TARGET_PATHS;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_POSITION;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_REGIONS;

public class ImageDetailActivity extends BaseActivity {
    public static final String TAG = "ImageDetailActivity";

    @BindView(R.id.view_pager)
    ExtendedViewPager mViewPager;

    List<String> mTargetRegions;
    List<String> mTargetPaths;
    int mTargetPosition;

    public static Intent createIntent(Context context,
                                      ArrayList<String> targetRegions,
                                      ArrayList<String> targetPaths,
                                      int targetPosition) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent(c, ImageDetailActivity.class);
        intent.putStringArrayListExtra(EXTRA_TARGET_REGIONS, targetRegions);
        intent.putStringArrayListExtra(EXTRA_TARGET_PATHS, targetPaths);
        intent.putExtra(EXTRA_TARGET_POSITION, targetPosition);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);
        ButterKnife.bind(this);
        mViewPager.setAdapter(new TouchImageAdapter());
        mViewPager.setCurrentItem(mTargetPosition);
    }

    @Override
    protected void readIntent() {
        super.readIntent();
        mTargetRegions = getIntent().getStringArrayListExtra(EXTRA_TARGET_REGIONS);
        mTargetPaths = getIntent().getStringArrayListExtra(EXTRA_TARGET_PATHS);
        mTargetPosition = getIntent().getIntExtra(EXTRA_TARGET_POSITION, 0);
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle(R.string.title_activity_image_detail);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private class TouchImageAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mTargetPaths.size();
        }

        @Override
        public View instantiateItem(ViewGroup container, final int position) {
            TouchImageView img = new TouchImageView(container.getContext());
            // img.setBackgroundResource(R.color.material_grey_900);
            img.setImageResource(R.color.material_grey_900);
            container.addView(img, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            downloadPhoto(img, position).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(@NonNull Task<Void> task) throws Exception {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "instantiateItem:ERROR:position:" + position, task.getException());
                    }
                    Log.d(TAG, "instantiateItem:SUCCESS:position:" + position);
                    return null;
                }
            });
            return img;
        }

        private Task<Void> downloadPhoto(TouchImageView imageView, int position) {
            StorageReference ref = StorageManager.getRef(mTargetPaths.get(position));
            return GlideManager.loadImage(ref, imageView);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}