package com.freeftr.coupon.coupon.dto.event;

import com.freeftr.coupon.couponhistory.domain.enums.HistoryType;

import java.time.LocalDateTime;

public record CouponHistoryEvent(
		Long couponMemberId,
		HistoryType type,
		LocalDateTime issuedDate
) {
}
