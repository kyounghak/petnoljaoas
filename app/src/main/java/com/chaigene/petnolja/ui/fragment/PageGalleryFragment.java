package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.event.WriteArticleEvent;
import com.chaigene.petnolja.ui.activity.ImageFilterActivity;
import com.chaigene.petnolja.ui.activity.OldMainActivity;
import com.chaigene.petnolja.ui.activity.WriteActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.sangcomz.fishbun.FishBun;
import com.sangcomz.fishbun.adapter.image.impl.GlideAdapter;
import com.sangcomz.fishbun.define.Define;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.yalantis.ucrop.model.AspectRatio;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.IMAGE_FILTER_ACTIVITY;
import static com.chaigene.petnolja.Constants.IMAGE_OPTION_MAX_COUNT;
import static com.chaigene.petnolja.Constants.IMAGE_OPTION_MAX_HEIGHT;
import static com.chaigene.petnolja.Constants.IMAGE_OPTION_MAX_WIDTH;
import static com.chaigene.petnolja.Constants.IMAGE_OPTION_QUALITY;
import static com.chaigene.petnolja.Constants.PAGE_HOME;
import static com.chaigene.petnolja.Constants.WRITE_ACTIVITY;

public class PageGalleryFragment extends ChildFragment {
    public static final String TAG = "PageGalleryFragment";

    private ArrayList<Uri> mSelectedImageUris;
    private ArrayList<Uri> mCroppedImageUris;

    private boolean mIsMultipleSelection;
    private int mCurrentModifyingIndex;

