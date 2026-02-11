package com.freeftr.coupon.coupon.presentation;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.application.CouponService;
import com.freeftr.coupon.coupon.application.QueueRedisService;
import com.freeftr.coupon.coupon.domain.enums.QueueStatus;
import com.freeftr.coupon.coupon.dto.response.QueueEntryResponse;
import com.freeftr.coupon.coupon.dto.response.QueuePositionResponse;
import com.freeftr.coupon.coupon.dto.response.QueueResultResponse;
import com.freeftr.coupon.member.application.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons/{couponId}/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueRedisService queueRedisService;
    private final CouponService couponService;
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<QueueEntryResponse> enterQueue(
            @PathVariable Long couponId,
            @RequestParam(name = "memberId") Long memberId
    ) {
        memberService.validateMemberExists(memberId);

        Long result = queueRedisService.enterQueue(couponId, memberId);

        if (result == -1L) {
            throw new BadRequestException(ErrorCode.QUEUE_NOT_ACTIVE);
        }
        if (result == -2L) {
            throw new BadRequestException(ErrorCode.QUEUE_ALREADY_ENTERED);
        }

        long estimatedWait = queueRedisService.estimateWaitSeconds(result);
        return ResponseEntity.ok(new QueueEntryResponse(result, estimatedWait));
    }

    @GetMapping("/position")
    public ResponseEntity<QueuePositionResponse> getPosition(
            @PathVariable Long couponId,
            @RequestParam(name = "memberId") Long memberId
    ) {
        Long position = queueRedisService.getPosition(couponId, memberId);

        if (position == null) {
            String resultValue = queueRedisService.getResult(couponId, memberId);
            if (resultValue != null) {
                String[] parts = resultValue.split(":", 2);
                QueueStatus status = QueueStatus.valueOf(parts[0]);
                String message = parts.length > 1 ? parts[1] : "";
                return ResponseEntity.ok(new QueuePositionResponse(
                        null, null, new QueueResultResponse(status, message)));
            }
            throw new BadRequestException(ErrorCode.QUEUE_NOT_FOUND);
        }

        long estimatedWait = queueRedisService.estimateWaitSeconds(position);
        return ResponseEntity.ok(new QueuePositionResponse(position, estimatedWait));
    }

    @GetMapping("/result")
    public ResponseEntity<QueueResultResponse> getResult(
            @PathVariable Long couponId,
            @RequestParam(name = "memberId") Long memberId
    ) {
        String result = queueRedisService.getResult(couponId, memberId);

        if (result == null) {
            throw new BadRequestException(ErrorCode.QUEUE_RESULT_NOT_FOUND);
        }

        String[] parts = result.split(":", 2);
        QueueStatus status = QueueStatus.valueOf(parts[0]);
        String message = parts.length > 1 ? parts[1] : "";

        return ResponseEntity.ok(new QueueResultResponse(status, message));
    }

    @PostMapping("/activate")
    public ResponseEntity<Void> activateQueue(
            @PathVariable Long couponId,
            @RequestParam(name = "memberId") Long memberId
    ) {
        memberService.validateAdmin(memberId);

        if (queueRedisService.isQueueActive(couponId)) {
            throw new BadRequestException(ErrorCode.QUEUE_ALREADY_ACTIVE);
        }

        queueRedisService.activateQueue(couponId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateQueue(
            @PathVariable Long couponId,
            @RequestParam(name = "memberId") Long memberId
    ) {
        memberService.validateAdmin(memberId);
        queueRedisService.deactivateQueue(couponId);
        return ResponseEntity.noContent().build();
    }
}
