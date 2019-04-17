package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.chaigene.petnolja.manager.TasksManager;
import com.chaigene.petnolja.model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.hashtag.HashTagHelper;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.ConfigManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.FIRUser;
import com.chaigene.petnolja.ui.activity.BaseActivity;
import com.chaigene.petnolja.ui.activity.ImageFilterActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;
import com.sangcomz.fishbun.FishBun;
import com.sangcomz.fishbun.adapter.image.impl.GlideAdapter;
import com.sangcomz.fishbun.define.Define;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.mrapp.android.bottomsheet.BottomSheet;
import de.mrapp.android.util.ThreadUtil;

import static com.facebook.FacebookSdk.getCacheDir;
import static com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND;
import static com.chaigene.petnolja.Constants.IMAGE_OPTION_PROFILE_MAX_HEIGHT;
import static com.chaigene.petnolja.Constants.IMAGE_OPTION_PROFILE_MAX_WIDTH;
import static com.chaigene.petnolja.Constants.IMAGE_OPTION_QUALITY;
import static com.chaigene.petnolja.Constants.PROFILE_IMAGE_FILENAME;
import static com.chaigene.petnolja.Constants.SETTING_PROFILE_FRAGMENT;

public class SettingProfileFragment extends ChildFragment {
    public static final String TAG = "SettingProfileFragment";

    private static final long BOTTOM_SHEET_ITEM_CHANGE_PROFILE_IMAGE = 0;
    private static final long BOTTOM_SHEET_ITEM_DELETE_PROFILE_IMAGE = 1;

    private static final String ACTION_STATUS = "action_status";
    public static final int ACTION_STATUS_INSERT = 0;
    public static final int ACTION_STATUS_UPDATE = 1;

    @BindView(R.id.profile_image)
    ImageView mCivProfileImage;

    @BindView(R.id.nickname_input)
    EditText mEtNickname;

    @BindView(R.id.description_input)
    EditText mEtDescription;

    @BindView(R.id.next_button)
    Button mBtnNext;

    @BindView(R.id.done_button)
    Button mBtnDone;

    private int mActionStatus;

    @Nullable
    private User mMyUser;

    private Uri mSelectedImageUri;
    private Uri mCroppedImageUri;

    private boolean mInitializedAsyncTask;

    public synchronized boolean isInitializedAsyncTask() {
        return mInitializedAsyncTask;
    }

    public synchronized void setInitializedAsyncTask(boolean initializedAsyncTask) {
        Log.i(TAG, "setInitializedAsyncTask");
        this.mInitializedAsyncTask = initializedAsyncTask;
    }

    public static SettingProfileFragment newInstance(int status) {
        SettingProfileFragment fragment = new SettingProfileFragment();

        Bundle args = new Bundle();
        args.putInt(ACTION_STATUS, status);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        mActionStatus = bundle.getInt(ACTION_STATUS);
        Log.d(TAG, "readBundle:actionStatus:" + mActionStatus);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_setting_profile, container, false);
        ButterKnife.bind(this, mView);

        initView();

        if (mActionStatus == ACTION_STATUS_UPDATE) {
            asyncTask();
        }

