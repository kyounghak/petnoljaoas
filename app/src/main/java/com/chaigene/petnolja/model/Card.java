package com.chaigene.petnolja.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Card POJO.
 */
@IgnoreExtraProperties
public class Card extends Request implements Serializable {

    public static final int STATUS_REG_SUCCESS = 0;             // 등록 성공
    public static final int STATUS_REG_FAIL = -1;               // 등록 실패

    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_IS_PRIMARY = "isPrimary";
    public static final String FIELD_TIMESTAMP = "timestamp";

    // name:"BC카드"
    // sourceNo:"1234-1234-1234-1234"
    // displayNo:"****-****-****-1234"
    // expDate:"2019-01"
    // pin:"35"
    // birthday:"19901104"

    int status;
    // 빌링키 UID
    String billingKeyNo;
    // 유저 UID
    String userId;
    // 카드 이름(아임포트에서 반환받은 값을 저장함)
    String name;
    // 입력받은 카드 번호 *DB에 저장해두면 안됨(아임포트에 등록 후 삭제 필수)
    String sourceNo;
    // DB에 저장할 카드 번호. 뒷자리 4자리만 보여진다.
    String displayNo;
    // MM, YY 로 입력받아서 YYYY-MM 으로 변환해서 넣어준다.
    String expDate;
    // 카드 비밀번호 앞 2자리(법인카드의 경우 생략 가능) *DB에 저장해두면 안됨(아임포트에 등록 후 삭제 필수)
    String pin;
    // 카드 명의자의 생년월일 6자리(법인카드의 경우 사업자등록번호 10자리)
    String birthday;
    // 주결제카드 여부.
    boolean isPrimary;
    // 등록한 날짜.
    Date timestamp;

    public Card() {
    }

    public Card(String billingKeyNo, String userId, String sourceNo, String expDate, String pin, String birthday, boolean isPrimary) {
        this.billingKeyNo = billingKeyNo;
        this.userId = userId;
        this.sourceNo = sourceNo;
        this.expDate = expDate;
        this.pin = pin;
        this.birthday = birthday;
        this.isPrimary = isPrimary;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBillingKeyNo() {
        return billingKeyNo;
    }

    public void setBillingKeyNo(String billingKeyNo) {
        this.billingKeyNo = billingKeyNo;
    }

    public String getSourceNo() {
        return sourceNo;
    }

    public void setSourceNo(String sourceNo) {
        this.sourceNo = sourceNo;
    }

    public String getDisplayNo() {
        return displayNo;
    }

    public void setDisplayNo(String displayNo) {
        this.displayNo = displayNo;
    }

    public String getExpDate() {
        return expDate;
    }

    public void setExpDate(String expDate) {
        this.expDate = expDate;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public boolean getIsPrimary() {
        return this.isPrimary;
    }

    public void setIsPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
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
        result.put("name", name);
        result.put("sourceNo", sourceNo);
        result.put("displayNo", displayNo);
        result.put("expDate", expDate);
        result.put("pin", pin);
        result.put("birthday", birthday);
        result.put("getIsPrimary", isPrimary);
        result.put("timestamp", timestamp);
        return result;
    }
}