package com.chaigene.petnolja.hashtag;

import androidx.annotation.ColorInt;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

/**
 * Created by danylo.volokh on 12/22/2015.
 * This class is a combination of {@link android.text.style.ForegroundColorSpan}
 * and {@link ClickableSpan}.
 * <p>
 * You can set a color of this span plus set a click listener
 */
public class ClickableForegroundColorSpan extends ClickableSpan {

    private OnSpanClickListener mOnSpanClickListener;

    public interface OnSpanClickListener {
        void onSpanClicked(String hashTag);
    }

    private final int mColor;

    public ClickableForegroundColorSpan(@ColorInt int color, OnSpanClickListener listener) {
        mColor = color;
        mOnSpanClickListener = listener;

        if (mOnSpanClickListener == null) {
            throw new RuntimeException("constructor, click listener not specified. Are you sure you need to use this class?");
        }
    }

    /**
     * 여기서 해쉬태그와 같은 느낌으로 색상을 변경해준다.
     */
    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(mColor);
    }

    @Override
    public void onClick(View widget) {
        CharSequence text = ((TextView) widget).getText();

        Spanned s = (Spanned) text;
        int start = s.getSpanStart(this);
        int end = s.getSpanEnd(this);

        String hashtag = text.subSequence(start + 1/*skip "#" sign*/, end).toString();
        if (TextUtils.isEmpty(hashtag)) return;

        mOnSpanClickListener.onSpanClicked(hashtag);
    }
}
