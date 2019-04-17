package com.chaigene.petnolja.ui.view;

// This layout will display its children with rounded corners
// It works with Glide image library placeholders and animations
// It assumes your background is a solid color. If you need the corners to be truly transparent,
// this solution will not work for you.

// Source: http://stackoverflow.com/a/36796095/4729203

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.util.CommonUtil;

public class RoundedCornerLayout extends RelativeLayout {
    private static final String TAG = "RoundedCornerLayout";
    private static final int CORNER_RADIUS_DP = 14;

    private Bitmap maskBitmap;
    private Paint paint;
    private float cornerRadius;

    public RoundedCornerLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public RoundedCornerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public RoundedCornerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setWillNotDraw(false);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundedCornerLayout, defStyle, 0);
            cornerRadius = a.getDimensionPixelSize(R.styleable.RoundedCornerLayout_corner_radius, defStyle);
            a.recycle();
        } else {
            cornerRadius = CommonUtil.dpToPx(context, CORNER_RADIUS_DP);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (isInEditMode()) return;

        if (maskBitmap == null) {
            // cornerRadius = canvas.getWidth() / 2;
            maskBitmap = createMask(canvas.getWidth(), canvas.getHeight());
        }

        canvas.drawBitmap(maskBitmap, 0f, 0f, paint);
    }

    private Bitmap createMask(int width, int height) {
        Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mask);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE); // TODO set your background color as needed

        canvas.drawRect(0, 0, width, height, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRoundRect(new RectF(0, 0, width, height), cornerRadius, cornerRadius, paint);

        return mask;
    }
}