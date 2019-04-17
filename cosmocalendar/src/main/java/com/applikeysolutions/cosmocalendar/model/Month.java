package com.applikeysolutions.cosmocalendar.model;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Month {

    public static final String DEFAULT_PATTERN = "yyyy MMMM";

    private List<Day> days;
    private Day firstDay;
    private boolean synced;

    public Month(Day firstDay, List<Day> days) {
        this.days = days;
        this.firstDay = firstDay;
    }

    public Day getFirstDay() {
        return firstDay;
    }

    public void setFirstDay(Day firstDay) {
        this.firstDay = firstDay;
    }

    public List<Day> getDays() {
        return days;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    /**
     * Returns selected days that belong only to current month
     *
     * @return
     */
    // TODO: 이 메서드의 정확한 목적을 모르겠음.
    public List<Day> getDaysWithoutTitlesAndOnlyCurrent() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(firstDay.getCalendar().getTime());
        int currentMonth = calendar.get(Calendar.MONTH);

        List<Day> result = new ArrayList<>();
        for (Day day : days) {
            calendar.setTime(day.getCalendar().getTime());
            if (!(day instanceof DayOfWeek) && calendar.get(Calendar.MONTH) == currentMonth) {
                result.add(day);
            }
        }
        return result;
    }

    public String getMonthName() {
        return getMonthName(null);
    }

    public String getMonthName(String pattern) {
        // FALLBACK
        if (TextUtils.isEmpty(pattern)) pattern = DEFAULT_PATTERN;
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(firstDay.getCalendar().getTime());
    }
}
