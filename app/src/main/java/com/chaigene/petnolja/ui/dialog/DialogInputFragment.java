package com.chaigene.petnolja.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.AppCompatSpinner;

import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.chaigene.petnolja.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DialogInputFragment extends AppCompatDialogFragment {
    public static final String TAG = "DialogConfirmFragment";

    private static String EXTRA_HIGHLIGHT_CONTENT = "extra_highlight_content";
    private static String EXTRA_CONTENT = "extra_content";
    private static String EXTRA_SPINNER_TEXT = "extra_spinner_text";
    private static String EXTRA_INPUT_TEXT = "extra_input_text";
    private static String EXTRA_INPUT_HINT = "extra_input_hint";
    private static String EXTRA_INPUT_TYPE = "extra_input_type";
    private static String EXTRA_INPUT_TYPE_CAPS = "extra_input_type_caps";
    private static String EXTRA_INPUT_TYPE_LINE = "extra_input_type_line";
    private static String EXTRA_SPINNER_ENTRIES = "extra_spinner_entries";
    private static String EXTRA_SPINNER_LISTENER = "extra_spinner_listener";
    private static String EXTRA_LISTENER = "extra_listener";

    private static int INPUT_TYPE_NULL = 0;
    private static int INPUT_TYPE_NUMBER = 1;
    private static int INPUT_TYPE_TEXT = 2;
    private static int INPUT_TYPE_ALPHANUMERIC = 3;
    private static int INPUT_TYPE_LOWERCASE = 10;
    private static int INPUT_TYPE_UPPERCASE = 11;
    private static int INPUT_TYPE_SINGLELINE = 100;
    private static int INPUT_TYPE_MULTILINE = 110;

    @BindView(R.id.highlight_text)
    TextView mTvHighlightText;

    @BindView(R.id.content_text)
    TextView mTvContentText;

    @BindView(R.id.content_spinner)
    AppCompatSpinner mSpContent;

    @BindView(R.id.content_input)
    EditText mEtContent;

    private View mView;

    private String mHighlightContent;
    private String mContent;
    private CharSequence[] mSpinnerEntries;
    private OnSpinnerSelectListener mOnSpinnerListener;
    private OnButtonSelectListener mOnButtonSelectListener;

    private int mSpinnerIndex;
    private String mSpinnerText;
    private String mInputHint;
    private String mInputText;
    private int mInputType;
    private int mInputTypeCaps;
    private int mInputTypeLine;

    public static DialogInputFragment newInstance(@NonNull String content,
                                                  @NonNull OnSelectListener listener) {
        return newInstance(null, content, null, null, listener);
    }

    public static DialogInputFragment newInstance(@NonNull String content,
                                                  @Nullable CharSequence[] spinnerEntries,
                                                  @Nullable OnSpinnerListener spinnerListener,
                                                  @NonNull OnSelectListener listener) {
        return newInstance(null, content, spinnerEntries, spinnerListener, listener);
    }

    public static DialogInputFragment newInstance(@Nullable String highlightText,
                                                  @NonNull String content,
                                                  @Nullable CharSequence[] spinnerEntries,
                                                  @Nullable OnSpinnerListener spinnerListener,
                                                  @NonNull OnSelectListener listener) {
        Log.i(TAG, "newInstance");
        DialogInputFragment dialogConfirmFragment = new DialogInputFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_HIGHLIGHT_CONTENT, highlightText);
        args.putString(EXTRA_CONTENT, content);
        args.putCharSequenceArray(EXTRA_SPINNER_ENTRIES, spinnerEntries);
        args.putParcelable(EXTRA_SPINNER_LISTENER, spinnerListener);
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
        mSpinnerText = bundle.getString(EXTRA_SPINNER_TEXT);
        mInputHint = bundle.getString(EXTRA_INPUT_HINT);
        mInputText = bundle.getString(EXTRA_INPUT_TEXT);
        mInputType = bundle.getInt(EXTRA_INPUT_TYPE);
        mInputTypeCaps = bundle.getInt(EXTRA_INPUT_TYPE_CAPS);
        mInputTypeLine = bundle.getInt(EXTRA_INPUT_TYPE_LINE);
        mSpinnerEntries = bundle.getCharSequenceArray(EXTRA_SPINNER_ENTRIES);
        mOnSpinnerListener = bundle.getParcelable(EXTRA_SPINNER_LISTENER);
        mOnButtonSelectListener = bundle.getParcelable(EXTRA_LISTENER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_dialog_input, container, false);
        ButterKnife.bind(this, mView);

        if (mHighlightContent != null) {
            mTvHighlightText.setText(mHighlightContent);
        } else {
            mTvHighlightText.setVisibility(View.GONE);
        }

        mTvContentText.setText(mContent);

        // CharSequence[] entries = getResources().getTextArray(R.array.shipping_tracking_carrier_names);
        if (mSpinnerEntries != null) {
            mSpContent.setVisibility(View.VISIBLE);
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(mSpContent.getContext(), android.R.layout.simple_spinner_item, mSpinnerEntries);
            adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
            mSpContent.setAdapter(adapter);
        }

        if (mOnSpinnerListener != null) {
            mSpContent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    CharSequence selected = mSpinnerEntries[i];
                    Log.i(TAG, "onItemSelected:" + selected);
                    if (mOnSpinnerListener != null)
                        mOnSpinnerListener.onItemSelected(i, selected);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    Log.i(TAG, "onItemSelected");
                }
            });
        }

        if (mSpinnerText != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) mSpContent.getAdapter();
            int position = adapter.getPosition(mSpinnerText);
            mSpContent.setSelection(position);
        }

        if (mInputText != null) mEtContent.setText(mInputText);

        if (mInputHint != null) mEtContent.setHint(mInputHint);

        if (mInputType == INPUT_TYPE_NUMBER) {
            mEtContent.setInputType(InputType.TYPE_CLASS_NUMBER);
        }

        if (mInputType == INPUT_TYPE_TEXT) {
            mEtContent.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        if (mInputType == INPUT_TYPE_ALPHANUMERIC) {
            Log.d(TAG, "onCreateView:inputType==INPUT_TYPE_ALPHANUMERIC");
            ArrayList<InputFilter> curInputFilters = new ArrayList<>(Arrays.asList(mEtContent.getFilters()));
            curInputFilters.add(new AlphaNumericInputFilter());
            InputFilter[] newInputFilters = curInputFilters.toArray(new InputFilter[curInputFilters.size()]);
            mEtContent.setFilters(newInputFilters);
        }

        if (mInputTypeCaps == INPUT_TYPE_LOWERCASE) {
            ArrayList<InputFilter> curInputFilters = new ArrayList<>(Arrays.asList(mEtContent.getFilters()));
            curInputFilters.add(new InputFilter.AllCaps() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                    return String.valueOf(source).toLowerCase();
                }
            });
            InputFilter[] newInputFilters = curInputFilters.toArray(new InputFilter[curInputFilters.size()]);
            mEtContent.setFilters(newInputFilters);
        }

        if (mInputTypeCaps == INPUT_TYPE_UPPERCASE) {
            ArrayList<InputFilter> curInputFilters = new ArrayList<>(Arrays.asList(mEtContent.getFilters()));
            curInputFilters.add(new InputFilter.AllCaps());
            InputFilter[] newInputFilters = curInputFilters.toArray(new InputFilter[curInputFilters.size()]);
            mEtContent.setFilters(newInputFilters);
        }

        if (mInputTypeLine == INPUT_TYPE_SINGLELINE) {
            mEtContent.setMaxLines(1);
        }

        if (mInputTypeLine == INPUT_TYPE_MULTILINE) {
            mEtContent.setSingleLine(false);
            // mEtContent.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
            // mEtContent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            mEtContent.setInputType(mEtContent.getInputType() | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            mEtContent.setLines(1);
            mEtContent.setMaxLines(20);
            mEtContent.setVerticalScrollBarEnabled(true);
            mEtContent.setMovementMethod(ScrollingMovementMethod.getInstance());
            mEtContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
            mEtContent.setGravity(Gravity.TOP | Gravity.START);
        }

        return mView;
    }

    public void setSpinnerText(String itemName) {
        Bundle args = getArguments();
        args.putString(EXTRA_SPINNER_TEXT, itemName);
        setArguments(args);
        /*ArrayAdapter<String> adapter = (ArrayAdapter<String>) mSpContent.getAdapter();
        int position = adapter.getPosition(itemName);
        mSpContent.setSelection(position);*/
    }

    public void setInputHint(String hint) {
        Bundle args = getArguments();
        args.putString(EXTRA_INPUT_HINT, hint);
        setArguments(args);
    }

    public void setInputText(String text) {
        Bundle args = getArguments();
        args.putString(EXTRA_INPUT_TEXT, text);
        setArguments(args);
    }

    public void setInputTypeNumber() {
        Bundle args = getArguments();
        args.putInt(EXTRA_INPUT_TYPE, INPUT_TYPE_NUMBER);
        setArguments(args);
    }

    public void setInputTypeText() {
        Bundle args = getArguments();
        args.putInt(EXTRA_INPUT_TYPE, INPUT_TYPE_TEXT);
        setArguments(args);
    }

    public void setInputTypeAlphanumeric() {
        Bundle args = getArguments();
        args.putInt(EXTRA_INPUT_TYPE, INPUT_TYPE_ALPHANUMERIC);
        setArguments(args);
    }

    public void setInputTypeLowercase() {
        Bundle args = getArguments();
        args.putInt(EXTRA_INPUT_TYPE_CAPS, INPUT_TYPE_LOWERCASE);
        setArguments(args);
    }

    public void setInputTypeUppercase() {
        Bundle args = getArguments();
        args.putInt(EXTRA_INPUT_TYPE_CAPS, INPUT_TYPE_UPPERCASE);
        setArguments(args);
    }

    public void setInputTypeSingleline() {
        Bundle args = getArguments();
        args.putInt(EXTRA_INPUT_TYPE_LINE, INPUT_TYPE_SINGLELINE);
        setArguments(args);
    }

    public void setInputTypeMultiline() {
        Bundle args = getArguments();
        args.putInt(EXTRA_INPUT_TYPE_LINE, INPUT_TYPE_MULTILINE);
        setArguments(args);
    }

    public int getSpinnerIndex() {
        this.mSpinnerIndex = mSpContent.getSelectedItemPosition();
        return mSpinnerIndex;
    }

    public String getInputText() {
        this.mInputText = mEtContent.getText().toString();
        return mInputText;
    }

    @OnClick(R.id.confirm_button)
    void confirm() {
        Log.i(TAG, "confirm:listener:" + mOnButtonSelectListener);
        if (mOnButtonSelectListener == null) return;
        mOnButtonSelectListener.onConfirm(this);
        // dismiss();
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

    private interface OnSpinnerSelectListener extends Parcelable {
        void onItemSelected(int position, CharSequence selected);
    }

    public static class OnSpinnerListener implements OnSpinnerSelectListener {
        @Override
        public void onItemSelected(int position, CharSequence selected) {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
        }
    }

    private interface OnButtonSelectListener extends Parcelable {
        void onConfirm(DialogInputFragment inputDialog);

        void onDeny();
    }

    public static class OnSelectListener implements OnButtonSelectListener {
        @Override
        public void onConfirm(DialogInputFragment inputDialog) {
            inputDialog.dismiss();
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

    public static class AlphaNumericInputFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            // Only keep characters that are alphanumeric
            StringBuilder builder = new StringBuilder();
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if ((isCharAllowed(c) || Character.isDigit(c)) && !Character.isWhitespace(c)) {
                    builder.append(c);
                }
            }

            // If all characters are valid, return null, otherwise only return the filtered characters
            boolean allCharactersValid = (builder.length() == end - start);
            return allCharactersValid ? null : builder.toString();
        }

        private boolean isCharAllowed(char c) {
            Pattern ps = Pattern.compile("^[a-zA-Z ]+$");
            Matcher ms = ps.matcher(String.valueOf(c));
            return ms.matches();
        }
    }
}