    /*public interface OnWriteArticleListener {
        void onWriteArticle();
    }*/

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.page_fragment_gallery, container, false);
        ButterKnife.bind(this, mView);
        return mView;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWriteArticleEvent(WriteArticleEvent event) {
        pickFromGallery();
    }

    @OnClick(R.id.photo_picker)
    protected void pickFromGallery() {
        Log.d(TAG, "pickFromGallery");

        resetFields();

        FishBun.with(this)
                .setImageAdapter(new GlideAdapter())
                .setIsUseDetailView(true)
                .setPickerSpanCount(4)
                .setMaxCount(IMAGE_OPTION_MAX_COUNT)
                .setActionBarColor(
                        ContextCompat.getColor(getContext(), android.R.color.white),
                        CommonUtil.getColor(getContext(), R.color.colorPrimaryDark)
                )
                .setActionBarTitleColor(
                        ContextCompat.getColor(getContext(), android.R.color.black)
                )
                .setSelectedImages(mSelectedImageUris)
                .setAlbumSpanCount(1, 2)
                .setButtonInAlbumActivity(false)
                .setCamera(true)
                // .setReachLimitAutomaticClose(true)
                .setHomeAsUpIndicatorDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_arrow_back_black_24dp))
                // .setOkButtonDrawable(ContextCompat.getDrawable(this, R.drawable.ic_custom_ok))
                .setAllViewTitle(getString(R.string.fishbun_all_view_title))
                .setActionBarTitle(getString(R.string.fishbun_action_bar_title))
                .exceptGif(true)
                .setMenuText(getString(R.string.next))
                .setMenuTextColor(CommonUtil.getColor(getContext(), android.R.color.black))
                .textOnImagesSelectionLimitReached(getString(R.string.fishbun_limit_reached))
                .textOnNothingSelected(getString(R.string.fishbun_nothing_selected))
                .startAlbum();
    }

    private void resetFields() {
        mSelectedImageUris = new ArrayList<>();
        mCroppedImageUris = new ArrayList<>();
        mIsMultipleSelection = false;
        mCurrentModifyingIndex = 0;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));

        switch (requestCode) {
            // 이미지 선택
            case Define.ALBUM_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mSelectedImageUris = data.getParcelableArrayListExtra(Define.INTENT_PATH);

                    int uriCount = mSelectedImageUris.size();
                    if (uriCount <= 0) {
                        CommonUtil.showSnackbar(getView(), R.string.toast_cannot_retrieve_selected_image);
                        return;
                    }

                    // TODO: 선택된 이미지 갯수에 따라서 다른 behavior를 작동시킨다.
                    // 1개일 땐 필터를 사용할 수 있다.
                    // 2개 이상일 땐 자르기만 사용할 수 있다.
                    if (uriCount == 1) {
                        mIsMultipleSelection = false;
                    } else if (uriCount > 1) {
                        mIsMultipleSelection = true;
                    }

                    startCropActivity(mSelectedImageUris.iterator().next(), mIsMultipleSelection, false);
                }

                if (resultCode == RESULT_CANCELED) {
                    resetFields();
                }
                break;
            // 자르기
            case UCrop.REQUEST_CROP:
                if (resultCode == RESULT_OK) {
                    mCroppedImageUris.add(UCrop.getOutput(data));

                    if (mIsMultipleSelection) {
                        int maxIndex = mSelectedImageUris.size() - 1;
                        if (mCurrentModifyingIndex < maxIndex) {
                            int nextIndex = mCurrentModifyingIndex + 1;
                            Uri nextUri = mSelectedImageUris.get(nextIndex);
                            mCurrentModifyingIndex++;
                            startCropActivity(nextUri, mIsMultipleSelection, false);
                        } else {
                            Log.d(TAG, "onActivityResult:REQUEST_CROP:OK:LAST");
                            startWriteActivity(mCroppedImageUris);
                        }
                    } else {
                        startImageFilterActivity(mCroppedImageUris.iterator().next());
                    }
                }

                // 자르기가 취소되었을 경우 다시 이미지 픽커를 실행한다.
                if (resultCode == RESULT_CANCELED) {
                    pickFromGallery();
                }

                // 여러개의 이미지 편집 중에 뒤로가기를 눌렀을 때는 이전 이미지의 자르기 화면을 보여준다.
                if (resultCode == UCrop.RESULT_PREVIOUS) {
                    Log.d(TAG, "onActivityResult:RESULT_PREVIOUS");

                    // RESULT_PREVIOUS는 여러개 이미지에서만 호출될 수 밖에 없다.
                    if (mIsMultipleSelection) {
                        int croppedCount = mCroppedImageUris.size();
                        if (croppedCount == 0) {
                            // 아무 것도 실행하지 않는다.
                        } else if (croppedCount >= 0) {
                            Log.d(TAG, "onActivityResult:REQUEST_CROP:CANCEL:mCroppedImageUris:" + mCroppedImageUris.toString());

                            if (mCurrentModifyingIndex > 0) {
                                int previousIndex = mCurrentModifyingIndex - 1;
                                Uri previousUri = mSelectedImageUris.get(previousIndex);
                                mCurrentModifyingIndex--;
                                startCropActivity(previousUri, mIsMultipleSelection, true);
                            } else {
                                Log.d(TAG, "onActivityResult:REQUEST_CROP:CANCEL:FIRST");
                            }
                        }
                    }
                }

                if (resultCode == UCrop.RESULT_ERROR) {
                    Throwable error = UCrop.getError(data);
                    handleCropError(error);
                }
                break;
            case IMAGE_FILTER_ACTIVITY:
                // ImageFilterActivity로부터 Intent로 편집된 single 이미지 uri를 넘겨받아야 한다.
                // 그리고 mCroppedImageUris의 0번 인덱스에 값을 넣어준다.
                if (resultCode == RESULT_OK) {
                    ArrayList<Uri> uris = data.getParcelableArrayListExtra(Constants.EXTRA_IMAGE_URIS);
                    startWriteActivity(uris);
                }

                if (resultCode == RESULT_CANCELED) {
                    startCropActivity(mSelectedImageUris.iterator().next(), mIsMultipleSelection, false);
                }
            case WRITE_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    ((OldMainActivity) getActivity()).getViewPager().setCurrentItem(PAGE_HOME, true);
                    PageHomeFragment homeFragment = (PageHomeFragment) ((OldMainActivity) getActivity()).getPageFragment(PAGE_HOME);
                    if (homeFragment != null) homeFragment.refresh();
                }
        }
    }

    // 자르기 화면을 실행한다.
    // TODO: 단일 이미지 선택일 경우 일단 100% 화질로 저장한다.
    private void startCropActivity(@NonNull Uri uri, boolean isMultipleSelection, boolean isPrevious) {
        String destinationFileName = CommonUtil.format(Constants.IMAGE_OPTION_CACHE_FILENAME, mCurrentModifyingIndex);
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getContext().getCacheDir(), destinationFileName)));

        // 최대 해상도 설정
        // uCrop = uCrop.withMaxResultSize(IMAGE_OPTION_MAX_WIDTH, IMAGE_OPTION_MAX_HEIGHT);

        UCrop.Options options = new UCrop.Options();

        options.withMaxResultSize(IMAGE_OPTION_MAX_WIDTH, IMAGE_OPTION_MAX_HEIGHT);
        options.setAspectRatioOptions(0, new AspectRatio("1:1", 1, 1), new AspectRatio("3:4", 3, 4));

        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        int quality = isMultipleSelection ? IMAGE_OPTION_QUALITY : 100;
        options.setCompressionQuality(quality);

        options.setHideBottomControls(false);

        // 프리스타일 크롭은 그리드를 자유자재로 크기 조절 할 수 있다
        // options.setFreeStyleCropEnabled(false);

        // 원형의 딤레이어를 추가한다.
        // options.setCircleDimmedLayer(true);

        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.SCALE);

        // This sets max size for bitmap that will be decoded from source Uri.
        // More size - more memory allocation, default implementation uses screen diagonal.
        // options.setMaxBitmapSize(640);

        // options.setMaxScaleMultiplier(5);
        // options.setImageToCropBoundsAnimDuration(666);
        // options.setDimmedLayerColor(Color.CYAN);
        // options.setShowCropFrame(false);

        // 그리드 줄 숫자를 지정한다.
        options.setCropGridRowCount(2);
        options.setCropGridColumnCount(2);
        // options.setCropGridStrokeWidth(20);
        // options.setCropGridColor(Color.GREEN);

        // options.setToolbarCropDrawable(R.drawable.your_crop_icon);
        options.setToolbarCancelDrawable(R.drawable.ic_arrow_back_black_24dp);

        if (!mIsMultipleSelection) {
            options.setToolbarTitle(getString(R.string.ucrop_toolbar_title));
        } else {
            options.setToolbarTitle(CommonUtil.format("(%d/%d)", mCurrentModifyingIndex + 1, mSelectedImageUris.size()));
        }

        // Color palette
        options.setToolbarColor(CommonUtil.getColor(getContext(), android.R.color.white));
        options.setStatusBarColor(CommonUtil.getColor(getContext(), R.color.colorPrimaryDark));
        options.setActiveWidgetColor(CommonUtil.getColor(getContext(), R.color.colorPrimary));
        options.setToolbarWidgetColor(CommonUtil.getColor(getContext(), android.R.color.black));
        // options.setRootViewBackgroundColor(CommonUtil.getColor(getContext(), R.color.your_color_res));

        options.setFromUCrop(isPrevious);

        uCrop.withOptions(options);
        uCrop.start(getContext(), this);
    }

    /**
     * 이미지 필터 액티비티를 시작하는 메서드이다.
     *
     * @param imageUri 이미지 필터 처리를 위해 전달되어야 할 이미지의 Uri.
     */
    private void startImageFilterActivity(Uri imageUri) {
        if (imageUri != null) {
            Intent intent = createIntent(ImageFilterActivity.class);
            intent.putExtra(Constants.EXTRA_IMAGE_URI, imageUri);
            startActivityForResult(intent, IMAGE_FILTER_ACTIVITY);
        } else {
            CommonUtil.showSnackbar(getView(), R.string.toast_cannot_retrieve_cropped_image);
        }
    }

    /**
     * UCrop에서 오류가 반환될 때 해당 오류를 처리하기 위한 메서드이다.
     *
     * @param cropError UCrop으로 반환받은 Throwable.
     */
    private void handleCropError(Throwable cropError) {
        Log.e(TAG, "handleCropError", cropError);
        // if (cropError != null) {
        // Toast.makeText(this, cropError.getMessage(), Toast.LENGTH_LONG).show();
        // } else {
        // Toast.makeText(this, R.string.toast_unexpected_error, Toast.LENGTH_SHORT).show();
        // }
    }

    /**
     * 모든 이미지 편집과정을 마치고 피드, 혹은 재능 다이얼로그를 보여준다.
     */
    private void startWriteActivity(final ArrayList<Uri> uris) {
        Log.i(TAG, "startWriteActivity:uris:" + uris.toString());
        startActivityForResult(WriteActivity.createWriteIntent(getContext(), uris), WRITE_ACTIVITY);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Log.i(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);
    }
}