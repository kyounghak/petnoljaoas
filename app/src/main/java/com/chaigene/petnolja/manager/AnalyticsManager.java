package com.chaigene.petnolja.manager;

import android.app.Activity;
import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsManager {
    public static String TAG = "AnalyticsManager";

    public static FirebaseAnalytics getInstance(Context context) {
        return FirebaseAnalytics.getInstance(context);
    }

    // TODO: 탭이라면 상관이 없는데 프래그먼트 형태는 계속해서 반복 사용되기 때문에 큰 의미가 없을 것 같다.
    // Ref: https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics#setCurrentScreen(android.app.Activity, java.lang.String, java.lang.String)
    public static void setCurrentScreen(Activity activity, String screenName, String screenClassOverride) {
        Context context = activity.getApplicationContext();
        getInstance(context).setCurrentScreen(activity, screenName, screenClassOverride /* class override */);
    }

    // TODO: userId만 설정해도 괜찮은걸까? 닉네임이나 이메일은 기록하지 않는 것이 좋을까? 편의를 위해서라면 기록하는 것이 좋을 것 같다.
    public static void setUserId(Context context, String userId) {
        setUserProperty(context, "userId", userId);
    }

    public static void setUserProperty(Context context, String name, String value) {
        getInstance(context).setUserProperty(name, value);
    }
}