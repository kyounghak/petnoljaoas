package com.chaigene.petnolja.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * HashtagLog POJO.
 */
@IgnoreExtraProperties
public class HashtagLog extends STRObject implements Serializable {

    String hashtag;
    String userId;
    String ip;
    String uuid;
    int referrer;
    Date timestamp;

    public HashtagLog() {
    }

    public String getHashtag() {
        return hashtag;
    }

    public void setHashtag(String hashtag) {
        this.hashtag = hashtag;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getReferrer() {
        return referrer;
    }

    public void setReferrer(int referrer) {
        this.referrer = referrer;
    }

    // Ref: https://firebase.google.com/docs/firestore/reference/android/ServerTimestamp
    @ServerTimestamp
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("hashtag", hashtag);
        result.put("userId", userId);
        result.put("ip", ip);
        result.put("uuid", uuid);
        result.put("referrer", referrer);
        result.put("timestamp", timestamp);
        return result;
    }
}