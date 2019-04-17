package com.chaigene.petnolja.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.iid.FirebaseInstanceId;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.model.Token;
import com.chaigene.petnolja.util.NotificationUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class PackageReceiver extends BroadcastReceiver {
    public static final String TAG = "PackageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");
        if (intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);

                String versionName = info.versionName;
                int versionCode = info.versionCode;

                Log.i(TAG, "onReceive:versionCode: " + versionCode + "|versionName: " + versionName);

                onVersionUpgraded(versionCode, versionName);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void onVersionUpgraded(final int versionCode, final String versionName) {
        Log.i(TAG, "onVersionUpgraded:versionCode:" + versionCode + "|versionName:" + versionName);

        final String token = FirebaseInstanceId.getInstance().getToken();
        if (token == null) return;

        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {


                Task<Token> getStableTokenTask = NotificationUtil.getStableToken(token);
                Token stableToken;
                // try {
                stableToken = Tasks.await(getStableTokenTask);
                // } catch (Exception e) {
                //     throw e;
                // return null;
                // }

                // 만약 존재할 경우 업데이트 한다.
                if (stableToken != null) {
                    Task<Void> updateStableTokenTask = NotificationUtil.updateStableToken(stableToken.getId(), versionCode, versionName);
                    try {
                        Tasks.await(updateStableTokenTask);
                    } catch (Exception e) {
                        throw e;
                        // return null;
                    }
                }

                // 만약 존재하지 않을 경우 삽입한다.
                if (stableToken == null) {
                    Token newToken = new Token(token, versionCode, versionName);
                    Task<Void> insertStableTokenTask = NotificationUtil.insertStableToken(newToken);
                    try {
                        Tasks.await(insertStableTokenTask);
                    } catch (Exception e) {
                        throw e;
                        // return null;
                    }
                }

                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, task.getException());
                    return null;
                }
                return null;
            }
        });
    }
}