        return mView;
    }

    private void asyncTask() {
        showLoadingDialog();
        final Task<Void> downloadProfileImageTask = UserUtil.downloadProfileImage(AuthManager.getUserId(), mCivProfileImage);
        final Task<Void> loadUserInformationTask = loadUserInformation();
        Tasks.whenAll(downloadProfileImageTask, loadUserInformationTask).continueWith((Continuation<Void, Void>) task -> {
            if (!downloadProfileImageTask.isSuccessful()) {
                Log.w(TAG, "onCreate:downloadProfileImageTask:ERROR:", task.getException());
            }

            if (!loadUserInformationTask.isSuccessful()) {
                Log.w(TAG, "onCreate:loadUserInformationTask:ERROR:", task.getException());
            }

            Log.d(TAG, "onCreate:loadUserInformation+downloadProfileImage:SUCCESS");
            setInitializedAsyncTask(true);
            dismissDialog();
            return null;
        });
    }

    protected void initView() {
        setupNicknameInput();
        if (mActionStatus == ACTION_STATUS_INSERT) {
            mBtnNext.setVisibility(View.VISIBLE);
            mBtnDone.setVisibility(View.GONE);
        }
        if (mActionStatus == ACTION_STATUS_UPDATE) {
            mBtnNext.setVisibility(View.GONE);
            mBtnDone.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (mActionStatus == ACTION_STATUS_UPDATE) {
            setToolbarTitle("프로필 수정");
            setToolbarTitleAlign(Gravity.CENTER_HORIZONTAL);
        }
    }

    private void setupNicknameInput() {
        Log.i(TAG, "setupNicknameInput");

        if (mActionStatus == ACTION_STATUS_UPDATE) {
            mEtNickname.setEnabled(false);
            return;
        }

        /*InputFilter filter = new InputFilter() {
            final String TAG = "InputFilter";

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Log.i(TAG, "filter:source:" + source + "|start:" + start + "|end:" + end + "|dest:" + dest + "|dstart:" + dstart + "|dend:" + dend);
                boolean keepOriginal = true;
                StringBuilder sb = new StringBuilder(end - start);
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (isCharAllowed(c)) {
                        sb.append(c);
                    } else {
                        // CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_nickname);
                        keepOriginal = false;
                    }
                }

                Log.d(TAG, "filter:sb:" + sb);

                if (keepOriginal)
                    return null;
                else {
                    if (source instanceof Spanned) {
                        SpannableString sp = new SpannableString(sb);
                        TextUtils.copySpansFrom((Spanned) source, start, sb.length(), null, sp, 0);
                        return sp;
                    } else {
                        return sb;
                    }
                }
            }

            private boolean isCharAllowed(char c) {
                // boolean result = Character.isCharacter.isLetterOrDigit(c) || Character.isLowerCase(c) || Character.toString(c).equals("_");
                boolean result = isNicknameValidChars(String.valueOf(c));
                Log.i(TAG, "isCharAllowed:char:" + c + "|result:" + result);
                return result;
            }
        };
        ArrayList<InputFilter> curInputFilters = new ArrayList<InputFilter>(Arrays.asList(mEtNickname.getFilters()));
        curInputFilters.add(filter);
        InputFilter[] newInputFilters = curInputFilters.toArray(new InputFilter[curInputFilters.size()]);
        mEtNickname.setFilters(newInputFilters);*/

        mEtNickname.addTextChangedListener(new TextWatcher() {
            final String TAG = "TextWatcher";

            final Pattern UPPER_CASE_REGEX = Pattern.compile("[A-Z]");

            String oldText;
            String newText;
            int oldCursor;
            int newCursor;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.i(TAG, "beforeTextChanged:s:" + s + "|start:" + start + "|count:" + count + "|after:" + after);

                oldText = s.toString();
                oldCursor = mEtNickname.getSelectionStart();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.i(TAG, "onTextChanged:s:" + s + "|start:" + start + "|before:" + before + "|count:" + count);

                newText = s.toString();
                newCursor = mEtNickname.getSelectionStart();
                Log.d(TAG, "onTextChanged:newText:" + newText);
            }

            @Override
            public void afterTextChanged(final Editable s) {
                Log.i(TAG, "afterTextChanged:s:" + s);

                if (TextUtils.isEmpty(s.toString())) return;

                if (!isNicknameValidChars(s.toString())) {
                    Log.d(TAG, "onTextChanged:isNicknameValidChars:false");
                    Log.d(TAG, "onTextChanged:oldText:" + oldText);

                    mEtNickname.removeTextChangedListener(this);
                    s.clear();
                    s.insert(0, oldText);
                    mEtNickname.setSelection(oldCursor);
                    mEtNickname.addTextChangedListener(this);

                    CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_nickname);
                } else {
                    Log.d(TAG, "onTextChanged:isNicknameValidChars:true");

                    // Ref: https://gist.github.com/karolw/549cb5c1ef46c008d4b1
                    Matcher matcher = UPPER_CASE_REGEX.matcher(s);
                    while (matcher.find()) {
                        CharSequence upperCaseRegion = s.subSequence(matcher.start(), matcher.end());
                        s.replace(matcher.start(), matcher.end(), upperCaseRegion.toString().toLowerCase());
                    }
                }
            }
        });
    }

    private void resetFields() {
        mSelectedImageUri = null;
        mCroppedImageUri = null;
    }

    @OnClick(R.id.profile_image)
    void changeProfileImage() {
        if (mActionStatus == ACTION_STATUS_INSERT) {
            pickFromGallery();
        }

        if (mActionStatus == ACTION_STATUS_UPDATE) {
            showBottomSheet();
        }
    }

    private void showBottomSheet() {
        BottomSheet.Builder builder = new BottomSheet.Builder(getContext());
        builder.addItem(
                (int) BOTTOM_SHEET_ITEM_CHANGE_PROFILE_IMAGE,
                R.string.bottom_sheet_item_change_profile_image,
                R.drawable.ic_action_picture
        );
        builder.addItem(
                (int) BOTTOM_SHEET_ITEM_DELETE_PROFILE_IMAGE,
                R.string.bottom_sheet_item_delete_profile_image,
                R.drawable.ic_action_discard
        );
        builder.setOnItemClickListener((parent, view, position, id) -> {
            if (id == BOTTOM_SHEET_ITEM_CHANGE_PROFILE_IMAGE) {
                pickFromGallery();
            }
            if (id == BOTTOM_SHEET_ITEM_DELETE_PROFILE_IMAGE) {
                deleteProfileImage();
            }
        });
        BottomSheet bottomSheet = builder.create();
        bottomSheet.show();
    }

    private void pickFromGallery() {
        Log.d(TAG, "pickFromGallery");
        resetFields();
        FishBun.with(this)
                .setImageAdapter(new GlideAdapter())
                .setIsUseDetailView(true)
                .setMaxCount(1)
                .setPickerSpanCount(4)
                .setActionBarColor(
                        CommonUtil.getColor(getContext(), android.R.color.white),
                        CommonUtil.getColor(getContext(), R.color.colorPrimaryDark)
                )
                .setActionBarTitleColor(
                        ContextCompat.getColor(getContext(), android.R.color.black)
                )
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
                .textOnNothingSelected("이미지를 선택해주세요.")
                .startAlbum();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));

        switch (requestCode) {
            // 이미지 선택
            case Define.ALBUM_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    ArrayList<Uri> uri = data.getParcelableArrayListExtra(Define.INTENT_PATH);
                    mSelectedImageUri = uri.iterator().next();
                    startCropActivity(mSelectedImageUri);
                }

                if (resultCode == RESULT_CANCELED) {
                    resetFields();
                }
                break;
            // 자르기
            case UCrop.REQUEST_CROP:
                if (resultCode == RESULT_OK) {
                    mCroppedImageUri = UCrop.getOutput(data);
                    startImageFilterActivity(mCroppedImageUri);
                }

                // 자르기가 취소되었을 경우 다시 이미지 픽커를 실행한다.
                if (resultCode == RESULT_CANCELED) {
                    pickFromGallery();
                }

                if (resultCode == UCrop.RESULT_ERROR) {
                    Throwable error = UCrop.getError(data);
                    handleCropError(error);
                }
                break;
            // 필터
            case Constants.IMAGE_FILTER_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    // TODO: 이미지를 업로드 하고 다시 다운로드 받는다.
                    ArrayList<Uri> uris = data.getParcelableArrayListExtra(Constants.EXTRA_IMAGE_URIS);
                    uploadProfileImage(uris.iterator().next());
                }

                if (resultCode == RESULT_CANCELED) {
                    startCropActivity(mSelectedImageUri);
                }
        }
    }

    private void startCropActivity(@NonNull Uri uri) {
        Log.i(TAG, "startCropActivity:uri" + uri);
        String destinationFileName = CommonUtil.format(Constants.IMAGE_OPTION_CACHE_FILENAME, 0);
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        uCrop = uCrop.withMaxResultSize(IMAGE_OPTION_PROFILE_MAX_WIDTH, IMAGE_OPTION_PROFILE_MAX_HEIGHT);
        UCrop.Options options = new UCrop.Options();
        options.withAspectRatio(1, 1);
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(IMAGE_OPTION_QUALITY);
        options.setHideBottomControls(false);
        // options.setCircleDimmedLayer(true);
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.SCALE);
        options.setCropGridRowCount(2);
        options.setCropGridColumnCount(2);
        options.setToolbarCancelDrawable(R.drawable.ic_arrow_back_white_24dp);
        options.setToolbarTitle(getString(R.string.ucrop_toolbar_title));
        options.setToolbarColor(CommonUtil.getColor(getContext(), android.R.color.white));
        options.setStatusBarColor(CommonUtil.getColor(getContext(), R.color.colorPrimaryDark));
        options.setActiveWidgetColor(CommonUtil.getColor(getContext(), R.color.colorPrimary));
        options.setToolbarWidgetColor(CommonUtil.getColor(getContext(), android.R.color.black));
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
            startActivityForResult(intent, Constants.IMAGE_FILTER_ACTIVITY);
        } else {
            CommonUtil.showSnackbar(getActivity(), R.string.toast_cannot_retrieve_cropped_image);
        }
    }

    /**
     * UCrop에서 오류가 반환될 때 해당 오류를 처리하기 위한 메서드이다.
     *
     * @param cropError UCrop으로 반환받은 Throwable.
     */
    private void handleCropError(Throwable cropError) {
        Log.e(TAG, "handleCropError", cropError);
    }

    protected void uploadProfileImage(final Uri uri) {
        Log.d(TAG, "uploadProfileImage:uri: " + uri.toString());
        showLoadingDialog();

        final String myUid = AuthManager.getUserId();
        StorageReference fileRef = StorageManager.getUsersRef().child(myUid).child(PROFILE_IMAGE_FILENAME);
        DatabaseReference signatureRef = DatabaseManager.getUserUsersRef().child(myUid).child("signature");

        final AtomicBoolean isTaskFinished = new AtomicBoolean(false);
        StorageManager.putFileWithSignature(fileRef, signatureRef, uri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "uploadProfileImage:ERROR", task.getException());
                CommonUtil.showSnackbar(getActivity(), "업로드가 실패하였습니다. 잠시후 다시 시도해주세요.");
                dismissDialog();
                isTaskFinished.set(true);
                return Tasks.forResult(null);
            }
            String signature = task.getResult();
            User cachedUser = UserUtil.getInstance().loadCachedUser(AuthManager.getUserId());
            if (cachedUser != null) {
                cachedUser.setSignature(signature);
                UserUtil.getInstance().saveCachedUser(cachedUser);
            }
            return UserUtil.downloadProfileImage(myUid, mCivProfileImage);
        }).continueWith((Continuation<Void, Void>) task -> {
            if (isTaskFinished.get()) Tasks.forResult(null);
            dismissDialog();
            if (!task.isSuccessful()) {
                Log.w(TAG, "uploadProfileImage:downloadProfileImage:ERROR", task.getException());
                return null;
            }
            Log.d(TAG, "uploadProfileImage:downloadProfileImage:SUCCESS");
            return null;
        });
    }

    private void deleteProfileImage() {
        Log.d(TAG, "deleteProfileImage");
        showLoadingDialog();
        final String myUid = AuthManager.getUserId();
        final StorageReference fileRef = StorageManager.getUsersRef().child(myUid).child(PROFILE_IMAGE_FILENAME);
        final AtomicBoolean isTaskFinished = new AtomicBoolean(false);
        StorageManager.delete(fileRef).continueWithTask(task -> {
            Task<Void> nextTask = UserUtil.setSignature(myUid, "-1");
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "deleteProfileImage:ERROR:removeValue:ERROR", e);
                if (e instanceof StorageException) {
                    StorageException storageEx = (StorageException) e;
                    if (storageEx.getErrorCode() == ERROR_OBJECT_NOT_FOUND) {
                        return nextTask;
                    }
                } else {
                    isTaskFinished.set(true);
                    return Tasks.forResult(null);
                }
            }
            Log.d(TAG, "deleteProfileImage:SUCCESS:removeValue:SUCCESS");
            return nextTask;
        }).continueWith((Continuation<Void, Void>) task -> {
            if (isTaskFinished.get()) Tasks.forResult(null);
            if (!task.isSuccessful()) {
                Log.w(TAG, "deleteProfileImage:setSignature:ERROR", task.getException());
            } else {
                Log.d(TAG, "deleteProfileImage:setSignature:SUCCESS");
            }
            CommonUtil.showSnackbar(getActivity(), "프로필 이미지가 삭제되었습니다.");
            mCivProfileImage.setImageDrawable(CommonUtil.getDrawable(getContext(), R.drawable.bg_button_profile_image));
            dismissDialog();
            return null;
        });
    }

    private Task<Void> loadUserInformation() {
        return UserUtil.getUser(AuthManager.getUserId()).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "loadUserInformation:ERROR:", task.getException());
                return null;
            }

            User user = task.getResult();
            Log.d(TAG, "loadUserInformation:user:" + user);

            mEtNickname.setText(user.getNickname());
            mEtDescription.setText(user.getDescription());
            HashTagHelper helper = CommonUtil.createDefaultHashTagHelper(getActivity(), null);
            helper.handle(mEtDescription);

            mMyUser = user;
            return null;
        });
    }

    @OnClick(R.id.next_button)
    void insertUser() {
        Log.i(TAG, "insertUser");

        CommonUtil.hideKeyboard(getActivity());

        final String nickname = mEtNickname.getText().toString().trim();
        final String description = mEtDescription.getText().toString().trim();

        boolean isCancelled = false;
        View focusView = null;

        if (TextUtils.isEmpty(nickname)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_nickname_field_required);
            focusView = mEtNickname;
            isCancelled = true;
        }

        if (!isNicknameValid(nickname)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_nickname);
            focusView = mEtNickname;
            isCancelled = true;
        }

        if (!isDescriptionValid(description)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_description_exceed_length);
            focusView = mEtDescription;
            isCancelled = true;
        }

        if (isCancelled) {
            focusView.requestFocus();
        } else {
            showLoadingDialog();

            Tasks.call(Executors.newSingleThreadExecutor(), (Callable<Void>) () -> {
                Task<Boolean> isNicknameExistsTask = UserUtil.isNicknameExists(nickname);
                Boolean isNicknameExists = Tasks.await(isNicknameExistsTask);

                if (!isNicknameExistsTask.isSuccessful()) {
                    dismissDialog();
                    Log.w(TAG, "isNicknameExistsTask:ERROR:", isNicknameExistsTask.getException());
                    CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                    return null;
                }

                if (isNicknameExists) {
                    ThreadUtil.runOnUiThread(() -> {
                        dismissDialog();
                        CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_nickname_duplication);
                        mEtNickname.requestFocus();
                    });
                    return null;
                }

                FIRUser firUser = new FIRUser();
                firUser.setKey(AuthManager.getUserId());
                firUser.setNickname(nickname);
                firUser.setDescription(description);
                UserUtil.update(firUser).continueWith((Continuation<Void, Void>) task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "updateUserInformation:ERROR");
                        CommonUtil.showSnackbar(getActivity(), "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                        return null;
                    }
                    Log.d(TAG, "updateUserInformation:SUCCESS");

                    ConfigManager.getInstance(getContext()).setNickname(nickname);
                    ConfigManager.getInstance(getContext()).setEmail(AuthManager.getEmail());

                    ((BaseActivity) getActivity()).onFragmentResult(SETTING_PROFILE_FRAGMENT, RESULT_OK, null);
                    return null;
                });
                return null;
            }).continueWith((Continuation<Void, Void>) task -> {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "updateUserInformation:ERROR:", task.getException());
                }
                return null;
            });
        }
    }

    @OnClick(R.id.done_button)
    void updateUser() {
        Log.i(TAG, "updateUser");

        CommonUtil.hideKeyboard(getActivity());

        final String description = mEtDescription.getText().toString().trim();

        boolean isCancelled = false;
        View focusView = null;

        if (!isDescriptionValid(description)) {
            CommonUtil.showSnackbar(getActivity(), R.string.msg_invalid_description_exceed_length);
            focusView = mEtDescription;
            isCancelled = true;
        }

        if (isCancelled) {
            focusView.requestFocus();
        } else {
            showLoadingDialog();

            TasksManager.call((Callable<Void>) () -> {
                User user = new User();
                user.setId(AuthManager.getUserId());
                user.setDescription(description);

                Task<Void> updateTask = UserUtil.update(user);
                Tasks.await(updateTask);

                return null;
            }).continueWith((Continuation<Void, Void>) task -> {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "updateUser:ERROR:", task.getException());
                }
                finish();
                // ((BaseActivity) getActivity()).onFragmentResult(SETTING_PROFILE_FRAGMENT, RESULT_OK, null);
                if (getTargetFragment() != null)
                    getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, new Intent());
                return null;
            });
        }
    }

    private boolean isNicknameValid(String nickname) {
        return isNicknameValidChars(nickname);
    }

    private boolean isNicknameValidChars(String nickname) {
        return nickname.matches("([A-Za-z0-9\\_]+)");
    }

    private boolean isDescriptionValid(String description) {
        return description.length() <= 1000;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
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

    private void finish() {
        // 임시 조취.
        getActivity().onBackPressed();
    }
}