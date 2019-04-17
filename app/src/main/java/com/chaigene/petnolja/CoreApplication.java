package com.chaigene.petnolja;

import android.content.Context;
import androidx.multidex.MultiDexApplication;
import androidx.appcompat.app.AppCompatDelegate;

import com.facebook.FacebookSdk;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.kakao.auth.IApplicationConfig;
import com.kakao.auth.KakaoAdapter;
import com.kakao.auth.KakaoSDK;

import java.util.Locale;

// Ref: https://developer.android.com/studio/build/multidex.html?hl=ko
public class CoreApplication extends MultiDexApplication {
    public static final String TAG = "CoreApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Source: https://stackoverflow.com/a/37864531/4729203
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Fabric.with(this, new Crashlytics(), new TwitterCore(authConfig), new Digits.Builder().build());
        // TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        // TwitterAuthConfig authConfig = new TwitterAuthConfig(getString(R.string.twitter_consumer_key), getString(R.string.twitter_consumer_secret));
        // Fabric.with(this, new Crashlytics());

        /*TwitterConfig config = new TwitterConfig.Builder(this)
                .twitterAuthConfig(authConfig)
                .debug(true)
                .build();
        Twitter.initialize(config);*/

        // FirebaseApp.initializeApp(this);

        // WTF? (Ref: http://stackoverflow.com/a/41884415/4729203)
        // noinspection deprecation
        FacebookSdk.sdkInitialize(this);

        KakaoSDK.init(new KakaoAdapter() {
            @Override
            public IApplicationConfig getApplicationConfig() {
                return new IApplicationConfig() {
                    @Override
                    public Context getApplicationContext() {
                        return CoreApplication.this;
                    }
                };
            }
        });

        // You will probably be using the Firebase RealTime Database, and since we are creating this application class,
        // go a head and uncomment below line to activate offline feature
        // FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // DatabaseManager.setPersistenceEnabled(true);

        FirebaseAuth.getInstance().setLanguageCode(Locale.KOREAN.getLanguage());

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.WARN);
        // FirebaseFirestore.setLoggingEnabled(true);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        /*DatabaseManager.setPersistenceEnabled(true);
        if (AuthManager.isSignedIn()) {
            OldArticleUtil.timelineKeepSynced();
            OldArticleUtil.exploreKeepSynced();
        }*/

        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        firebaseRemoteConfig.setConfigSettings(configSettings);
    }
}