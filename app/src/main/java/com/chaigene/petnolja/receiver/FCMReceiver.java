package com.chaigene.petnolja.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

public class FCMReceiver extends BroadcastReceiver {
    public static final String TAG = "FCMReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");

        RemoteMessage remoteMessage = intent.getParcelableExtra("extra_remote_message");

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "onReceive:data: " + remoteMessage.getData());
        }

        /*String body = remoteMessage.getNotification().getBody();
        if (body != null) {
            Log.d(TAG, "onReceive:body: " + body);
        }*/
    }
}