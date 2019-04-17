package com.chaigene.petnolja.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class FIRMessage extends FIRObject {
    @Exclude
    public static final int TYPE_MESSAGE = 0;
    @Exclude
    public static final int TYPE_IMAGE = 1;

    String uid;
    String message;
    int type;
    Object timestamp;

    public FIRMessage() {
    }

    public FIRMessage(String uid, String message, int type) {
        this.uid = uid;
        this.message = message;
        this.type = type;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    @Exclude
    public Long getTimestamp(Boolean isLong) {
        if (timestamp instanceof Long) return (Long) timestamp;
        else return null;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("message", message);
        result.put("type", type);
        result.put("timestamp", timestamp);

        return result;
    }

    @Exclude
    @Override
    public String toString() {
        return String.format("id:%s/message:%s/type:%s/timestamp:%s",
                getUid(),
                getMessage(),
                getType() == TYPE_MESSAGE ? "TYPE_MESSAGE" : "TYPE_IMAGE",
                getTimestamp() != null ? CommonUtil.getFormattedTimeString(getTimestamp(true), "dd-MM-yy hh:mm aa") : ""
        );
    }
}
