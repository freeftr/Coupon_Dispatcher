package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.Coupon;
import com.freeftr.coupon.coupon.domain.CouponMember;
import com.freeftr.coupon.coupon.domain.repository.CouponMemberRepository;
import com.freeftr.coupon.coupon.domain.repository.CouponRepository;
import com.freeftr.coupon.member.domain.Member;
import com.freeftr.coupon.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponMemberService {

    private final CouponMemberRepository couponMemberRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;

    public void allocateCoupon(Long couponId, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.MEMBER_NOT_FOUND));

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.COUPON_NOT_FOUND));

        checkIssued(couponId, memberId);

        //TODO: Redis 카운터 확인

        // 쿠폰 발급 한도 조회
        int issued = couponMemberRepository.count(couponId);

        if (!coupon.checkQuantity(issued)) {
            throw new BadRequestException(ErrorCode.COUPON_SOLD_OUT);
        }

        CouponMember couponMember = CouponMember.builder()
                .couponId(couponId)
                .memberId(memberId)
                .build();

        couponMemberRepository.save(couponMember);

        //TODO: 발급이력기록 비동기? 메시지 큐로 던져서 컨슘으로 영속화
    }

    private void checkIssued(Long couponId, Long memberId) {
        if (couponMemberRepository.existsByCouponIdAndMemberId(couponId, memberId)) {
            throw new BadRequestException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }
}
