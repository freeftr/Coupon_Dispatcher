package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.Coupon;
import com.freeftr.coupon.coupon.domain.CouponMember;
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
        // 멤버 존재 검증
        Member member = getMember(memberId);

        // 쿠폰 존재 검증
        Coupon coupon = getCoupon(couponId);

        /**
        Lua Script 통해 쿠폰 발급
        - 발급 한도 검증
        - 중복 발급 검증
        **/
        String result = redisService.issueCoupon(
                couponId,
                memberId,
                coupon.getQuantity()
        );

        validateIssueResult(result);

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
        Member member = getMember(memberId);

        List<CouponResponse> cache = redisService.findCouponCacheByMemberId(memberId);

        // Cache hit
        if (cache != null) {
            log.info("cache hit");
            return cache;
        }

        log.info("cache miss");

        // Cache Miss
        List<CouponResponse> response = couponMemberRepository.findCouponsByMemberId(memberId);
        redisService.cacheCoupon(response, memberId);
        return response;
    }

    private void validateIssueResult(String result) {
        switch (result) {
            case "0":
                return;
            case "1":
                throw new BadRequestException(ErrorCode.COUPON_SOLD_OUT);
            case "2":
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
