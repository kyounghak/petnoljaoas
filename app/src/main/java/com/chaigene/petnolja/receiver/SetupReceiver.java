package com.chaigene.petnolja.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chaigene.petnolja.service.CoreService;

public class SetupReceiver extends BroadcastReceiver {
    public static final String TAG = "SetupReceiver";

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "SetupReceiver, onReceive, action:" + action);

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            CoreService.startService(context);
        } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
        } else if (action.equals(Intent.ACTION_PACKAGE_REPLACED)) {

            String replacedPackageName = intent.getData().getSchemeSpecificPart();
            Log.i(TAG, String.format("onReceive, packageName:%s, replacedPackageName:%s",
                    context.getPackageName(),
                    replacedPackageName));

            if (replacedPackageName.equals(context.getPackageName())) {
                CoreService.startService(context);
            }
        }
    }
}