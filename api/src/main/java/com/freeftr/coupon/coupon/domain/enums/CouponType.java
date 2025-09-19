package com.freeftr.coupon.coupon.domain.enums;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponType {
    치킨("치킨"),
    햄버거("햄버거"),
    피자("피자");

    private final String name;

    public static CouponType from(String value) {
        for (CouponType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        throw new BadRequestException(ErrorCode.INVALID_COUPON_TYPE);
    }
}
