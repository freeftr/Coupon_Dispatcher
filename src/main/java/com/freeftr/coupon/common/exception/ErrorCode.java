package com.freeftr.coupon.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    COUPON_NOT_FOUND(1000, "존재하지 않는 쿠폰입니다."),
    COUPON_ALREADY_USED(1001, "이미 사용한 쿠폰입니다."),
    COUPON_ALREADY_ISSUED(1002, "이미 발급된 쿠폰입니다."),
    COUPON_SOLD_OUT(1003, "이미 품절된 쿠폰입니다."),

    NOT_AN_ADMIN(2000, "관리자가 아닙니다."),

    MEMBER_NOT_FOUND(3000, "존재하지 않는 사용자입니다.")
    ;

    private final int code;
    private final String message;
}
