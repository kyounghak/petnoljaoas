package com.applikeysolutions.cosmocalendar.adapter.viewholder;

import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.applikeysolutions.cosmocalendar.model.Day;
import com.applikeysolutions.cosmocalendar.selection.BaseSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.RangeSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.SelectionState;
import com.applikeysolutions.cosmocalendar.settings.appearance.ConnectedDayIconPosition;
import com.applikeysolutions.cosmocalendar.utils.CalendarUtils;
import com.applikeysolutions.cosmocalendar.view.CalendarView;
import com.applikeysolutions.cosmocalendar.view.customviews.CircleAnimationDayView;
import com.applikeysolutions.customizablecalendar.R;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Calendar;
import java.util.Locale;

public class DayHolder extends BaseDayHolder {
    public static final String TAG = "DayHolder";

    private CircleAnimationDayView llDayView;
    private TextView tvDay;
    private TextView tvQuota;
    private BaseSelectionManager selectionManager;

    public DayHolder(View itemView, CalendarView calendarView) {
        super(itemView, calendarView);
        llDayView = itemView.findViewById(R.id.ll_day_view);
        tvDay = itemView.findViewById(R.id.tv_day_number);
        tvQuota = itemView.findViewById(R.id.tv_quota);
    }

    public void bind(Day day, BaseSelectionManager selectionManager) {
        this.selectionManager = selectionManager;

        tvDay.setText(String.valueOf(day.getDayNumber()));

        // Quota
        if (day.getMaxQuata() != 0) {
            String quota = String.format(
                    Locale.getDefault(),
                    "%d/%d",
                    day.getOccupied(),
                    day.getMaxQuata()
            );
            tvQuota.setText(quota);
            if (day.getMaxQuata() == day.getOccupied()) tvQuota.setTextColor(Color.RED);
        }

        boolean isSelected = selectionManager.isDaySelected(day);
        if (isSelected && !day.isDisabled()) {
            select(day);
        } else {
            // 굳이 매번 unselect를 호출할 필요가 있을까?
            unselect(day);
        }

        // 오늘일 경우.
        if (day.isCurrent()) {
            addCurrentDayIcon(isSelected);
        }

        // TODO: 만약 오늘보다 이전 날짜일 경우 현재 날짜를 Disabled 처리한다.

        Calendar compareCal = day.getCalendar();
        int year = compareCal.get(Calendar.YEAR);
        int month = compareCal.get(Calendar.MONTH);
        int date = compareCal.get(Calendar.DATE);
        compareCal.clear();
        compareCal.set(year, month, date);

        Calendar todayCal = Calendar.getInstance();
        int currentYear = todayCal.get(Calendar.YEAR);
        int currentMonth = todayCal.get(Calendar.MONTH);
        int currentDate = todayCal.get(Calendar.DATE);
        todayCal.clear();
        todayCal.set(currentYear, currentMonth, currentDate);

        boolean isPassed = todayCal.after(compareCal);
        if (isPassed) day.setDisabled(true);

        // Disabled 된 일자일 경우.
        if (day.isDisabled()) {
            tvDay.setTextColor(calendarView.getDisabledDayTextColor());
            tvQuota.setTextColor(calendarView.getDisabledDayTextColor());
        }
    }

    private void addCurrentDayIcon(boolean isSelected) {
        tvDay.setCompoundDrawablePadding((getPadding(getCurrentDayIconHeight(isSelected)) * -1) + 12);
        tvDay.setCompoundDrawablesWithIntrinsicBounds(0, isSelected
                ? calendarView.getCurrentDaySelectedIconRes()
                : calendarView.getCurrentDayIconRes(), 0, 0);
    }

    private int getCurrentDayIconHeight(boolean isSelected) {
        if (isSelected) {
            return CalendarUtils.getIconHeight(calendarView.getContext().getResources(), calendarView.getCurrentDaySelectedIconRes());
        } else {
            return CalendarUtils.getIconHeight(calendarView.getContext().getResources(), calendarView.getCurrentDayIconRes());
        }
    }

    private int getConnectedDayIconHeight(boolean isSelected) {
        if (isSelected) {
            return CalendarUtils.getIconHeight(calendarView.getContext().getResources(), calendarView.getConnectedDaySelectedIconRes());
        } else {
            return CalendarUtils.getIconHeight(calendarView.getContext().getResources(), calendarView.getConnectedDayIconRes());
        }
    }

    // 날짜를 선택한다.
    private void select(Day day) {
        Log.i(TAG, "select:day:" + day);
        if (day.isFromConnectedCalendar()) {
            if (day.isDisabled()) {
                tvDay.setTextColor(day.getConnectedDaysDisabledTextColor());
            } else {
                tvDay.setTextColor(day.getConnectedDaysSelectedTextColor());
            }
            addConnectedDayIcon(true);
        } else {
            tvDay.setTextColor(calendarView.getSelectedDayTextColor());
            tvDay.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            tvQuota.setTextColor(calendarView.getSelectedDayTextColor());
        }

        SelectionState state;
        if (selectionManager instanceof RangeSelectionManager) {
            state = ((RangeSelectionManager) selectionManager).getSelectedState(day);
        } else {
            state = SelectionState.SINGLE_DAY;
        }
        animateDay(state, day);
    }

