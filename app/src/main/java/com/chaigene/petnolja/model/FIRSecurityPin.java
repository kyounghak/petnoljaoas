package com.chaigene.petnolja.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
@SuppressWarnings("WeakerAccess")
public class FIRSecurityPin extends FIRObject {


    // 입력받은 6자리숫자. *DB에 저장해두면 안됨(암호화 뒤 삭제 필수)
    String source;
    // 암호화된 6자리숫자.
    String encrypted;
    // 비번틀린횟수. (일치할시 0으로 초기화)
    int failCount;
    // (옵션) 이 값이 존재하면 비번 변경 목적. *DB에 저장해두면 안됨(검증 뒤 삭제 필수)
    @Deprecated
    String previous;
    // 등록한 날짜.
    Object timestamp;

    public FIRSecurityPin() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
        this.timestamp = ServerValue.TIMESTAMP;
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

    @Deprecated
    public String getPrevious() {
        return previous;
    }

    @Deprecated
    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    @Exclude
    public Long getTimestamp(boolean isLong) {
        if (timestamp instanceof Long) return (Long) timestamp;
        else return null;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("source", source);
        result.put("encrypted", encrypted);
        result.put("failCount", failCount);
        result.put("previous", previous);
        result.put("timestamp", timestamp);
        return result;
    }
}