package com.chaigene.petnolja.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;

import com.chaigene.petnolja.BuildConfig;

public class ConfigManager {
    private final static String PREF_KEY_SAVED_IDENTIFIER = "saved_identifier";
    private final static String PREF_KEY_EVENT_POPUP_SKIP_ID = "event_popup_skip_id";
    private static final String PREF_KEY_FAKE_PAYMENT_MODE = "fake_payment_mode";
    private static final String PREF_KEY_GUIDE_SHOWN_MENTION_COMMENT = "guide_shown_mention_comment";
    private final static String PREF_KEY_USER_ID = "user_id";
    private final static String PREF_KEY_USER_NICKNAME = "user_nickname";
    private final static String PREF_KEY_USER_EMAIL = "user_email";
    private SharedPreferences mPref;

    private String savedIdentifier;
    private final Object savedIdentifierChanged = new Object();

    private String eventPopupSkipId;
    private final Object eventPopupSkipIdChanged = new Object();

    private boolean fakePaymentMode;
    private final Object fakePaymentModeChanged = new Object();

    private boolean guideShownMentionComment;
    private final Object guideShownMentionCommentChanged = new Object();

    private String userId;
    private final Object userIdChanged = new Object();

    private String userNickname;
    private final Object userNicknameChanged = new Object();

    private String userEmail;
    private final Object userEmailChanged = new Object();

    private static ConfigManager instance;

    public static ConfigManager getInstance(Context context) {
        if (instance == null) instance = new ConfigManager(context.getApplicationContext());
        return instance;
    }

    public static synchronized void releaseInstance() {
        if (instance != null) instance = null;
    }

    protected ConfigManager(Context context) {
        this.mPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        this.savedIdentifier = mPref.getString(PREF_KEY_SAVED_IDENTIFIER, null);
        this.eventPopupSkipId = mPref.getString(PREF_KEY_EVENT_POPUP_SKIP_ID, null);
        this.fakePaymentMode = mPref.getBoolean(PREF_KEY_FAKE_PAYMENT_MODE, false);
        this.guideShownMentionComment = mPref.getBoolean(PREF_KEY_GUIDE_SHOWN_MENTION_COMMENT, false);
        this.userId = mPref.getString(PREF_KEY_USER_ID, null);
        this.userNickname = mPref.getString(PREF_KEY_USER_NICKNAME, null);
        this.userEmail = mPref.getString(PREF_KEY_USER_EMAIL, null);
    }

    @Nullable
    public String getSavedIdentifier() {
        String savedEmail;
        synchronized (savedIdentifierChanged) {
            savedEmail = this.savedIdentifier;
        }
        return savedEmail;
    }

    public void setSavedIdentifier(@Nullable String savedIdentifier) {
        synchronized (savedIdentifierChanged) {
            this.savedIdentifier = savedIdentifier;
            mPref.edit().putString(PREF_KEY_SAVED_IDENTIFIER, savedIdentifier).apply();
        }
    }

    @Nullable
    public String getEventPopupSkipId() {
        String eventPopupSkipId;
        synchronized (eventPopupSkipIdChanged) {
            eventPopupSkipId = this.eventPopupSkipId;
        }
        return eventPopupSkipId;
    }

    public void setEventPopupSkipId(@Nullable String eventPopupSkipId) {
        synchronized (eventPopupSkipIdChanged) {
            this.eventPopupSkipId = eventPopupSkipId;
            mPref.edit().putString(PREF_KEY_EVENT_POPUP_SKIP_ID, eventPopupSkipId).apply();
        }
    }

    public boolean isFakePaymentMode() {
        boolean fakePaymentMode;
        synchronized (fakePaymentModeChanged) {
            // 단순히 preference 값만 가지고 코드가 실행되게 하면 보안상 굉장히 위험해진다.
            fakePaymentMode = this.fakePaymentMode && BuildConfig.DEBUG;
        }
        return fakePaymentMode;
    }

    public void setFakePaymentMode(boolean fakePaymentMode) {
        synchronized (fakePaymentModeChanged) {
            this.fakePaymentMode = fakePaymentMode;
            mPref.edit().putBoolean(PREF_KEY_FAKE_PAYMENT_MODE, fakePaymentMode).apply();
        }
    }

    public boolean isGuideShownMentionComment() {
        boolean guideShownMentionComment;
        synchronized (guideShownMentionCommentChanged) {
            guideShownMentionComment = this.guideShownMentionComment;
        }
        return guideShownMentionComment;
    }

    public void setGuideShownMentionComment(boolean guideShown) {
        synchronized (guideShownMentionCommentChanged) {
            this.guideShownMentionComment = guideShown;
            mPref.edit().putBoolean(PREF_KEY_GUIDE_SHOWN_MENTION_COMMENT, guideShown).apply();
        }
    }

    @Nullable
    public String getUserid() {
        String uid;
        synchronized (userIdChanged) {
            uid = this.userId;
        }
        return uid;
    }

    public void setUserId(@Nullable String userId) {
        synchronized (userIdChanged) {
            this.userId = userId;
            mPref.edit().putString(PREF_KEY_USER_ID, userId).apply();
        }
    }

    @Nullable
    public String getNickname() {
        String nickname;
        synchronized (userNicknameChanged) {
            nickname = this.userNickname;
        }
        return nickname;
    }

    public void setNickname(@Nullable String nickname) {
        synchronized (userNicknameChanged) {
            this.userNickname = nickname;
            mPref.edit().putString(PREF_KEY_USER_NICKNAME, nickname).apply();
        }
    }

    @Nullable
    public String getEmail() {
        String email;
        synchronized (userEmailChanged) {
            email = this.userEmail;
        }
        return email;
    }

    public void setEmail(@Nullable String email) {
        synchronized (userEmailChanged) {
            this.userEmail = email;
            mPref.edit().putString(PREF_KEY_USER_EMAIL, email).apply();
        }
    }
}
