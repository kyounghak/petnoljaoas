package com.chaigene.petnolja.event;

import android.util.Log;

import com.chaigene.petnolja.ui.fragment.RootFragment;

public class StartActivityEvent {

    public static final String TAG = "StartActivityEvent";

    public final RootFragment rootFragment;

    public StartActivityEvent(RootFragment fragment) {
        Log.i(TAG, "StartActivityEvent:fragment:" + fragment);
        this.rootFragment = fragment;
    }
}