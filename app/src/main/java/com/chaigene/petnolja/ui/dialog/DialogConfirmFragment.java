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

public class DialogConfirmFragment extends AppCompatDialogFragment {
    public static final String TAG = "DialogConfirmFragment";

    private static String EXTRA_HIGHLIGHT_CONTENT = "extra_highlight_content";
    private static String EXTRA_CONTENT = "extra_content";
    private static String EXTRA_LISTENER = "extra_listener";

    @BindView(R.id.highlight_text)
    TextView mTvHighlightText;

    @BindView(R.id.content_text)
    TextView mTvContentText;

    private View mView;

    private String mHighlightContent;
    private String mContent;
    private OnButtonSelectListener mOnButtonSelectListener;

    public static DialogConfirmFragment newInstance(@NonNull String content, @NonNull OnSelectListener listener) {
        return newInstance(null, content, listener);
    }

    public static DialogConfirmFragment newInstance(@Nullable String highlightText, @NonNull String content, @NonNull OnSelectListener listener) {
        Log.i(TAG, "newInstance");
        DialogConfirmFragment dialogConfirmFragment = new DialogConfirmFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_HIGHLIGHT_CONTENT, highlightText);
        args.putString(EXTRA_CONTENT, content);
        args.putParcelable(EXTRA_LISTENER, listener);
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
        mHighlightContent = bundle.getString(EXTRA_HIGHLIGHT_CONTENT);
        mContent = bundle.getString(EXTRA_CONTENT);
        mOnButtonSelectListener = bundle.getParcelable(EXTRA_LISTENER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_dialog_confirm, container, false);
        ButterKnife.bind(this, mView);

        if (mHighlightContent != null) {
            mTvHighlightText.setText(mHighlightContent);
        } else {
            mTvHighlightText.setVisibility(View.GONE);
        }

        mTvContentText.setText(mContent);
        return mView;
    }

    @OnClick(R.id.confirm_button)
    void confirm() {
        Log.i(TAG, "confirm:listener:" + mOnButtonSelectListener);
        if (mOnButtonSelectListener == null) return;
        mOnButtonSelectListener.onConfirm();
        dismiss();
    }

    @OnClick(R.id.deny_button)
    void deny() {
        Log.i(TAG, "deny:listener:" + mOnButtonSelectListener);
        if (mOnButtonSelectListener == null) return;
        mOnButtonSelectListener.onDeny();
        dismiss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        /*Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }*/
    }

    private interface OnButtonSelectListener extends Parcelable {
        void onConfirm();

        void onDeny();
    }

    public static class OnSelectListener implements OnButtonSelectListener {
        @Override
        public void onConfirm() {
        }

        @Override
        public void onDeny() {
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