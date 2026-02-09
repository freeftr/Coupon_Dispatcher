package com.freeftr.coupon.coupon.dto.response;

public record QueuePositionResponse(
        Long position,
        Long estimatedWaitSeconds
) {
}
