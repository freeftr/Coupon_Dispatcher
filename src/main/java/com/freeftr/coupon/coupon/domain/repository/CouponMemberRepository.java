package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.domain.CouponMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponMemberRepository {

    private final CouponMemberJpaRepository couponMemberJpaRepository;

    public void save(CouponMember couponMember) {
        couponMemberJpaRepository.save(couponMember);
    }

    public int count(Long couponId) {
        return couponMemberJpaRepository.countByCouponId(couponId);
    }

    public boolean existsByCouponIdAndMemberId(Long couponId, Long memberId) {
        return couponMemberJpaRepository.existsByCouponIdAndMemberId(couponId, memberId);
    }
}
