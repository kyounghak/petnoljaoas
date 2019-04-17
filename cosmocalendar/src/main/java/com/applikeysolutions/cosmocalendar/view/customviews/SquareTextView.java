package com.applikeysolutions.cosmocalendar.view.customviews;

import android.content.Context;

import androidx.appcompat.widget.AppCompatTextView;

// TIP: 정사각형 형태의 TextView이다.
// 캘린더 상단에 월화수목금토일을 표시할 때 사용된다.
public class SquareTextView extends AppCompatTextView {

    public SquareTextView(Context context) {
        super(context);
    }

    //Square view
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
