package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.domain.CouponMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponMemberJpaRepository extends JpaRepository<CouponMember, Long> {

    @Query("""
    SELECT COUNT(cm)
    FROM CouponMember cm
    WHERE cm.couponId = :couponId
    """)
    int countByCouponId(Long couponId);

	boolean existsByCouponIdAndMemberId(Long couponId, Long memberId);
}
