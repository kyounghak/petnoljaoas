package com.chaigene.petnolja.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.kakao.auth.Session;
import com.kakao.util.helper.SharedPreferencesCache;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.hashtag.AtSignHelper;
import com.chaigene.petnolja.hashtag.HashTagHelper;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
import static android.content.Context.ACTIVITY_SERVICE;

public class CommonUtil {
    public static final String TAG = "CommonUtil";

    // View
    @MainThread
    public static void showViews(View... views) {
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
        }
    }

    @MainThread
    public static void hideViews(View... views) {
        for (View v : views) {
            v.setVisibility(View.GONE);
        }
    }

    @MainThread
    public static void enableViews(View... views) {
        for (View v : views) {
            v.setEnabled(true);
        }
    }

    @MainThread
    public static void disableViews(View... views) {
        for (View v : views) {
            v.setEnabled(false);
        }
    }

    @MainThread
    public static void setVisibilityWithFade(final View view, final int visibility) {
        if (view.getVisibility() == visibility) return;
        String visibilityForDebug = visibility == View.VISIBLE ? "VISIBLE" : visibility == View.GONE ? "GONE" : "INVISIBLE";
        Log.i(TAG, "setVisibilityWithFade:view:" + CommonUtil.getResourceName(view) + "|visibility:" + visibilityForDebug);
        float startAlpha = visibility == View.VISIBLE ? 0.0f : 1.0f;
        float endAlpha = visibility == View.VISIBLE ? 1.0f : 0.0f;
        view.setAlpha(startAlpha);
        if (visibility == View.VISIBLE) view.setVisibility(visibility);
        view.animate().setDuration(300).alpha(endAlpha).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (visibility != View.VISIBLE) view.setVisibility(visibility);
                        view.animate().setListener(null);
                    }
                }
        );
    }

    @MainThread
    public static void setVisibilityWithSlide(final View view, final int visibility) {
        if (view.getVisibility() == visibility) return;
        String visibilityForDebug = visibility == View.VISIBLE ? "VISIBLE" : visibility == View.GONE ? "GONE" : "INVISIBLE";
        Log.i(TAG, "setVisibilityWithSlide:view:" + CommonUtil.getResourceName(view) + "|visibility:" + visibilityForDebug);
        final float startAlpha = visibility == View.VISIBLE ? 0.0f : 1.0f;
        final float endAlpha = visibility == View.VISIBLE ? 1.0f : 0.0f;
        if (visibility == View.VISIBLE) {
            view.setVisibility(visibility);
            view.setAlpha(startAlpha);
        }
        view.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setVisibilityWithSlide:height:" + view.getHeight() + "|measuredHeight:" + view.getMeasuredHeight());
                float startTranslationY = visibility == View.VISIBLE ? -view.getHeight() : 0;
                float endTranslationY = visibility == View.VISIBLE ? 0 : -view.getHeight();
                Log.i(TAG, "setVisibilityWithSlide:startTranslationY:" + startTranslationY + "|endTranslationY:" + endTranslationY);
                view.setTranslationY(startTranslationY);
                view.animate().setDuration(300).alpha(endAlpha).translationY(endTranslationY).setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                if (visibility != View.VISIBLE) view.setVisibility(visibility);
                                view.animate().setListener(null);
                            }
                        }
                );
            }
        });
    }

    @MainThread
    public static void showSnackbar(View subView, String message) {
        View rootView = subView.getRootView().findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        try {
            Field mAccessibilityManagerField = BaseTransientBottomBar.class.getDeclaredField("mAccessibilityManager");
            mAccessibilityManagerField.setAccessible(true);
            AccessibilityManager accessibilityManager = (AccessibilityManager) mAccessibilityManagerField.get(snackbar);
            Field mIsEnabledField = AccessibilityManager.class.getDeclaredField("mIsEnabled");
            mIsEnabledField.setAccessible(true);
            mIsEnabledField.setBoolean(accessibilityManager, false);
            mAccessibilityManagerField.set(snackbar, accessibilityManager);
        } catch (Exception e) {
            Log.d(TAG, "showSnackbar:reflection_error:" + e.toString());
        }
    }

    @MainThread
    public static void showSnackbar(View subView, @StringRes int messageResId) {
        Snackbar snackbar = Snackbar.make(subView.getRootView(), messageResId, Snackbar.LENGTH_LONG);
        try {
            Field mAccessibilityManagerField = BaseTransientBottomBar.class.getDeclaredField("mAccessibilityManager");
            mAccessibilityManagerField.setAccessible(true);
            AccessibilityManager accessibilityManager = (AccessibilityManager) mAccessibilityManagerField.get(snackbar);
            Field mIsEnabledField = AccessibilityManager.class.getDeclaredField("mIsEnabled");
            mIsEnabledField.setAccessible(true);
            mIsEnabledField.setBoolean(accessibilityManager, false);
            mAccessibilityManagerField.set(snackbar, accessibilityManager);
        } catch (Exception e) {
            Log.d(TAG, "showSnackbar:reflection_error:" + e.toString());
        }
    }

    @MainThread
    public static void showSnackbar(Activity activity, String message) {
        View rootView = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        try {
            Field mAccessibilityManagerField = BaseTransientBottomBar.class.getDeclaredField("mAccessibilityManager");
            mAccessibilityManagerField.setAccessible(true);
            AccessibilityManager accessibilityManager = (AccessibilityManager) mAccessibilityManagerField.get(snackbar);
            Field mIsEnabledField = AccessibilityManager.class.getDeclaredField("mIsEnabled");
            mIsEnabledField.setAccessible(true);
            mIsEnabledField.setBoolean(accessibilityManager, false);
            mAccessibilityManagerField.set(snackbar, accessibilityManager);
        } catch (Exception e) {
            Log.d(TAG, "showSnackbar:reflection_error:" + e.toString());
        }
        snackbar.show();
    }

    @MainThread
    public static void showSnackbar(Activity activity, @StringRes int messageResId) {
        View rootView = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, messageResId, Snackbar.LENGTH_LONG);
        try {
            Field mAccessibilityManagerField = BaseTransientBottomBar.class.getDeclaredField("mAccessibilityManager");
            mAccessibilityManagerField.setAccessible(true);
            AccessibilityManager accessibilityManager = (AccessibilityManager) mAccessibilityManagerField.get(snackbar);
            Field mIsEnabledField = AccessibilityManager.class.getDeclaredField("mIsEnabled");
            mIsEnabledField.setAccessible(true);
            mIsEnabledField.setBoolean(accessibilityManager, false);
            mAccessibilityManagerField.set(snackbar, accessibilityManager);
        } catch (Exception e) {
            Log.d(TAG, "showSnackbar:reflection_error:" + e.toString());
        }
        snackbar.show();
    }

    public static boolean isServiceRunning(Context context, String serviceName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo runningServiceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(runningServiceInfo.service.getClassName())) return true;
        }
        return false;
    }

    /**
     * 딜레이가 발생되는 작업을 할 경우에는 무조건 아래 메서드를 통해서 작업한다.
     */
    public static Task<Void> delayCall(final Callable callable, final long delayMillis) {
        return delayCall(callable, delayMillis, true);
    }

    public static Task<Void> delayCall(final Callable callable, final long delayMillis, boolean isUIThread) {
        final TaskCompletionSource tcs = new TaskCompletionSource();
        Looper looper = isUIThread ? Looper.getMainLooper() : Looper.myLooper();
        new Handler(looper).postDelayed(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                Tasks.call(callable).continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()) {
                            tcs.setException(task.getException());
                        }
                        tcs.setResult(task.getResult());
                        return null;
                    }
                });
            }
        }, delayMillis);
        return tcs.getTask();
    }

    public static boolean runDelayed(Runnable runnable, long delayMillis, boolean isUiThread) {
        Looper looper = isUiThread ? Looper.getMainLooper() : Looper.myLooper();
        return new Handler(looper).postDelayed(runnable, delayMillis);
    }

    public static boolean runOnUiThread(Runnable runnable) {
        return new Handler(Looper.getMainLooper()).post(runnable);
    }

    // Resources
    public static String getResourceName(View view) {
        String name;
        try {
            name = view.getResources().getResourceName(view.getId());
            if (name.contains("/")) name = name.substring(name.lastIndexOf("/") + 1, name.length());
        } catch (Resources.NotFoundException e) {
            return null;
        }
        return name;
    }

    public static String getResourceName(View view, boolean isDetail) {
        String name = view.getResources().getResourceName(view.getId());
        if (!isDetail) if (name.contains("/"))
            name = name.substring(name.lastIndexOf("/") + 1, name.length() - 1);
        return name;
    }

    /**
     * 해당 리소스ID의 컬러값을 정수로 반환한다.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#M}, the returned
     * color will be styled for the specified Context's theme.
     *
     * @param context    Base, 혹은 Application context.
     * @param colorResId The desired resource identifier, as generated by the aapt
     *                   tool. This integer encodes the package, type, and resource
     *                   entry. The value 0 is an invalid identifier.
     * @return A single color value in the form 0xAARRGGBB.
     * @throws android.content.res.Resources.NotFoundException if the given ID does not exist.
     */
    public static int getColor(@NonNull Context context, @ColorRes int colorResId) {
        Context c = context.getApplicationContext();
        return ContextCompat.getColor(c, colorResId);
    }

    /**
     * 해당 리소스ID의 Drawable을 반환한다.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#LOLLIPOP}, the
     * returned drawable will be styled for the specified Context's theme.
     *
     * @param context       Base, 혹은 Application context.
     * @param drawableResId The desired resource identifier, as generated by the aapt tool.
     *                      This integer encodes the package, type, and resource entry.
     *                      The value 0 is an invalid identifier.
     * @return Drawable An object that can be used to draw this resource.
     */
    public static Drawable getDrawable(@NonNull Context context, @DrawableRes int drawableResId) {
        Context c = context.getApplicationContext();
        return ContextCompat.getDrawable(c, drawableResId);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void systemBarLolipop(Activity act) {
        if (getAPIVerison() >= 5.0) {
            Window window = act.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(act.getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    /**
     * Making notification bar transparent
     */
    public static void changeStatusBarColor(Activity act) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = act.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    // Format

    /**
     * <p>Convert a <code>String</code> to an <code>int</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     * <p>
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * <p>
     * <pre>
     *   NumberUtils.toInt(null) = 0
     *   NumberUtils.toInt("")   = 0
     *   NumberUtils.toInt("1")  = 1
     * </pre>
     *
     * @param str the string to convert, may be null
     * @return the int represented by the string, or <code>zero</code> if
     * conversion fails
     * @since 2.1
     */
    public static int toInt(String str) {
        return toInt(str, 0);
    }

    /**
     * <p>Convert a <code>String</code> to an <code>int</code>, returning a
     * default value if the conversion fails.</p>
     * <p>
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * <p>
     * <pre>
     *   NumberUtils.toInt(null, 1) = 1
     *   NumberUtils.toInt("", 1)   = 1
     *   NumberUtils.toInt("1", 0)  = 1
     * </pre>
     *
     * @param str          the string to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the string, or the default if conversion fails
     * @since 2.1
     */
    public static int toInt(String str, int defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static long toLong(String str) {
        return toLong(str, 0);
    }

    public static long toLong(String str, int defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static String format(String format, Object... args) {
        return String.format(Locale.getDefault(), format, args);
    }

    public static String format(Context context, @StringRes int resId, Object... args) {
        Context c = context.getApplicationContext();
        String format = c.getString(resId);
        return String.format(Locale.getDefault(), format, args);
    }

    public static String numberFormat(@NonNull String format) {
        int num = !TextUtils.isEmpty(format) ? Integer.parseInt(format) : 0;
        return numberFormat(num);
    }

    public static String numberFormat(@NonNull int format) {
        return NumberFormat.getNumberInstance().format(format);
    }

    public static String numberFormat(@NonNull String format, @NonNull String unit) {
        return format("%s%s", numberFormat(format), unit);
    }

    public static String numberFormat(@NonNull int format, @NonNull String unit) {
        return format("%s%s", numberFormat(format), unit);
    }

    // 반드시 앞에 +가 붙어있어야 한다.
    public static String phoneFormat(@NonNull String phoneNumber) {
        String result = null;
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, null);
            // int countryCode = number.getCountryCode();
            // String regionCode = phoneNumberUtil.getRegionCodeForCountryCode(countryCode);
            String localNumber = phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            result = localNumber.replace(" ", "-");
        } catch (NumberParseException e) {
            // The phone number does not start with +.
            e.printStackTrace();
        }
        return result;
    }

    public static boolean isValidNumber(@NonNull String phoneNumber) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, null);
            int countryCode = number.getCountryCode();
            String regionCode = phoneNumberUtil.getRegionCodeForCountryCode(countryCode);
            return phoneNumberUtil.isValidNumberForRegion(number, regionCode);
        } catch (NumberParseException e) {
            // The phone number does not start with +.
            e.printStackTrace();
            return false;
        }
    }

    /*public static String numberFormat(@NonNull String format) {
        int num = !TextUtils.isEmpty(format) ? Integer.parseInt(format) : 0;
        return NumberFormat.getNumberInstance().format(num);
    }

    public static String numberFormat(@NonNull String format, @NonNull String unit) {
        int num = !TextUtils.isEmpty(format) ? Integer.parseInt(format) : 0;
        return format("%s%s", num, unit);
    }*/

    public static String ellipsize(String str, int maxSize) {
        if (TextUtils.isEmpty(str)) return "";
        int limit = maxSize - 3;
        return (str.length() > maxSize) ? str.substring(0, limit) + "..." : str;
    }

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;
    private static final int WEEK_MILLIS = 7 * DAY_MILLIS;

    public static String getTimeAgo(Context context, long time) {
        Context c = context.getApplicationContext();

        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        // Ref: https://www.epochconverter.com/
        long now = getTimeInMillis();
        Log.d(TAG, "getTimeAgo:time:" + time + "|now:" + now);
        if (time > now || time <= 0) {
            return c.getString(R.string.util_format_just_now);
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return c.getString(R.string.util_format_just_now);
            // return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return c.getString(R.string.util_format_a_minute_ago);
            //return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return c.getString(R.string.util_format_n_minutes_ago, diff / MINUTE_MILLIS);
            //return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return c.getString(R.string.util_format_a_hour_ago);
            // return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return c.getString(R.string.util_format_n_hours_ago, diff / HOUR_MILLIS);
            // return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return c.getString(R.string.util_format_yesterday);
            // return "yesterday";
        } else if (diff < 7 * DAY_MILLIS) {
            return c.getString(R.string.util_format_n_days_ago, diff / DAY_MILLIS);
            // return diff / DAY_MILLIS + " days ago";
        } else if (diff < 2 * WEEK_MILLIS) {
            return c.getString(R.string.util_format_a_week_ago);
            // return "a week ago";
        } else {
            return c.getString(R.string.util_format_n_weeks_ago, diff / WEEK_MILLIS);
            // return diff / WEEK_MILLIS + " weeks ago";
        }
    }

    /**
     * 모든 형태의 시간 변환을 아래 메서드로 통합하고자 한다.
     *
     * @param unixtime
     * @param format   e.g. dd-MM-yy hh:mmaa
     */
    public static String getFormattedTimeString(Long unixtime, String format) {
        // Log.i(TAG, "getFormattedTimeString:unixtime:" + unixtime);
        Date dateObj = new Date(unixtime);
        return getFormattedTimeString(dateObj, format);
    }

    public static String getFormattedTimeString(Date dateObj, String format) {
        // Log.i(TAG, "getFormattedTimeString:unixtime:" + unixtime);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(format, Locale.getDefault());
        return dateFormatter.format(dateObj);
    }

    public static String getPrettyCardNo(String uglyCardNo) {
        if (uglyCardNo == null || uglyCardNo.length() < 4) return null;
        String lastFourDigits = uglyCardNo.substring(uglyCardNo.length() - 4);
        String format = "∗∗∗∗ - ∗∗∗∗ - ∗∗∗∗ - %s";
        return format(format, lastFourDigits);
    }

    // Calculation
    public static int dpToPx(Context context, int dp) {
        Context c = context.getApplicationContext();
        return (int) (dp * c.getResources().getDisplayMetrics().density);
    }

    public static float dpToPx(Context context, float dp) {
        Context c = context.getApplicationContext();
        return (int) (dp * c.getResources().getDisplayMetrics().density);
    }

    public static int pxToDp(Context context, int px) {
        Context c = context.getApplicationContext();
        return (int) (px / c.getResources().getDisplayMetrics().density);
    }

    // 이미 TextView에 내용이 존재할 때 사용한다.
    public static void appendHighlightText(@NonNull TextView textView,
                                           @Nullable CharSequence highlightText,
                                           @NonNull ClickableSpan clickableSpan) {
        Spannable spannable = (Spannable) textView.getText();
        int highlightLength = highlightText.length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, highlightLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(clickableSpan, 0, highlightLength, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        if (textView.getMovementMethod() == null)
            textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static void appendHighlightText(@NonNull TextView textView,
                                           @Nullable CharSequence highlightText,
                                           @Nullable CharSequence body,
                                           @Nullable ClickableSpan clickableSpan) {
        int highlightLength;
        CharSequence content;
        if (highlightText != null) {
            highlightLength = highlightText.length();
            content = highlightText + "  " + body;
        } else {
            highlightLength = 0;
            content = body;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(content);
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, highlightLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(clickableSpan, 0, highlightLength, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        if (textView.getMovementMethod() == null)
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(builder);
    }

    public static CharSequence getBoldText(CharSequence text) {
        if (text == null) return new SpannableStringBuilder("");
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    // interface OnHighlightClickListener

    // Tasks
    public static Task<Void> sleep(final long millis) {
        return Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Thread.sleep(millis);
                return null;
            }
        });
    }

    // Hashtag helper
    public static HashTagHelper createDefaultHashTagHelper(
            @NonNull Context context,
            @Nullable HashTagHelper.OnHashTagClickListener listener) {
        Context c = context.getApplicationContext();
        char[] additionalSymbols = new char[]{'_'};
        return HashTagHelper.Creator.create(getColor(c, R.color.colorAccentLight), listener, additionalSymbols);
    }

    // AtSign helper
    public static AtSignHelper createDefaultAtSignHelper(
            @NonNull Context context,
            @Nullable AtSignHelper.OnAtSignClickListener listener) {
        Context c = context.getApplicationContext();
        char[] additionalSymbols = new char[]{'_'};
        return AtSignHelper.Creator.create(getColor(c, R.color.colorAccentLight), listener, additionalSymbols);
    }

    // System
    public static float getAPIVerison() {
        Float f = null;
        try {
            StringBuilder strBuild = new StringBuilder();
            strBuild.append(android.os.Build.VERSION.RELEASE.substring(0, 2));
            f = new Float(strBuild.toString());
        } catch (NumberFormatException e) {
            Log.e("", "erro ao recuperar a versão da API" + e.getMessage());
        }
        return f.floatValue();
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getDeviceUUID(Context context) {
        final SharedPreferencesCache cache = Session.getAppCache();
        String PROPERTY_DEVICE_ID = "property_device_id";
        final String id = cache.getString(PROPERTY_DEVICE_ID);
        UUID uuid;
        if (id != null) {
            uuid = UUID.fromString(id);
        } else {
            final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            try {
                if (!"9774d56d682e549c".equals(androidId)) {
                    uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
                } else {
                    final String deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                    uuid = deviceId != null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : UUID.randomUUID();
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            Bundle bundle = new Bundle();
            bundle.putString(PROPERTY_DEVICE_ID, uuid.toString());
            cache.save(bundle);
        }
        return uuid.toString();
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean isAppInForeground(Context context) {
        boolean result;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            ActivityManager.RunningTaskInfo foregroundTaskInfo = activityManager.getRunningTasks(1).get(0);
            String foregroundTaskPackageName = foregroundTaskInfo.topActivity.getPackageName();
            result = foregroundTaskPackageName.toLowerCase().equals(context.getPackageName().toLowerCase());
            Log.i(TAG, "isAppInForeground:RunningTaskInfo:" + result);
            return result;
        } else {
            ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
            ActivityManager.getMyMemoryState(appProcessInfo);
            if (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE) {
                Log.i(TAG, "isAppInForeground:RunningAppProcessInfo:true");
                return true;
            }
            // App is not foreground, but screen is locked, so show notification
            // KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            // result = km.inKeyguardRestrictedInputMode();
            result = false;
            Log.i(TAG, "isAppInForeground:KeyguardManager:" + result);
            return result;
        }
    }

    public static long getTimeInMillis() {
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Seoul");
        // TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
        Calendar cal = Calendar.getInstance(Locale.KOREA);
        // Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTimeZone(timeZone);
        return cal.getTimeInMillis();
    }

    // Source: https://stackoverflow.com/a/26152562/4729203
    @Deprecated
    public static boolean isKeyboardShown(View rootView) {

        /* 128dp = 32dp * 4, minimum button height 32dp and generic 4 rows soft keyboard */
        final int SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD = 128;

        Rect rect = new Rect();
        rootView.getWindowVisibleDisplayFrame(rect);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();

        /* heightDiff = rootView height - status bar height (r.top) - visible frame height (rect.bottom - rect.top) */
        int heightDiff = rootView.getBottom() - rect.bottom;

        /* Threshold size: dp to pixels, multiply with display density */
        boolean isKeyboardShown = heightDiff > SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD * dm.density;

        Log.d(TAG, "isKeyboardShown ? " + isKeyboardShown + ", heightDiff:" + heightDiff + ", density:" + dm.density
                + "root view height:" + rootView.getHeight() + ", rect:" + rect);

        return isKeyboardShown;
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static boolean checkPlayServices(Activity activity) {
        Context c = activity.getApplicationContext();
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(c);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                // apiAvailability.makeGooglePlayServicesAvailable(activity);
            } else {
                // This should never happen.
                Log.i(TAG, "This device is not supported.");
                // finish();
            }
            return false;
        }
        return true;
    }

    public static void showKeyboard(Activity activity) {
        /*((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY
        );*/
        Context c = activity.getApplicationContext();
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
            // imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            imm.showSoftInput(view, 0);
        }
    }

    public static void hideKeyboard(Activity activity) {
        Context c = activity.getApplicationContext();
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void clearFocus(EditText editText) {
        editText.setFocusableInTouchMode(false);
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(true);
        editText.setFocusable(true);
        editText.clearFocus();
    }

    public static int getScreenWidth(Context context) {
        Context c = context.getApplicationContext();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        Log.d(TAG, "getScreenWidth:metrics:" + metrics.toString());
        return metrics.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        Context c = context.getApplicationContext();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    // From "Tools"
    @SuppressWarnings("ConstantConditions")
    public static int getGridSpanCount(Context context) {
        Context c = context.getApplicationContext();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        float screenWidth = metrics.widthPixels;
        float cellWidth = context.getResources().getDimension(R.dimen.explorer_item_size);
        return Math.round(screenWidth / cellWidth);
    }

    // TODO: 실제 RecyclerView의 높이값으로 적용하는게 더 정확하다.
    @SuppressWarnings("ConstantConditions")
    public static int getGridRowCount(Context context) {
        Context c = context.getApplicationContext();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        float screenHeight = metrics.heightPixels;
        float cellHeight = context.getResources().getDimension(R.dimen.explorer_item_size);
        int count = ((Double) Math.ceil(screenHeight / cellHeight)).intValue();
        return count;
    }

    public static int getGridRowCount(Context context, int targetHeight) {
        // Log.i(TAG, "getGridRowCount:targetHeight:" + targetHeight);
        float cellHeight = context.getResources().getDimension(R.dimen.explorer_item_size);
        int count = ((Double) Math.ceil(targetHeight / cellHeight)).intValue();
        // Log.i(TAG, "getGridRowCount:cellHeight:" + cellHeight);
        // Log.i(TAG, "getGridRowCount:targetHeight/cellHeight:" + targetHeight / cellHeight);
        // Log.i(TAG, "getGridRowCount:count:" + count);
        return count;
    }

    public static int getGridItemCount(Context context, int targetHeight) {
        int spanCount = getGridSpanCount(context);
        int rowCount = getGridRowCount(context, targetHeight);
        int count = spanCount * rowCount;
        // Log.i(TAG, "getGridRowCount:count:" + count);
        return count;
    }

    /**
     * Check whether this exception contains an exception of the given type:
     * either it is of the given class itself or it contains a nested cause
     * of the given type.
     *
     * @param exType the exception type to look for
     * @return whether there is a nested exception of the specified type
     */
    public static boolean instanceOfException(Throwable cause, Class exType) {
        if (exType == null) {
            return false;
        }
        while (cause != null) {
            if (exType.isInstance(cause)) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
