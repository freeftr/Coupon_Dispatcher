package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.Coupon;
import com.freeftr.coupon.coupon.domain.CouponMember;
import com.freeftr.coupon.coupon.domain.repository.CouponMemberRepository;
import com.freeftr.coupon.coupon.domain.repository.CouponRepository;
import com.freeftr.coupon.couponhistory.domain.repository.CouponHistoryRepository;
import com.freeftr.coupon.member.domain.Member;
import com.freeftr.coupon.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponMemberService {

    private final CouponMemberRepository couponMemberRepository;
    private final RedisService redisService;
    private final CouponHistoryRepository couponHistoryRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;

    public void allocateCoupon(Long couponId, Long memberId) {
        // 멤버 존재 검증
        Member member = getMember(memberId);

        // 쿠폰 존재 검증
        Coupon coupon = getCoupon(couponId);

        /*
        Lua Script 통해 쿠폰 발급
        - 발급 한도 검증
        - 중복 발급 검증
        */
        int result = redisService.issueCoupon(couponId, memberId, coupon.getQuantity());

        validateIssueResult(result);

        CouponMember couponMember = CouponMember.builder()
                .couponId(couponId)
                .memberId(memberId)
                .build();

        couponMemberRepository.save(couponMember);

        //TODO: 발급이력기록 비동기? 메시지 큐로 던져서 컨슈머에서 영속화
    }

    @Transactional
    public void useCoupon(Long couponMemberId, Long memberId) {
        CouponMember couponMember = getCouponMember(couponMemberId);

        validateAuthor(memberId, couponMember);

        couponMember.useCoupon();
        //TODO: 필요하다면 동시성 처리
    }

    private void validateIssueResult(int result) {
        switch (result) {
            case 0:
                return;
            case 1:
                throw new BadRequestException(ErrorCode.COUPON_SOLD_OUT);
            case 2:
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

    private static void validateAuthor(Long memberId, CouponMember couponMember) {
        if (!couponMember.isAuthor(memberId)) {
            throw new BadRequestException(ErrorCode.NOT_AN_COUPON_AUTHOR);
        }
    }
}
