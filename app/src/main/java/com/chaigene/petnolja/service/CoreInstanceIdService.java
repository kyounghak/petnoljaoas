package com.chaigene.petnolja.service;

import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.chaigene.petnolja.util.NotificationUtil;

public class CoreInstanceIdService extends FirebaseInstanceIdService {
    private static final String TAG = "CoreInstanceIdService";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        Log.i(TAG, "onTokenRefresh");

        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "onTokenRefresh:refreshedToken: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        NotificationUtil.insertToken(token).continueWithTask((Continuation<Void, Task<Void>>) task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "sendRegistrationToServer:ERROR:" + task.getException());
                return null;
            }
            return null;
        });
    }
}