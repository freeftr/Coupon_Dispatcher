package com.freeftr.coupon.coupon.dto.request;

import java.time.LocalDateTime;

public record PeriodUpdateRequest(
        Long couponId,
        Integer newPeriod
) {
}
