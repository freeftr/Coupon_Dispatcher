package com.freeftr.coupon.coupon.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueuePositionResponse(
        Long position,
        Long estimatedWaitSeconds,
        QueueResultResponse result
) {
    public QueuePositionResponse(Long position, Long estimatedWaitSeconds) {
        this(position, estimatedWaitSeconds, null);
    }
}
