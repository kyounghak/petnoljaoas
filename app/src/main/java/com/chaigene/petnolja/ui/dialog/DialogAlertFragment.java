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
import android.widget.TextView;

import com.chaigene.petnolja.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DialogAlertFragment extends AppCompatDialogFragment {
    public static final String TAG = "DialogAlertFragment";

    private static String EXTRA_HIGHLIGHT_CONTENT = "extra_highlight_content";
    private static String EXTRA_CONTENT = "extra_content";
    private static String EXTRA_LISTENER = "extra_listener";
    private static String EXTRA_IS_CANCELLABLE = "extra_is_cancellable";

    @BindView(R.id.highlight_text)
    TextView mTvHighlightText;

    @BindView(R.id.content_text)
    TextView mTvContentText;

    private View mView;

    private String mHighlightContent;
    private String mContent;
    private OnButtonSelectListener mOnButtonSelectListener;
    private boolean mIsCancellable;

    public static DialogAlertFragment newInstance(@NonNull String content,
                                                  @NonNull OnDoneListener listener) {
        return newInstance(null, content, listener);
    }

    public static DialogAlertFragment newInstance(@NonNull String content,
                                                  @NonNull OnDoneListener listener,
                                                  boolean isCancellable) {
        return newInstance(null, content, listener, isCancellable);
    }

    public static DialogAlertFragment newInstance(@Nullable String highlightText,
                                                  @NonNull String content,
                                                  @NonNull OnDoneListener listener) {
        return newInstance(highlightText, content, listener, true);
    }

    public static DialogAlertFragment newInstance(@Nullable String highlightText,
                                                  @NonNull String content,
                                                  @NonNull OnDoneListener listener,
                                                  boolean isCancellable) {
        Log.i(TAG, "newInstance");
        DialogAlertFragment confirmDialogFragment = new DialogAlertFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_HIGHLIGHT_CONTENT, highlightText);
        args.putString(EXTRA_CONTENT, content);
        args.putParcelable(EXTRA_LISTENER, listener);
        args.putBoolean(EXTRA_IS_CANCELLABLE, isCancellable);
        confirmDialogFragment.setArguments(args);

        // 백그라운드 커스터마이징
        // confirmDialogFragment.setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Panel);

        return confirmDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readBundle(getArguments());
    }

    protected void readBundle(@Nullable Bundle bundle) {
        Log.i(TAG, "readBundle");
        mHighlightContent = bundle.getString(EXTRA_HIGHLIGHT_CONTENT);
        mContent = bundle.getString(EXTRA_CONTENT);
        mOnButtonSelectListener = bundle.getParcelable(EXTRA_LISTENER);
        mIsCancellable = bundle.getBoolean(EXTRA_IS_CANCELLABLE, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_dialog_alert, container, false);
        ButterKnife.bind(this, mView);

        if (mHighlightContent != null) {
            mTvHighlightText.setText(mHighlightContent);
        } else {
            mTvHighlightText.setVisibility(View.GONE);
        }

        mTvContentText.setText(mContent);
        return mView;
    }

    @OnClick(R.id.done_button)
    void confirm() {
        Log.i(TAG, "confirm:listener:" + mOnButtonSelectListener);
        if (mOnButtonSelectListener == null) return;
        mOnButtonSelectListener.onDone();
        dismiss();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        if (!mIsCancellable) dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private interface OnButtonSelectListener extends Parcelable {
        void onDone();
    }

    public static class OnDoneListener implements OnButtonSelectListener {
        @Override
        public void onDone() {
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