package com.applikeysolutions.cosmocalendar.view.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.applikeysolutions.customizablecalendar.R;

import androidx.annotation.Nullable;

public abstract class RangeSelectionView extends FrameLayout {
    public RangeSelectionView(Context context) {
        super(context);
    }

    public RangeSelectionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RangeSelectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RangeSelectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void initView(Context context) {
        View.inflate(context, R.layout.view_selection_bar_range, this);
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    public void a() {

    }

    public abstract void b();
}
