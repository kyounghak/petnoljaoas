package com.chaigene.petnolja.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * PrivateInfo POJO.
 */
@IgnoreExtraProperties
public class PrivateInfo extends Request implements Serializable {

    public static final String SEX_MALE = "M";             // 남자
    public static final String SEX_FEMALE = "F";           // 여자

    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_BIRTHDAY = "birthday";
    public static final String FIELD_SEX = "sex";
    public static final String FIELD_ADDRESS = "address";

    // username:"홍길동"
    // email:"panda@gmail.com"
    // phone:"+821011020017"
    // address:"서울 영등포구 여의공원로 13 (07235)"
    // bankAccount:"숫자로만입력"
    // bankAccountHolder:"홍길동"
    // bankName:"국민은행"

    String username;
    String email;
    String phone;
    String birthday;
    String sex;
    String address;

    String bankAccountHolder;
    String bankName;
    String bankAccount;

    public PrivateInfo() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBankAccountHolder() {
        return bankAccountHolder;
    }

    public void setBankAccountHolder(String bankAccountHolder) {
        this.bankAccountHolder = bankAccountHolder;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("username", username);
        result.put("email", email);
        result.put("phone", phone);
        result.put("birthday", birthday);
        result.put("sex", sex);
        result.put("address", address);
        result.put("bankAccountHolder", bankAccountHolder);
        result.put("bankName", bankName);
        result.put("bankAccount", bankAccount);
        return result;
    }

    @Override
    public String toString() {
        return "PrivateInfo{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", birthday='" + birthday + '\'' +
                ", sex='" + sex + '\'' +
                ", address='" + address + '\'' +
                ", bankAccountHolder='" + bankAccountHolder + '\'' +
                ", bankName='" + bankName + '\'' +
                ", bankAccount='" + bankAccount + '\'' +
                '}';
    }
}