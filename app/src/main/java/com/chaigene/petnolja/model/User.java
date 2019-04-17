package com.chaigene.petnolja.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class User extends STRObject {
    public static final String FIELD_NICKNAME = "nickname";
    public static final String FIELD_EMAIL = "email";

    private int type;
    private String nickname;
    private String email;
    private String provider;
    private String description;
    private List<String> tokens;
    private String signature;
    private Date signUpDate;
    private boolean isDeleted;
    private Date deletedDate;

    public User() {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    // Ref: https://firebase.google.com/docs/firestore/reference/android/ServerTimestamp
    @ServerTimestamp
    public Date getSignUpDate() {
        return signUpDate;
    }

    public void setSignUpDate(Date signUpDate) {
        this.signUpDate = signUpDate;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    @ServerTimestamp
    public Date getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        this.deletedDate = deletedDate;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("nickname", nickname);
        result.put("email", email);
        result.put("provider", provider);
        result.put("description", description);
        // result.put("purchaseCount", purchaseCount);
        // result.put("followingCount", followingCount);
        // result.put("followerCount", followerCount);
        result.put("tokens", tokens);
        result.put("signature", signature);
        result.put("signUpDate", signUpDate);
        result.put("isDeleted", isDeleted);
        result.put("deletedDate", deletedDate);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "ide=" + id +
                ", type=" + type +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", provider='" + provider + '\'' +
                ", description='" + description + '\'' +
                /*", purchaseCount=" + purchaseCount +
                ", followingCount=" + followingCount +
                ", followerCount=" + followerCount +*/
                ", tokens=" + tokens +
                ", signature='" + signature + '\'' +
                ", signUpDate=" + signUpDate +
                ", isDeleted=" + isDeleted +
                ", deletedDate=" + deletedDate +
                '}';
    }
}