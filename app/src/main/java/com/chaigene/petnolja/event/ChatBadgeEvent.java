package com.chaigene.petnolja.event;

import android.util.Log;

public class ChatBadgeEvent {
    public static final String TAG = "ChatBadgeEvent";

    public final int badgeCount;

    public ChatBadgeEvent(int badgeCount) {
        Log.i(TAG, "ChatBadgeEvent:badgeCount:" + badgeCount);
        this.badgeCount = badgeCount;
    }
}