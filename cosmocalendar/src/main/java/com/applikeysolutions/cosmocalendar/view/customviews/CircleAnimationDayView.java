package com.applikeysolutions.cosmocalendar.view.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.applikeysolutions.cosmocalendar.model.Day;
import com.applikeysolutions.cosmocalendar.selection.SelectionState;
import com.applikeysolutions.cosmocalendar.utils.CalendarUtils;
import com.applikeysolutions.cosmocalendar.view.CalendarView;

import androidx.appcompat.widget.AppCompatTextView;

// TODO: ViewGroup로 변경해야 한다.
public class CircleAnimationDayView extends LinearLayout {
    public static final String TAG = "CircleAnimationDayView";

    private SelectionState selectionState;
    private CalendarView calendarView;

    private int animationProgress;
    private boolean clearView;
    private boolean stateChanged;

    //Circle
    private Paint circlePaint;
    private Paint circleUnderPaint;
    private Day day;
    private int circleColor;

    //Variable to fix bugs when cannot start animation during scroll/fast scroll
    //seems like animation can't be done on views that are not visible on screen
    private boolean animationStarted;
    private long animationStartTime;

    //Start/End range half rectangle
    private Paint rectanglePaint;
    private Rect rectangle;

    //Rectangle
    private Paint backgroundRectanglePaint;
    private Rect backgroundRectangle;

    public static final int DEFAULT_PADDING = 10;
    public static final int MAX_PROGRESS = 100;
    public static final long SELECTION_ANIMATION_DURATION = 300;

    public CircleAnimationDayView(Context context) {
        super(context);
    }

