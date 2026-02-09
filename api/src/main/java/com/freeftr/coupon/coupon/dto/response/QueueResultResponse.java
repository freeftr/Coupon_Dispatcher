package com.freeftr.coupon.coupon.dto.response;

import com.freeftr.coupon.coupon.domain.enums.QueueStatus;

public record QueueResultResponse(
        QueueStatus status,
        String message
) {
}
