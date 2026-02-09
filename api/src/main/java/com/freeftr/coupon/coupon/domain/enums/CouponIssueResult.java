package com.freeftr.coupon.coupon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponIssueResult {
    SUCCESS(0L),
    SOLD_OUT(1L),
    ALREADY_ISSUED(2L);

    private final Long code;

    public static CouponIssueResult from(Long code) {
        for (CouponIssueResult result : values()) {
            if (result.code.equals(code)) {
                return result;
            }
        }
        throw new IllegalArgumentException("Unknown coupon issue result code: " + code);
    }
}
