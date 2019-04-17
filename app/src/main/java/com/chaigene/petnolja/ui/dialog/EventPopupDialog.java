package com.chaigene.petnolja.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.manager.TasksManager;
import com.chaigene.petnolja.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EventPopupDialog extends AppCompatDialogFragment {
    public static final String TAG = "EventPopupDialog";

    private static String EXTRA_EVENT = "extra_event";
    private static String EXTRA_LISTENER = "extra_listener";

    @BindView(R.id.event_image)
    ImageView mIvEventImage;

    private View mView;

    private Event mEvent;
    private OnButtonSelectListener mOnButtonSelectListener;

    public static DialogOrderDetailFragment newInstance(@NonNull Event event, @NonNull OnSelectListener l) {
        Log.i(TAG, "newInstance");
        DialogOrderDetailFragment dialogConfirmFragment = new DialogOrderDetailFragment();

        Bundle args = new Bundle();
        args.putSerializable(EXTRA_EVENT, event);
        args.putParcelable(EXTRA_LISTENER, l);
        dialogConfirmFragment.setArguments(args);

        // 백그라운드 커스터마이징
        // confirmDialogFragment.setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Panel);

        return dialogConfirmFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readBundle(getArguments());
    }

    protected void readBundle(@Nullable Bundle bundle) {
        Log.i(TAG, "readBundle");
        mEvent = (Event) bundle.getSerializable(EXTRA_EVENT);
        mOnButtonSelectListener = bundle.getParcelable(EXTRA_LISTENER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.dialog_event_popup, container, false);
        ButterKnife.bind(this, mView);

        /*if (mHighlightContent != null) {
            mTvHighlightText.setText(mHighlightContent);
        } else {
            mTvHighlightText.setVisibility(View.GONE);
        }*/

        // mTvContentText.setText(mContent);
        return mView;
    }

    private Task<Void> asyncTask() {
        // TODO: UserUtil.getPrivateInfo()를 통해 seller의 정보를 가져온다.
        // mBuyerPrivateInfo = null;
        // mSellerPrivateInfo = null;
        return TasksManager.call(() -> {
            // Task<PrivateInfo> getBuyerPrivateInfoTask = UserUtil.getPrivateInfo(mTargetOrder.getBuyerId());
            // mBuyerPrivateInfo = Tasks.await(getBuyerPrivateInfoTask);

            // Task<PrivateInfo> getSellerPrivateInfoTask = UserUtil.getPrivateInfo(mTargetOrder.getSellerId());
            // mSellerPrivateInfo = Tasks.await(getSellerPrivateInfoTask);

            /*if (!getSellerPrivateInfoTask.isSuccessful()) {
                Exception getSellerPrivateInfoError = getSellerPrivateInfoTask.getException();
                throw getSellerPrivateInfoError;
            }*/
            return null;
        });
    }

    private void initView() {
        Log.i(TAG, "initView");

        downloadPhoto(mIvEventImage, mEvent.getId(), "main.png");

        //String orderStatus = null;

        /*if (BuildConfig.DEBUG) {
            if (mShopType == SHOP_TYPE_SELL) {
                switch (mTargetOrder.getStatus()) {
                    case STATUS_SHIPPING_IN_PROGRESS:
                        // CommonUtil.showViews(mBtnShippingComplete);
                        break;
                    case STATUS_SHIPPING_COMPLETE:
                        // CommonUtil.hideViews(mBtnShippingComplete);
                        break;
                }
            }
        }*/

        // 배송정보 버튼
        /*String shippingEditBtnText;
        if (TextUtils.isEmpty(mTvShippingCarrier.getText()) && TextUtils.isEmpty(mTvShippingTrackingNo.getText())) {
            shippingEditBtnText = "배송정보입력";
        } else {
            shippingEditBtnText = "배송정보수정";
        }
        mBtnShippingEdit.setText(shippingEditBtnText);*/
    }

    // TIP: LauncherActivity에서 이미 다운로드를 받아두었다면 캐쉬에 저장되어 있기 때문에 이 메서드에서는 사실상
    // 네트워크 통신이 발생하지 않는다.
    private void downloadPhoto(@NonNull ImageView view, @NonNull String eventId, @NonNull String filename) {
        StorageReference postsRef = StorageManager.getEventsRef().child(eventId).child(filename);
        GlideManager.loadImage(postsRef, view);
    }

    @OnClick(R.id.do_not_show_again_button)
    void doNotShowAgain() {
        Log.i(TAG, "confirm:listener:" + mOnButtonSelectListener);

        // TODO: EventUtil의 setDoNotShowAgainPopup을 호출해줘야 하지만
        // 자동으로 호출할 것인가 직접 구현할 것인가는 고민해보아야 한다.
        // 일단은 직접 구현하는 것으로 하자.

        if (mOnButtonSelectListener == null) return;
        mOnButtonSelectListener.onDoNotShowAgain();
        dismiss();
    }

    @OnClick(R.id.close_button)
    void close() {
        Log.i(TAG, "deny:listener:" + mOnButtonSelectListener);
        if (mOnButtonSelectListener == null) return;
        mOnButtonSelectListener.onClose();
        dismiss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        return dialog;
    }

    private interface OnButtonSelectListener extends Parcelable {
        void onDoNotShowAgain();

        void onClose();
    }

    public static class OnSelectListener implements OnButtonSelectListener {
        @Override
        public void onDoNotShowAgain() {
        }

        @Override
        public void onClose() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
        }
    }
}