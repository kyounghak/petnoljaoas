package com.chaigene.petnolja.model;

public class ErrorCode {
    public static final int ORDER_INVALID_CARD_INFO = 10300;            // 잘못된 카드 정보 입니다.
    public static final int ORDER_INVALID_SECURITY_PIN = 10301;         // 잘못된 결제비밀번호 입니다. (실패횟수: 1/5회)
    public static final int ORDER_PIN_FAIL_QUOTA_EXCEEDED = 10302;      // 결제비밀번호를 5회 이상 실패하셨습니다. 재등록하여 사용해주세요.

    public static final int CARD_NOT_ALLOWED_CARD_COMPANY = 11000;      // 취급하지 않는 카드사입니다.
    public static final int CARD_DIFFRENT_USERNAME = 11001;             // 인증된 정보와 카드의 명의자가 다릅니다.
    public static final int CARD_INVALID_NO = 11002;                    // 유효하지 않은 카드번호를 입력하셨습니다.

    public static final int VIRAL_LOG_ALREADY_EXISTS = 12000;           // 로그 값이 이미 존재합니다.
}