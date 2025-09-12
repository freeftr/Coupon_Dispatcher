package com.freeftr.coupon.coupon.dto.request;

import com.freeftr.coupon.coupon.domain.enums.CouponType;

import java.time.LocalDateTime;

public record CouponCreateRequest(
        CouponType type,
        Integer validityPeriod,
        Integer quantity
) {
}
