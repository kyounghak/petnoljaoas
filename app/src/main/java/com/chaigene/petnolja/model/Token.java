package com.chaigene.petnolja.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Token POJO.
 */
@IgnoreExtraProperties
public class Token extends STRObject implements Serializable {

    @Deprecated
    public static final String FIELD_USER_ID = "userId";

    public static final String FIELD_TOKEN = "token";
    public static final String FIELD_VERSION_CODE = "versionCode";

    @Exclude
    @Deprecated
    private String userId;

    public static final String FIELD_VERSION_NAME = "versionName";

    private String token;
    private int versionCode;
    private String versionName;

    public Token() {
    }

    public Token(String token, int versionCode, String versionName) {
        this.token = token;
        this.versionCode = versionCode;
        this.versionName = versionName;
    }

    @Deprecated
    public Token(String userId, String token, int versionCode, String versionName) {
        this.userId = userId;
        this.token = token;
        this.versionCode = versionCode;
        this.versionName = versionName;
    }

    @Exclude
    @Deprecated
    public String getUserId() {
        return userId;
    }

    @Exclude
    @Deprecated
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("userId", userId);
        result.put("token", token);
        result.put("versionCode", versionCode);
        result.put("versionName", versionName);
        return result;
    }
}