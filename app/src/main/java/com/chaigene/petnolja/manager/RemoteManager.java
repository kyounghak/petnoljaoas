package com.chaigene.petnolja.manager;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class RemoteManager {
    public static String TAG = "RemoteManager";

    private static volatile RemoteManager mInstance;

    public static RemoteManager getInstance() {
        if (mInstance == null) mInstance = new RemoteManager();
        return mInstance;
    }

    public static void releaseInstance() {
        if (mInstance != null) {
            mInstance.release();
            mInstance = null;
        }
    }

    private void release() {
        // this.mCachedPosts = null;
    }

    private RemoteManager() {
        // this.mCachedPosts = new ArrayList<>();
    }

    public static FirebaseRemoteConfig getRemoteConfig() {
        return FirebaseRemoteConfig.getInstance();
    }
}