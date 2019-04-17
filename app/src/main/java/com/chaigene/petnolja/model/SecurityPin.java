package com.chaigene.petnolja.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * SecurityPin POJO.
 */
@IgnoreExtraProperties
public class SecurityPin extends STRObject implements Serializable {

    public static final String FIELD_USER_ID = "userId";

    // 입력받은 6자리숫자. *DB에 저장해두면 안됨(암호화 뒤 삭제 필수)
    String source;
    // 암호화된 6자리숫자.
    String encrypted;
    // 비번틀린횟수. (일치할시 0으로 초기화)
    int failCount;
    // 등록한 날짜.
    Date timestamp;

    public SecurityPin() {
    }

    public SecurityPin(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(String encrypted) {
        this.encrypted = encrypted;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
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
        result.put("source", source);
        result.put("encrypted", encrypted);
        result.put("failCount", failCount);
        result.put("timestamp", timestamp);
        return result;
    }
}