    // TIP: ???
    private void addConnectedDayIcon(boolean isSelected) {
        tvDay.setCompoundDrawablePadding((getPadding(getConnectedDayIconHeight(isSelected)) * -1) + 12);

        switch (calendarView.getConnectedDayIconPosition()) {
            case ConnectedDayIconPosition.TOP:
                tvDay.setCompoundDrawablesWithIntrinsicBounds(0, isSelected
                        ? calendarView.getConnectedDaySelectedIconRes()
                        : calendarView.getConnectedDayIconRes(), 0, 0);
                break;

            case ConnectedDayIconPosition.BOTTOM:
                tvDay.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, isSelected
                        ? calendarView.getConnectedDaySelectedIconRes()
                        : calendarView.getConnectedDayIconRes());
                break;
        }
    }

    // Day를 Animating 한다.
    private void animateDay(SelectionState state, Day day) {
        Log.i(TAG, "animateDay:state:" + state.toString() + "|day:" + day.toString());
        // TODO: What does 'day.getSelectionState' meaning?
        if (day.getSelectionState() != state) {
            if (day.isSelectionCircleDrawed() && state == SelectionState.SINGLE_DAY) {
                llDayView.showAsSingleCircle(calendarView);
            } else if (day.isSelectionCircleDrawed() && state == SelectionState.START_RANGE_DAY) {
                llDayView.showAsStartCircle(calendarView, false);
            } else if (day.isSelectionCircleDrawed() && state == SelectionState.END_RANGE_DAY) {
                llDayView.showAsEndCircle(calendarView, false);
            } else {
                llDayView.setSelectionStateAndAnimate(state, calendarView, day);
            }
        } else {
            switch (state) {
                case SINGLE_DAY:        // TIP: 단일 선택
                    // TODO: 해당일에 Circle이 이미 그려졌다면?
                    if (day.isSelectionCircleDrawed()) {
                        // 단일 Circle을 그린다.
                        llDayView.showAsSingleCircle(calendarView);
                    } else {
                        llDayView.setSelectionStateAndAnimate(state, calendarView, day);
                    }
                    break;

                case RANGE_DAY:         // TIP: 시작일과 종료일 사이에 있는 뷰의 경우이다.
                    llDayView.setSelectionStateAndAnimate(state, calendarView, day);
                    break;

                case START_RANGE_DAY_WITHOUT_END:
                    // TODO: 종료일이 존재하지 않는 시작일은 도대체 무엇인가?
                    if (day.isSelectionCircleDrawed()) {
                        llDayView.showAsStartCircleWithouEnd(calendarView, false);
                    } else {
                        llDayView.setSelectionStateAndAnimate(state, calendarView, day);
                    }
                    break;

                case START_RANGE_DAY:   // TIP: 시작일
                    // TODO: 해당일에 Circle이 이미 그려졌다면?
                    if (day.isSelectionCircleDrawed()) {
                        llDayView.showAsStartCircle(calendarView, false);
                    } else {
                        llDayView.setSelectionStateAndAnimate(state, calendarView, day);
                    }
                    break;

                case END_RANGE_DAY:     // TIP: 종료일
                    // TODO: 해당일에 Circle이 이미 그려졌다면?
                    if (day.isSelectionCircleDrawed()) {
                        llDayView.showAsEndCircle(calendarView, false);
                    } else {
                        llDayView.setSelectionStateAndAnimate(state, calendarView, day);
                    }
                    break;
            }
        }
    }

    private void unselect(Day day) {
        Log.i(TAG, "unselect:day:" + day.toString());
        int textColor;
        if (day.isFromConnectedCalendar()) {
            // TIP: What is connected???
            if (day.isDisabled()) {
                textColor = day.getConnectedDaysDisabledTextColor();
            } else {
                textColor = day.getConnectedDaysTextColor();
            }
            addConnectedDayIcon(false);
        } else if (day.isWeekend()) {
            // 주말이라면.
            textColor = calendarView.getWeekendDayTextColor();
            tvDay.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        } else {
            textColor = calendarView.getDayTextColor();
            tvDay.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
        day.setSelectionCircleDrawed(false);
        tvDay.setTextColor(textColor);

        // 선택을 취소하면 뷰를 클리어해준다.
        llDayView.clearView();
    }

    private int getPadding(int iconHeight) {
        return (int) (iconHeight * Resources.getSystem().getDisplayMetrics().density);
    }
}
