package com.chaigene.petnolja.hashtag;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This is a helper class that should be used with {@link android.widget.EditText} or {@link TextView}
 * In order to have hash-tagged words highlighted. It also provides a click listeners for every hashtag
 * <p>
 * Example :
 * #ThisIsHashTagWord
 * #ThisIsFirst#ThisIsSecondHashTag
 * #hashtagendsifitfindsnotletterornotdigitsignlike_thisIsNotHighlithedArea
 */
public final class HashTagHelper implements ClickableForegroundColorSpan.OnSpanClickListener {

    public static final String TAG = "HashTagHelper";

    /**
     * If this is not null then  all of the symbols in the List will be considered as valid symbols of hashtag
     * For example :
     * mAdditionalHashTagChars = {'$','_','-'}
     * it means that hashtag: "#this_is_hashtag-with$dollar-sign" will be highlighted.
     * <p>
     * Note: if mAdditionalHashTagChars would be "null" only "#this" would be highlighted
     */
    private final List<Character> mAdditionalHashTagChars;
    private TextView mTextView;
    private int mHashTagWordColor;

    private OnHashTagClickListener mOnHashTagClickListener;

    public static final class Creator {

        private Creator() {
        }

        public static HashTagHelper create(int color, OnHashTagClickListener listener) {
            return new HashTagHelper(color, listener, null);
        }

        public static HashTagHelper create(int color, OnHashTagClickListener listener, char... additionalHashTagChars) {
            return new HashTagHelper(color, listener, additionalHashTagChars);
        }

    }

