package com.freeftr.batch.couponhistory.dto.event;

import com.freeftr.batch.couponhistory.domain.enums.HistoryType;

import java.time.LocalDateTime;

public record CouponHistoryEvent(
		Long couponMemberId,
		HistoryType type,
		LocalDateTime issuedDate
) {
}
