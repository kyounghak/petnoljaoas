package com.chaigene.petnolja.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.image.GPUImageFilterTools;
import com.chaigene.petnolja.image.GPUImageFilterTools.FilterAdjuster;
import com.chaigene.petnolja.image.GPUImageFilterTools.FilterType;
import com.chaigene.petnolja.util.CommonUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class ImageFilterActivity extends BaseActivity implements
        SeekBar.OnSeekBarChangeListener,
        GPUImageView.OnPictureSavedListener {

    public static final String TAG = "ImageFilterActivity";

    public static final String IMAGE_OUTPUT_FOLDER_NAME = "Panda";

    private GPUImageFilter mFilter;
    private FilterAdjuster mFilterAdjuster;

    @BindView(R.id.gpuimage)
    GPUImageView mGPUImageView;

    @BindView(R.id.image_list)
    LinearLayout mLLImageList;

    private Uri mImageUri;

    private boolean mShowLoader = false;

    private int[] filterString = {
            R.string.text_filter_normal,
            R.string.text_filter_in1977,
            R.string.text_filter_amaro,
            R.string.text_filter_brannan,
            R.string.text_filter_early_bird,
            R.string.text_filter_hefe,
            R.string.text_filter_hudson,
            R.string.text_filter_inkwell,
            R.string.text_filter_lomofi,
            R.string.text_filter_lord_kelvin,
            R.string.text_filter_nashville,
            R.string.text_filter_rise,
            R.string.text_filter_sierra,
            R.string.text_filter_sutro,
            R.string.text_filter_toaster,
            R.string.text_filter_valencia,
            R.string.text_filter_walden,
            R.string.text_filter_xproii
    };

    private int[] images = {
            R.drawable.filter_normal,
            R.drawable.filter_in1977,
            R.drawable.filter_amaro,
            R.drawable.filter_brannan,
            R.drawable.filter_early_bird,
            R.drawable.filter_hefe,
            R.drawable.filter_hudson,
            R.drawable.filter_inkwell,
            R.drawable.filter_lomofi,
            R.drawable.filter_lord_kelvin,
            R.drawable.filter_nashville,
            R.drawable.filter_rise,
            R.drawable.filter_sierra,
            R.drawable.filter_sutro,
            R.drawable.filter_toaster,
            R.drawable.filter_valencia,
            R.drawable.filter_walden,
            R.drawable.filter_xproii
    };

    private GPUImageFilterTools.FilterList filters = new GPUImageFilterTools.FilterList();
    private List<ImageView> selectList = new ArrayList<ImageView>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_filter);
        ButterKnife.bind(this);

        initView();
        initData();

        mImageUri = getIntent().getParcelableExtra(Constants.EXTRA_IMAGE_URI);
        handleImage(mImageUri);
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle(R.string.title_activity_image_filter);
    }

    @Override
    protected void initView() {
        // ((SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(this);

        // DisplayMetrics displayMetrics = new DisplayMetrics();
        // getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // int width = displayMetrics.widthPixels;

        mLLImageList.removeAllViews();

        // images list 개수만큼 적용한다.
        // item 자체에 OnClickListener를 추가한다.
        for (int i = 0; i < images.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_filter, null);
            CircleImageView imageView = (CircleImageView) view.findViewById(R.id.image_iv);
            TextView textView = (TextView) view.findViewById(R.id.image_tv);
            ImageView selectIV = (ImageView) view.findViewById(R.id.filter_select);
            selectList.add(selectIV);
            imageView.setImageDrawable(getResources().getDrawable(images[i]));
            textView.setText(getResources().getString(filterString[i]));
            view.setOnClickListener(new ImageItemClick(i));
            mLLImageList.addView(view);
        }

        selectList.get(0).setVisibility(View.VISIBLE);
    }

    private void initData() {
        filters.addFilter("default", null);
        filters.addFilter("1977", FilterType.I_1977);
        filters.addFilter("Amaro", FilterType.I_AMARO);
        filters.addFilter("Brannan", FilterType.I_BRANNAN);
        filters.addFilter("Earlybird", FilterType.I_EARLYBIRD);
        filters.addFilter("Hefe", FilterType.I_HEFE);
        filters.addFilter("Hudson", FilterType.I_HUDSON);
        filters.addFilter("Inkwell", FilterType.I_INKWELL);
        filters.addFilter("Lomo", FilterType.I_LOMO);
        filters.addFilter("LordKelvin", FilterType.I_LORDKELVIN);
        filters.addFilter("Nashville", FilterType.I_NASHVILLE);
        filters.addFilter("Rise", FilterType.I_RISE);
        filters.addFilter("Sierra", FilterType.I_SIERRA);
        filters.addFilter("sutro", FilterType.I_SUTRO);
        filters.addFilter("Toaster", FilterType.I_TOASTER);
        filters.addFilter("Valencia", FilterType.I_VALENCIA);
        filters.addFilter("Walden", FilterType.I_WALDEN);
        filters.addFilter("Xproll", FilterType.I_XPROII);

        // 여기서부터는 추가할 수도 있고 제외할 수도 있다.
        filters.addFilter("Contrast", FilterType.CONTRAST);
        filters.addFilter("Brightness", FilterType.BRIGHTNESS);
        filters.addFilter("Sepia", FilterType.SEPIA);
        filters.addFilter("Vignette", FilterType.VIGNETTE);
        filters.addFilter("ToneCurve", FilterType.TONE_CURVE);
        filters.addFilter("Lookup (Amatorka)", FilterType.LOOKUP_AMATORKA);
    }

    private class ImageItemClick implements View.OnClickListener {
        int clickPostion;

        ImageItemClick(int postion) {
            clickPostion = postion;
        }

        @Override
        public void onClick(View v) {
            if (clickPostion == 0) {
                switchFilterTo(new GPUImageFilter());
            } else {
                GPUImageFilter filter = GPUImageFilterTools.createFilterForType(
                        ImageFilterActivity.this,
                        filters.filters.get(clickPostion)
                );
                switchFilterTo(filter);
            }
            for (int i = 0; i < selectList.size(); i++) {
                if (i == clickPostion) {
                    selectList.get(i).setVisibility(View.VISIBLE);
                } else {
                    selectList.get(i).setVisibility(View.INVISIBLE);
                }
            }
            mGPUImageView.requestRender();
        }
    }

    // 필터를 변경하는 메서드이다.
    private void switchFilterTo(final GPUImageFilter filter) {
        if (mFilter == null || (filter != null && !mFilter.getClass().equals(filter.getClass()))) {
            mFilter = filter;
            mGPUImageView.setFilter(mFilter);
            mFilterAdjuster = new FilterAdjuster(mFilter);
        }
    }

    // 유저가 프로그레스바를 변경할 때 호출되는 메서드이다.
    @Override
    @Deprecated
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if (mFilterAdjuster != null) {
            mFilterAdjuster.adjust(progress);
        }
        mGPUImageView.requestRender();
    }

    // 이 메서드를 호출하면 이미지가 저장된다.
    private void saveImage() {
        mShowLoader = true;
        supportInvalidateOptionsMenu();
        // String fileName = System.currentTimeMillis() + ".jpg";
        // mGPUImageView.saveToPictures(IMAGE_OUTPUT_FOLDER_NAME, fileName, Constants.IMAGE_OPTION_QUALITY, this);
        try {
            File file = new File(new URI(mImageUri.toString()));
            mGPUImageView.saveToPictures(file, Constants.IMAGE_OPTION_QUALITY, this);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // 이미지 저장이 성공되면 호출되는 콜백 이벤트 메서드이다.
    // TODO: onPictureSaved 메서드가 호출되면 현재 액티비티를 finish 해야 한다.
    // 대신 종료 할 때 Intent를 통해 저장된 이미지의 Uri를 넘겨주는 로직이 구현되어야 한다.
    // 새로운 액티비티를 실행하고 현재 액티비티는 종료시킨다.
    // 여기서 WriteActivity를 실행시키는 것보다 현재 액티비티를 호출한 상위 액티비티에 맡기는 것이 더 낫지 않을까?
    @Override
    public void onPictureSaved(final Uri uri) {
        Log.i(TAG, "onPictureSaved:uri:" + uri.toString());

        // Toast.makeText(this, "Saved: " + uri.toString(), Toast.LENGTH_SHORT).show();

        ArrayList<Uri> imageUris = new ArrayList<>();
        imageUris.add(uri);
        Intent result = new Intent();
        result.putExtra(Constants.EXTRA_IMAGE_URIS, imageUris);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
    }

    private void handleImage(final Uri selectedImage) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
            float width = bitmap.getWidth();
            float height = bitmap.getHeight();
            float ratio = width / height;
            mGPUImageView.setRatio(ratio);
            mGPUImageView.setImage(selectedImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: 자르기 액티비티로 돌아갈 수 있도록 구현한다.
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_image_filter, menu);

        // Change crop & loader menu icons color to match the rest of the UI colors

        MenuItem menuItemLoader = menu.findItem(R.id.menu_loader);
        Drawable menuItemLoaderIcon = menuItemLoader.getIcon();
        if (menuItemLoaderIcon != null) {
            try {
                Log.d(TAG, "onCreateOptionsMenu:menuItemLoaderIcon_is_not_null");
                menuItemLoaderIcon.mutate();
                menuItemLoaderIcon.setColorFilter(CommonUtil.getColor(this, android.R.color.black), PorterDuff.Mode.SRC_ATOP);
                menuItemLoader.setIcon(menuItemLoaderIcon);
            } catch (IllegalStateException e) {
                Log.i(TAG, String.format("%s - %s", e.getMessage(), getString(R.string.ucrop_mutate_exception_hint)));
            }
            ((Animatable) menuItemLoader.getIcon()).start();
        }

        menu.findItem(R.id.action_confirm).setIcon(null);

        getToolbar().setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.i(TAG, "onPrepareOptionsMenu");
        menu.findItem(R.id.action_confirm).setVisible(!mShowLoader);
        menu.findItem(R.id.menu_loader).setVisible(mShowLoader);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_confirm:
                saveImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}