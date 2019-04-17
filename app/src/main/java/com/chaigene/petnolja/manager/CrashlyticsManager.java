package com.chaigene.petnolja.manager;

public abstract class CrashlyticsManager {

    public static final String KEY_USER_TYPE = "user_type";

    /**
     * 유저 정보가 필요없는 제한된 상황에서만 사용하기를 권고한다.
     * Context가 존재하지 않는 Class에서 사용할 때의 메서드이다.
     *
     * @param priority
     * @param tag
     * @param msg
     *//*
    public static void log(int priority, String tag, String msg) {
        String log = String.format("%s/%s %s", logToAlphabet(priority), tag, msg);
        try {
            crash(log);
        } catch (CrashlyticsException e) {
            if (BuildConfig.DEBUG) Crashlytics.log(priority, tag, msg);
            else Crashlytics.log(log);

            Crashlytics.logException(e);
        }
    }

    *//**
     * 유저 정보가 필요없는 제한된 상황에서만 사용하기를 권고한다.
     * Context가 존재하지 않는 Class에서 사용할 때의 메서드이다.
     *
     * @param priority
     * @param tag
     * @param msg
     * @param e
     *//*
    public static void log(int priority, String tag, String msg, Exception e) {
        String log = String.format("%s/%s %s", logToAlphabet(priority), tag, msg);

        if (BuildConfig.DEBUG) Crashlytics.log(priority, tag, msg);
        else Crashlytics.log(log);

        Crashlytics.logException(e);
    }

    *//**
     * 이메일을 알기 위해서는 사용자가 권한을 허용해주어야 한다.
     * Crashlytics.setUserEmail(email);
     *
     * @param ctx
     * @param priority
     * @param tag
     * @param msg
     *//*
    *//*public static void log(Context ctx, int priority, String tag, String msg) {
        Context context = ctx.getApplicationContext();
        String log = String.format("%s/%s %s", logToAlphabet(priority), tag, msg);
        try {
            crash(log);
        } catch (CrashlyticsException e) {
            if (BuildConfig.DEBUG) Crashlytics.log(priority, tag, msg);
            else Crashlytics.log(log);

            if (DigitsManager.isSignedIn())
                Crashlytics.setUserIdentifier(String.valueOf(DigitsManager.getId()));
            if (UserManager.getInstance(context).isSignedIn()) {
                Crashlytics.setUserName(UserManager.getInstance(context).getUserId());
                Crashlytics.setInt(KEY_USER_TYPE, UserManager.getInstance(context).getUserType());
            }

            Crashlytics.logException(e);
        }
    }*//*

    *//**
     * 자체 Exception 값을 반환 받아서 업로드 할 수 있다.
     *
     * @param ctx
     * @param priority
     * @param tag
     * @param msg
     * @param e
     *//*
    *//*public static void log(Context ctx, int priority, String tag, String msg, Exception e) {
        Context context = ctx.getApplicationContext();
        String log = String.format("%s/%s %s", logToAlphabet(priority), tag, msg);

        if (BuildConfig.DEBUG) Crashlytics.log(priority, tag, msg);
        else Crashlytics.log(log);

        if (DigitsManager.isSignedIn())
            Crashlytics.setUserIdentifier(String.valueOf(DigitsManager.getId()));
        if (UserManager.getInstance(context).isSignedIn()) {
            Crashlytics.setUserName(UserManager.getInstance(context).getUserId());
            Crashlytics.setInt(KEY_USER_TYPE, UserManager.getInstance(context).getUserType());
        }

        Crashlytics.logException(e);
    }*//*

    *//**
     * @param detailMessage This Exception is non-fatal, and for logging only.
     * @throws CrashlyticsException
     *//*
    private static void crash(String detailMessage) throws CrashlyticsException {
        throw new CrashlyticsException(detailMessage);
    }

    private static String logToAlphabet(int priority) {
        switch (priority) {
            case Log.VERBOSE:
                return "V";
            case Log.DEBUG:
                return "D";
            case Log.INFO:
                return "I";
            case Log.WARN:
                return "W";
            case Log.ERROR:
                return "E";
            case Log.ASSERT:
                return "A";
            default:
                return "";
        }
    }

    public static class CrashlyticsException extends Exception {
        private CrashlyticsException() {
        }

        private CrashlyticsException(String detailMessage) {
            super(detailMessage);
        }
    }*/
}