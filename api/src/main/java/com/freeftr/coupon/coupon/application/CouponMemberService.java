package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.Coupon;
import com.freeftr.coupon.coupon.domain.CouponMember;
import com.freeftr.coupon.coupon.domain.enums.CouponIssueResult;
import com.freeftr.coupon.coupon.domain.repository.CouponMemberRepository;
import com.freeftr.coupon.coupon.domain.repository.CouponRepository;
import com.freeftr.coupon.coupon.dto.event.CouponHistoryEvent;
import com.freeftr.coupon.coupon.dto.response.CouponResponse;
import com.freeftr.coupon.couponhistory.domain.enums.HistoryType;
import com.freeftr.coupon.member.domain.Member;
import com.freeftr.coupon.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponMemberService {

    private final CouponMemberRepository couponMemberRepository;
    private final RedisService redisService;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void issueCoupon(Long couponId, Long memberId) {
        Member member = getMember(memberId);
        Coupon coupon = getCoupon(couponId);

        CouponIssueResult issueResult = redisService.issueCoupon(
                couponId,
                memberId,
                coupon.getQuantity()
        );

        validateIssueResult(issueResult);

        // DB 롤백 시 Redis 보상 트랜잭션 등록
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    redisService.rollbackIssueCoupon(couponId, memberId);
                    redisService.removeCouponFromCache(memberId, couponId);
                }
            }
        });

        CouponMember couponMember = CouponMember.builder()
                .couponId(couponId)
                .memberId(memberId)
                .expireDate(LocalDate.now().plusMonths(coupon.getValidityPeriod()))
                .build();

        Long couponMemberId = couponMemberRepository.save(couponMember).getId();

        redisService.addCouponToCache(
                new CouponResponse(
                        couponId,
                        coupon.getType(),
                        couponMember.getExpireDate()),
                memberId
        );

        applicationEventPublisher.publishEvent(
                new CouponHistoryEvent(
                        couponMemberId,
                        HistoryType.ISSUED,
                        LocalDateTime.now()
                )
        );
    }

    @Transactional
    public void useCoupon(Long couponMemberId, Long memberId) {
        CouponMember couponMember = getCouponMember(couponMemberId);

        validateAuthor(memberId, couponMember);

        couponMember.useCoupon(LocalDate.now());

        redisService.removeCouponFromCache(memberId, couponMember.getCouponId());

        applicationEventPublisher.publishEvent(
                new CouponHistoryEvent(
                        couponMemberId,
                        HistoryType.USED,
                        LocalDateTime.now()
                )
        );
        //TODO: 필요하다면 동시성 처리
    }

    public List<CouponResponse> findCouponsByMemberId(Long memberId) {
        getMember(memberId);

        List<CouponResponse> cache = redisService.findCouponCacheByMemberId(memberId);

        if (cache != null) {
            log.info("cache hit");
            return cache;
        }

        log.info("cache miss");

        String lockKey = "member:" + memberId + ":coupons";
        boolean locked = redisService.tryLock(lockKey);

        if (!locked) {
            // 다른 요청이 캐시를 채우는 중이므로 잠시 후 재조회
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cache = redisService.findCouponCacheByMemberId(memberId);
            if (cache != null) {
                return cache;
            }
        }

        try {
            List<CouponResponse> response = couponMemberRepository.findCouponsByMemberId(memberId);
            redisService.cacheCoupon(response, memberId);
            return response;
        } finally {
            if (locked) {
                redisService.unlock(lockKey);
            }
        }
    }

    private void validateIssueResult(CouponIssueResult result) {
        switch (result) {
            case SUCCESS:
                return;
            case SOLD_OUT:
                throw new BadRequestException(ErrorCode.COUPON_SOLD_OUT);
            case ALREADY_ISSUED:
                throw new BadRequestException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    private Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.COUPON_NOT_FOUND));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private CouponMember getCouponMember(Long couponMemberId) {
        return couponMemberRepository.findById(couponMemberId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.COUPON_MEMBER_NOT_FOUND));
    }

    private void validateAuthor(Long memberId, CouponMember couponMember) {
        if (!couponMember.isAuthor(memberId)) {
            throw new BadRequestException(ErrorCode.NOT_AN_COUPON_AUTHOR);
        }
    }
}
