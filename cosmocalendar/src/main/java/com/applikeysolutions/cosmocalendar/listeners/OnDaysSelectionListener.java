package com.applikeysolutions.cosmocalendar.listeners;

import com.applikeysolutions.cosmocalendar.model.Day;

import java.util.List;

public interface OnDaysSelectionListener {
    void onDaysSelected(List<Day> selectedDays);
}
