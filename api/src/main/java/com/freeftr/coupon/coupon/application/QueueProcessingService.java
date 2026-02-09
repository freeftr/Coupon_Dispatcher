package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.enums.QueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueProcessingService {

    private final QueueRedisService queueRedisService;
    private final CouponMemberService couponMemberService;

    @Value("${app.queue.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.queue.process-interval-ms}")
    public void processQueues() {
        Set<String> activeCouponIds = queueRedisService.getActiveCouponIds();

        for (String couponIdStr : activeCouponIds) {
            Long couponId = Long.parseLong(couponIdStr);
            processQueue(couponId);
        }
    }

    public void processQueue(Long couponId) {
        List<ZSetOperations.TypedTuple<String>> members = queueRedisService.popFromQueue(couponId, batchSize);

        if (members.isEmpty()) return;

        for (ZSetOperations.TypedTuple<String> entry : members) {
            Long memberId = Long.parseLong(entry.getValue());

            try {
                couponMemberService.issueCoupon(couponId, memberId);
                queueRedisService.saveResult(couponId, memberId, QueueStatus.SUCCESS, "쿠폰이 발급되었습니다.");
                log.info("Queue: coupon {} issued to member {}", couponId, memberId);
            } catch (BadRequestException e) {
                if (e.getCode() == ErrorCode.COUPON_SOLD_OUT.getCode()) {
                    queueRedisService.saveResult(couponId, memberId, QueueStatus.SOLD_OUT, "쿠폰이 품절되었습니다.");
                    queueRedisService.drainQueueAsSoldOut(couponId);
                    log.info("Queue: coupon {} sold out, draining remaining queue", couponId);
                    return;
                }
                queueRedisService.saveResult(couponId, memberId, QueueStatus.FAILED, e.getMessage());
                log.warn("Queue: failed to issue coupon {} to member {}: {}", couponId, memberId, e.getMessage());
            }
        }
    }
}