    public CircleAnimationDayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleAnimationDayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // Ref: https://stackoverflow.com/a/17516300/4729203
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.i(TAG, "onLayout:changed:" + changed + "|l:" + l + "|t:" + t + "|r:" + r + "|b:" + b);
        super.onLayout(changed, l, t, r, b);
    }

    // Square view
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            // For making all day views same height (ex. screen width 1080 and we have days with width 154/154/155/154/154/155/154)
            int what = CalendarUtils.getCircleWidth(getContext()) + MeasureSpec.EXACTLY;
            Log.i(TAG, "onMeasure:widthMeasureSpec:" + widthMeasureSpec + "|heightMeasureSpec:" + what);
            super.onMeasure(widthMeasureSpec, what);
        } else {
            Log.i(TAG, "onMeasure:widthMeasureSpec:" + widthMeasureSpec + "|heightMeasureSpec:" + widthMeasureSpec);
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }

    /*@Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }*/

    @Override
    public void dispatchDraw(Canvas canvas) {
        Log.i(TAG, "onDraw:clearView:" + clearView + "|day:" + day + "|selectionState:" + selectionState);

        if (clearView) {
            clearVariables();
        }

        if (selectionState != null) {
            switch (selectionState) {
                case START_RANGE_DAY:
                case END_RANGE_DAY:
                    drawRectangle(canvas);
                    drawCircleUnder(canvas);
                    drawCircle(canvas);
                    break;

                case START_RANGE_DAY_WITHOUT_END:      // TIP: 최초 시작일 선택일 때.
                    drawCircle(canvas);
                    break;

                case SINGLE_DAY:
                    //Animation not started yet
                    //progress not MAX_PROGRESS
                    boolean condition1 = !animationStarted
                            && animationProgress != MAX_PROGRESS;

                    //Animation started
                    //but was terminated (more than SELECTION_ANIMATION_DURATION have passed from animationStartTime)
                    //progress not MAX_PROGRESS
                    long currentTime = System.currentTimeMillis();
                    boolean condition2 = animationStarted
                            && (currentTime > animationStartTime + SELECTION_ANIMATION_DURATION)
                            && animationProgress != MAX_PROGRESS;

                    if (condition1 || condition2) {
                        animateView();
                    } else {
                        drawCircle(canvas);
                    }
                    break;

                case RANGE_DAY:
                    drawBackgroundRectangle(canvas);
                    break;
            }
        }

        // super.draw(canvas);
        // super.onDraw가 먼저 호출되면 Text가 가려져버린다.
        super.dispatchDraw(canvas);
    }

    // TODO: Circle을 그린다.
    private void drawCircle(Canvas canvas) {
        if (animationProgress == 100) {
            if (day != null) {
                day.setSelectionCircleDrawed(true);
            }
        }
        if (circlePaint == null || stateChanged) {
            createCirclePaint();
        }

        final int diameter = getWidth() - DEFAULT_PADDING * 2;
        final int diameterProgress = animationProgress * diameter / MAX_PROGRESS;

        setBackgroundColor(Color.TRANSPARENT);
        canvas.drawCircle(getWidth() / 2, getWidth() / 2, diameterProgress / 2, circlePaint);
    }

    private void drawCircleUnder(Canvas canvas) {
        Log.i(TAG, "drawCircleUnder");
        if (circleUnderPaint == null || stateChanged) {
            createCircleUnderPaint();
        }
        final int diameter = getWidth() - DEFAULT_PADDING * 2;
        canvas.drawCircle(getWidth() / 2, getWidth() / 2, diameter / 2, circleUnderPaint);
    }

    private void createCirclePaint() {
        circlePaint = new Paint();
        circlePaint.setColor(circleColor);
        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    private void createCircleUnderPaint() {
        circleUnderPaint = new Paint();
        circleUnderPaint.setColor(calendarView.getSelectedDayBackgroundColor());
        circleUnderPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    // TIP: SelectionState가 Range일 경우 Rectangle을 그린다.
    private void drawRectangle(Canvas canvas) {
        if (rectanglePaint == null) {
            createRectanglePaint();
        }
        // Rect를 가져온다.
        if (rectangle == null) {
            rectangle = getRectangleForState();
        }
        canvas.drawRect(rectangle, rectanglePaint);
    }

    // TIP: Rectangle에 대한 Paint를 반환한다.
    // Paint는 색깔이나 Antialias에 대한 정보들이 담겨있는 객체이다.
    private void createRectanglePaint() {
        rectanglePaint = new Paint();
        rectanglePaint.setColor(calendarView.getSelectedDayBackgroundColor());
        rectanglePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    // TIP: Range일 경우에는 Background에 Rectangle을 그려준다.
    // (사실상 getSelectedDayBackgroundColor로 그려주는 색깔 차이 밖에 없다.)
    // (당연히 정사각형이다.)
    private void drawBackgroundRectangle(Canvas canvas) {
        if (backgroundRectanglePaint == null) {
            createBackgroundRectanglePaint();
        }
        if (backgroundRectangle == null) {
            backgroundRectangle = getRectangleForState();
        }
        canvas.drawRect(backgroundRectangle, backgroundRectanglePaint);
    }

    private void createBackgroundRectanglePaint() {
        backgroundRectanglePaint = new Paint();
        backgroundRectanglePaint.setColor(calendarView.getSelectedDayBackgroundColor());
        backgroundRectanglePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    public SelectionState getSelectionState() {
        return selectionState;
    }

    // TIP: DayHolder에서 뷰를 렌더링 할 때 SelectionState에 따라서 Circle을 그리기 위한 용도로 이 메서드가 호출된다.
    public void setSelectionStateAndAnimate(SelectionState state, CalendarView calendarView, Day day) {
        Log.i(TAG, "setSelectionStateAndAnimate:state:" + state.toString() + "|day:" + day.toString());
        isStateChanged(state);
        selectionState = state;
        this.calendarView = calendarView;
        // 현재 날짜에 대한 SelectionState를 설정한다.
        day.setSelectionState(state);
        this.day = day;

        if (selectionState != null && calendarView != null) {
            switch (selectionState) {
                case START_RANGE_DAY:
                    circleColor = calendarView.getSelectedDayBackgroundStartColor();
                    break;

                case END_RANGE_DAY:
                    circleColor = calendarView.getSelectedDayBackgroundEndColor();
                    break;

                case START_RANGE_DAY_WITHOUT_END:
                    setBackgroundColor(Color.TRANSPARENT);
                    circleColor = calendarView.getSelectedDayBackgroundStartColor();
                    break;

                case SINGLE_DAY:
                    circleColor = calendarView.getSelectedDayBackgroundColor();
                    setBackgroundColor(Color.TRANSPARENT);
                    break;
            }
        }
        animateView();
    }

    // TIP: Rectangle에 대한 Rect를 반환한다.
    // Rect는 Rectangle이 그려지는 4가지 점에 대한 영역 정보를 담고있는 객체이다.
    private Rect getRectangleForState() {
        Rect rect;
        int width = getWidth();
        int height = getHeight();
        switch (selectionState) {
            case START_RANGE_DAY:   // 시작일일 경우에는 중앙부터 끝까지 그려준다.
                rect = new Rect(width / 2, DEFAULT_PADDING, width, height - DEFAULT_PADDING);
                return rect;
            case END_RANGE_DAY:     // 종료일일 경우에는 시작부터 중앙까지 그려준다.
                rect = new Rect(0, DEFAULT_PADDING, width / 2, height - DEFAULT_PADDING);
                return rect;
            case RANGE_DAY:         // Range일 경우에는 정사각형으로 그려준다.
                rect = new Rect(0, DEFAULT_PADDING, width, height - DEFAULT_PADDING);
                return rect;
            default:
                return null;
        }
    }

    private void animateView() {
        Log.i(TAG, "animateView");
        CircularFillAnimation animation = new CircularFillAnimation();
//        animation.setInterpolator(new BounceInterpolator()); //just for fun
        animation.setDuration(SELECTION_ANIMATION_DURATION);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animationStarted = true;
                animationStartTime = System.currentTimeMillis();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animationStarted = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        startAnimation(animation);
        invalidate();
    }

    @Override
    public void startAnimation(Animation animation) {
        super.startAnimation(animation);
        Log.i(TAG, "startAnimation");
    }

    @Override
    public void invalidate() {
        super.invalidate();
        Log.i(TAG, "invalidate");
    }

    // TODO: 필드의 값이 null이거나 새로운 state일 때 필드의 값을 true로 바꿔주는 메서드인데
    // 왜 함수명이 boolean에 대한 getter 형식인 모르겠다.
    private void isStateChanged(SelectionState newState) {
        stateChanged = (selectionState == null || selectionState != newState);
    }

    public void setAnimationProgress(int animationProgress) {
        this.animationProgress = animationProgress;
    }

    public void clearView() {
        Log.i(TAG, "clearView");
        // selectionState가 null이 아닐 때만 true로 설정한다.
        if (selectionState != null) {
            clearView = true;
            invalidate();
        }
    }

    private void clearVariables() {
        Log.i(TAG, "clearVariables");

        selectionState = null;
        calendarView = null;
        circlePaint = null;
        rectanglePaint = null;
        rectangle = null;

        stateChanged = false;

        circleColor = 0;

        animationProgress = 0;
        animationStarted = false;
        animationStartTime = 0;

        setBackgroundColor(Color.TRANSPARENT);

        clearView = false;
    }

    public void showAsCircle(int circleColor) {
        this.circleColor = circleColor;
        animationProgress = 100;
        setMinimumWidth(CalendarUtils.getCircleWidth(getContext()));
        setMinimumHeight(CalendarUtils.getCircleWidth(getContext()));
        // setWidth(CalendarUtils.getCircleWidth(getContext()));
        // setHeight(CalendarUtils.getCircleWidth(getContext()));
        /*setLayoutParams(new FrameLayout.LayoutParams(
                CalendarUtils.getCircleWidth(getContext()),
                CalendarUtils.getCircleWidth(getContext())
        ));*/
        requestLayout();
        invalidate();
    }

    // 단일 Circle을 그려준다.
    public void showAsSingleCircle(CalendarView calendarView) {
        clearVariables();
        selectionState = SelectionState.SINGLE_DAY;
        showAsCircle(calendarView.getSelectedDayBackgroundColor());
    }

    // 시작일의 Circle을 그려준다.
    public void showAsStartCircle(CalendarView calendarView, boolean animate) {
        if (animate) {
            clearVariables();
        }
        this.calendarView = calendarView;
        selectionState = SelectionState.START_RANGE_DAY;
        showAsCircle(calendarView.getSelectedDayBackgroundStartColor());
    }

    // 종료일이 존재하지 않는 시작일의 Circle을 그려준다.
    public void showAsStartCircleWithouEnd(CalendarView calendarView, boolean animate) {
        if (animate) {
            clearVariables();
        }
        this.calendarView = calendarView;
        selectionState = SelectionState.START_RANGE_DAY_WITHOUT_END;
        showAsCircle(calendarView.getSelectedDayBackgroundStartColor());
    }

    // 종료일의 Circle을 그려준다.
    public void showAsEndCircle(CalendarView calendarView, boolean animate) {
        if (animate) {
            clearVariables();
        }
        this.calendarView = calendarView;
        selectionState = SelectionState.END_RANGE_DAY;
        showAsCircle(calendarView.getSelectedDayBackgroundEndColor());
    }

    // Animation
    class CircularFillAnimation extends Animation {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {
            int progress = (int) (interpolatedTime * 100);
            CircleAnimationDayView.this.setAnimationProgress(progress);
            CircleAnimationDayView.this.requestLayout();
        }
    }
}
