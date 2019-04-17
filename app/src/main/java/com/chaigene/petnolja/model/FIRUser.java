package com.chaigene.petnolja.model;

import androidx.annotation.Nullable;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class FIRUser extends FIRObject implements Serializable {

    public static final int TYPE_USER = 0;             // 유저
    public static final int TYPE_MAKER = 1;            // 메이커
    public static final int TYPE_MANAGER = 100;        // 관리자
    public static final int TYPE_ADMIN = 1000;         // 어드민

    int type;
    String nickname;
    String email;
    String provider;
    String description;
    int purchaseCount;
    int followingCount;
    int followerCount;
    Map<String, Boolean> tokens;
    String signature;

    boolean isDeleted;
    Object deletedTimestamp;

    // 폰인증을 한 여부
    @Deprecated
    boolean isPhoneVerified;

    public FIRUser() {
        // Default constructor required for calls to DataSnapshot.getValue(OldUser.class)
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    @Nullable
    public String getProvider() {
        return provider;
    }

    public void setProvider(@Nullable String provider) {
        this.provider = provider;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(int purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public int getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    @Nullable
    public Map<String, Boolean> getTokens() {
        return tokens;
    }

    public void setTokens(@Nullable Map<String, Boolean> tokens) {
        this.tokens = tokens;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Object getDeletedTimestamp() {
        return deletedTimestamp;
    }

    @Exclude
    public Long getDeletedTimestamp(Boolean isLong) {
        if (deletedTimestamp instanceof Long) return (Long) deletedTimestamp;
        else return null;
    }

    public void setDeletedTimestamp(Object deletedTimestamp) {
        this.deletedTimestamp = deletedTimestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("nickname", nickname);
        result.put("email", email);
        result.put("provider", provider);
        result.put("description", description);
        result.put("purchaseCount", purchaseCount);
        result.put("followingCount", followingCount);
        result.put("followerCount", followerCount);
        result.put("tokens", tokens);
        result.put("signature", signature);
        result.put("isDeleted", isDeleted);
        result.put("deletedTimestamp", deletedTimestamp);
        return result;
    }
}