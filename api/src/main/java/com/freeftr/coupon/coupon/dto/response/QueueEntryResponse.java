package com.freeftr.coupon.coupon.dto.response;

public record QueueEntryResponse(
        Long position,
        Long estimatedWaitSeconds
) {
}
