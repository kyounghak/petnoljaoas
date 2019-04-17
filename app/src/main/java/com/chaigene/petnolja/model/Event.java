package com.chaigene.petnolja.model;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

@IgnoreExtraProperties
public class Event extends STRObject {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_START_DATE = "startDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_IS_POPUP = "isPopup";
    public static final String FIELD_IS_ENABLED = "isEnabled";
    public static final String FIELD_CREATED_DATE = "createdDate";

    private String title;
    private Date startDate;
    private Date endDate;
    private boolean isPopup;
    private boolean isEnabled;
    private Date createdDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // Ref: https://firebase.google.com/docs/firestore/reference/android/ServerTimestamp
    @ServerTimestamp
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @ServerTimestamp
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isPopup() {
        return isPopup;
    }

    public void setPopup(boolean popup) {
        isPopup = popup;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", isPopup=" + isPopup +
                ", isEnabled=" + isEnabled +
                ", createdDate=" + createdDate +
                '}';
    }
}