package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.domain.CouponMember;
import com.freeftr.coupon.coupon.dto.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponMemberRepository {

    private final CouponMemberJpaRepository couponMemberJpaRepository;

    public CouponMember save(CouponMember couponMember) {
        return couponMemberJpaRepository.save(couponMember);
    }

    public Optional<CouponMember> findById(Long couponMemberId) {
        return couponMemberJpaRepository.findById(couponMemberId);
    }

    public List<CouponResponse> findCouponsByMemberId(Long memberId) {
        return couponMemberJpaRepository.findCouponsByMemberId(memberId);
    }
}
