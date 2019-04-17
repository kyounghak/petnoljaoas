package com.chaigene.petnolja.model;

import java.util.Map;

@Deprecated
public class OldUser extends FIRUser {
    boolean isFollowing;

    @Deprecated
    boolean isFollower;

    public OldUser(FIRUser firUser, Boolean isFollowing) {
        this.key = firUser.getKey();
        this.type = firUser.getType();
        this.nickname = firUser.getNickname();
        this.email = firUser.getEmail();
        this.provider = firUser.getProvider();
        this.description = firUser.getDescription();
        this.tokens = firUser.getTokens();
        this.purchaseCount = firUser.getPurchaseCount();
        this.followingCount = firUser.getFollowingCount();
        this.followerCount = firUser.getFollowerCount();
        this.signature = firUser.signature;
        this.isFollowing = isFollowing;
    }

    public OldUser() {
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = super.toMap();
        result.put("key", key);
        result.put("isFollowing", isFollowing);
        return result;
    }
}