    public interface OnHashTagClickListener {
        void onHashTagClicked(String hashTag);
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
            if (text.length() > 0) {
                eraseAndColorizeAllText(text);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private HashTagHelper(int color, OnHashTagClickListener listener, char... additionalHashTagCharacters) {
        mHashTagWordColor = color;
        mOnHashTagClickListener = listener;
        mAdditionalHashTagChars = new ArrayList<>();

        if (additionalHashTagCharacters != null) {
            for (char additionalChar : additionalHashTagCharacters) {
                mAdditionalHashTagChars.add(additionalChar);
            }
        }
    }

    public void handle(TextView textView) {
        if (mTextView == null) {
            mTextView = textView;
            mTextView.addTextChangedListener(mTextWatcher);

            // in order to use spannable we have to set buffer type
            mTextView.setText(mTextView.getText(), TextView.BufferType.SPANNABLE);

            if (mOnHashTagClickListener != null) {
                // we need to set this in order to getUserPosts onClick event
                mTextView.setMovementMethod(LinkMovementMethod.getInstance());

                // after onClick clicked text become highlighted
                mTextView.setHighlightColor(Color.TRANSPARENT);
            } else {
                // hash tags are not clickable, no need to change these parameters
            }

            setColorsToAllHashTags(mTextView.getText());
        } else {
            throw new RuntimeException("TextView is not null. You need to create a unique HashTagHelper for every TextView");
        }

    }

    private void eraseAndColorizeAllText(CharSequence text) {

        Spannable spannable = ((Spannable) mTextView.getText());

        CharacterStyle[] spans = spannable.getSpans(0, text.length(), CharacterStyle.class);
        for (CharacterStyle span : spans) {
            // TODO: 문제는 여기서 다 지워버린다는 것이 문제다.
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            CharSequence spanText = text.subSequence(spanStart, spanEnd);
            if (spanText.toString().contains("#")) spannable.removeSpan(span);
        }

        setColorsToAllHashTags(text);
    }

    private void setColorsToAllHashTags(CharSequence text) {

        int startIndexOfNextHashSign;

        // TextView의 모든 문자를 기준으로 loop를 돈다.
        int index = 0;
        while (index < text.length() - 1) {
            char sign = text.charAt(index);
            // we assume it is next. if if was not changed by findNextValidHashTagChar then index will be incremented by 1
            int nextNotLetterDigitCharIndex = index + 1;
            // 만약 현재 index의 char가 #일 경우에 해당 해쉬태그가 언제 끝나는지를 검색한다.
            if (sign == '#') {
                startIndexOfNextHashSign = index;

                nextNotLetterDigitCharIndex = findNextValidHashTagChar(text, startIndexOfNextHashSign);
                // Log.d(TAG, "setColorsToAllHashTags:nextNotLetterDigitCharIndex:" + nextNotLetterDigitCharIndex);

                // 해쉬태그만 존재할 경우에는 아에 span 처리하지 않는다.
                boolean isNextSpaceChar = index + 1 == nextNotLetterDigitCharIndex;

                if (!isNextSpaceChar)
                    setColorForHashTagToTheEnd(startIndexOfNextHashSign, nextNotLetterDigitCharIndex);
            }

            // 해쉬태그가 아니라면 계속 1씩 증가한다.
            index = nextNotLetterDigitCharIndex;
        }
    }

    // 어디까지를 해쉬태그로 인정할 것인가를 결정하는 메서드이다.
    private int findNextValidHashTagChar(CharSequence text, int start) {
        // Log.i(TAG, "findNextValidHashTagChar");

        int nonLetterDigitCharIndex = -1;
        // skip first sign '#"
        for (int index = start + 1; index < text.length(); index++) {

            // 현재 index의 char
            char sign = text.charAt(index);

            // Log.d(TAG, "findNextValidHashTagChar:char:" + sign + "|isWhitespace:" + Character.isWhitespace(sign));
            // Log.d(TAG, "findNextValidHashTagChar:char:" + sign + "|isSpaceChar:" + Character.isSpaceChar(sign));
            boolean isValidSign = Character.isLetterOrDigit(sign) || mAdditionalHashTagChars.contains(sign);
            // Log.i(TAG, "findNextValidHashTagChar:char:" + sign + "|isValidSign:" + isValidSign);
            if (!isValidSign) {
                nonLetterDigitCharIndex = index;
                break;
            }
        }
        if (nonLetterDigitCharIndex == -1) {
            // we didn't find non-letter. We are at the end of text
            nonLetterDigitCharIndex = text.length();
        }

        return nonLetterDigitCharIndex;
    }

    // 실질적으로 컬러를 입혀주는 메서드이다.
    private void setColorForHashTagToTheEnd(int startIndex, int endIndex) {
        Log.i(TAG, "setColorForHashTagToTheEnd:startIndex:" + startIndex + "|endIndex:" + endIndex);
        Spannable s = (Spannable) mTextView.getText();

        CharacterStyle span;

        if (mOnHashTagClickListener != null) {
            span = new ClickableForegroundColorSpan(mHashTagWordColor, this);
        } else {
            // no need for clickable span because it is messing with selection when click
            span = new ForegroundColorSpan(mHashTagWordColor);
        }

        s.setSpan(span, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // TODO: # 이후에 띄워쓰기를 할 경우 " " 자체가 해시태그로 인식되어 삽입된다.
    // 값이 empty일 경우는 List에 추가하지 않도록 한다.
    public List<String> getAllHashTags(boolean withHashes) {

        // 전체 텍스트에 대한 String
        String text = mTextView.getText().toString();
        // 전체 텍스트에 대한 Spannable
        Spannable spannable = (Spannable) mTextView.getText();

        // 중복을 방지하기 위해서 Map을 사용한다.
        Set<String> hashTags = new LinkedHashSet<>();

        for (CharacterStyle span : spannable.getSpans(0, text.length(), CharacterStyle.class)) {
            // # 심볼이 있는 span만 추출한다
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            CharSequence spanText = text.subSequence(spanStart, spanEnd);
            if (!spanText.toString().contains("#")) continue;

            // Skip "#" sign with 'withHashes' flag.
            int start = !withHashes ? spannable.getSpanStart(span) + 1 : spannable.getSpanStart(span);
            String hashtag = text.substring(start, spannable.getSpanEnd(span));
            // 이미 스페이스 방지 처리를 해두었기 때문에 사실상 필요없는 코드이다.
            if (TextUtils.isEmpty(hashtag)) continue;

            hashTags.add(hashtag);
        }

        return new ArrayList<>(hashTags);
    }

    public List<String> getAllHashTags() {
        return getAllHashTags(false);
    }

    @Override
    public void onSpanClicked(String hashTag) {
        mOnHashTagClickListener.onHashTagClicked(hashTag);
    }
}
