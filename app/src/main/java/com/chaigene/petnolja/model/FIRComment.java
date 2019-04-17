package com.chaigene.petnolja.model;

import androidx.annotation.Nullable;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class FIRComment extends FIRObject implements Serializable {
    private static final String TAG = "FIRComment";

    String uid;
    String nickname;
    String content;
    Map<String, Boolean> hashtags = new HashMap<>();
    Map<String, Boolean> mentions = new HashMap<>();
    Object timestamp;

    public FIRComment(String uid, String nickname, String content) {
        this.uid = uid;
        this.nickname = nickname;
        this.content = content;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public FIRComment(String key, String uid, String nickname, String content) {
        this.key = key;
        this.uid = uid;
        this.nickname = nickname;
        this.content = content;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public FIRComment() {
        timestamp = ServerValue.TIMESTAMP;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Boolean> getHashtags() {
        return hashtags;
    }

    public void setHashtags(Map<String, Boolean> hashtags) {
        this.hashtags = hashtags;
    }

    @Exclude
    public void setHashtags(@Nullable List<String> hashtags) {
        // Log.i(TAG, "setPhotos:photos:" + hashtags.toString());
        if (hashtags != null) {
            HashMap<String, Boolean> hashtagMap = new HashMap<>();
            for (String hashtag : hashtags) {
                hashtagMap.put(hashtag, true);
            }
            this.hashtags = hashtagMap;
        } else {
            this.hashtags = null;
        }
    }

    public Map<String, Boolean> getMentions() {
        return mentions;
    }

    public void setMentions(Map<String, Boolean> mentions) {
        this.mentions = mentions;
    }

    @Exclude
    public void setMentions(@Nullable List<String> mentions) {
        // Log.i(TAG, "setMentions:photos:" + mentions.toString());
        if (mentions != null) {
            HashMap<String, Boolean> mentionMap = new HashMap<>();
            for (String mention : mentions) {
                mentionMap.put(mention, true);
            }
            this.mentions = mentionMap;
        } else {
            this.mentions = null;
        }
    }

    public Object getTimestamp() {
        return timestamp;
    }

    @Exclude
    @Nullable
    public Long getTimestamp(boolean isLong) {
        if (timestamp instanceof Long) return (Long) timestamp;
        else return null;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("uid", uid);
        result.put("nickname", nickname);
        result.put("content", content);
        result.put("timestamp", timestamp);
        return result;
    }
}
