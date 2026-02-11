package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.enums.QueueStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueProcessingServiceTest {

    @InjectMocks
    private QueueProcessingService queueProcessingService;

    @Mock
    private QueueRedisService queueRedisService;

    @Mock
    private CouponMemberService couponMemberService;

    @Test
    @DisplayName("대기열에서 pop한 멤버에게 쿠폰을 발급한다.")
    void process_queue_success() {
        Long couponId = 1L;

        List<ZSetOperations.TypedTuple<String>> members = List.of(
                new DefaultTypedTuple<>("100", 1.0),
                new DefaultTypedTuple<>("200", 2.0)
        );

        given(queueRedisService.popFromQueue(couponId, 0)).willReturn(members);

        queueProcessingService.processQueue(couponId);

        verify(couponMemberService).issueCoupon(couponId, 100L);
        verify(couponMemberService).issueCoupon(couponId, 200L);
        verify(queueRedisService).saveResult(eq(couponId), eq(100L), eq(QueueStatus.SUCCESS), anyString());
        verify(queueRedisService).saveResult(eq(couponId), eq(200L), eq(QueueStatus.SUCCESS), anyString());
    }

    @Test
    @DisplayName("품절 시 남은 대기열을 drain한다.")
    void process_queue_sold_out() {
        Long couponId = 1L;

        List<ZSetOperations.TypedTuple<String>> members = List.of(
                new DefaultTypedTuple<>("100", 1.0),
                new DefaultTypedTuple<>("200", 2.0)
        );

        given(queueRedisService.popFromQueue(couponId, 0)).willReturn(members);
        doNothing().when(couponMemberService).issueCoupon(couponId, 100L);
        doThrow(new BadRequestException(ErrorCode.COUPON_SOLD_OUT))
                .when(couponMemberService).issueCoupon(couponId, 200L);

        queueProcessingService.processQueue(couponId);

        verify(queueRedisService).saveResult(eq(couponId), eq(100L), eq(QueueStatus.SUCCESS), anyString());
        verify(queueRedisService).saveResult(eq(couponId), eq(200L), eq(QueueStatus.SOLD_OUT), anyString());
        verify(queueRedisService).drainQueueAsSoldOut(couponId);
    }

    @Test
    @DisplayName("발급 실패 시 FAILED 결과를 저장한다.")
    void process_queue_failed() {
        Long couponId = 1L;

        List<ZSetOperations.TypedTuple<String>> members = List.of(
                new DefaultTypedTuple<>("100", 1.0)
        );

        given(queueRedisService.popFromQueue(couponId, 0)).willReturn(members);
        doThrow(new BadRequestException(ErrorCode.COUPON_ALREADY_ISSUED))
                .when(couponMemberService).issueCoupon(couponId, 100L);

        queueProcessingService.processQueue(couponId);

        verify(queueRedisService).saveResult(eq(couponId), eq(100L), eq(QueueStatus.FAILED), anyString());
    }

    @Test
    @DisplayName("대기열이 비어있으면 아무것도 하지 않는다.")
    void process_empty_queue() {
        Long couponId = 1L;

        given(queueRedisService.popFromQueue(couponId, 0)).willReturn(Collections.emptyList());

        queueProcessingService.processQueue(couponId);

        verifyNoInteractions(couponMemberService);
    }

}
