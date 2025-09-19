package com.freeftr.coupon.coupon.dto.response;

import com.freeftr.coupon.coupon.domain.enums.CouponType;

import java.time.LocalDate;

public record CouponResponse(
		Long couponId,
		CouponType couponType,
		LocalDate expireDate
) {
}
