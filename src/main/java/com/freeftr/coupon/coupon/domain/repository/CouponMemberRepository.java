package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.domain.CouponMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponMemberRepository {

    private final CouponMemberJpaRepository couponMemberJpaRepository;

    public void save(CouponMember couponMember) {
        couponMemberJpaRepository.save(couponMember);
    }

    public Optional<CouponMember> findByCouponIdAndMemberId(Long couponId, Long memberId) {
        return couponMemberJpaRepository.findByCouponIdAndMemberId(couponId, memberId);
    }

    public Optional<CouponMember> findById(Long couponMemberId) {
        return couponMemberJpaRepository.findById(couponMemberId);
    }
}
