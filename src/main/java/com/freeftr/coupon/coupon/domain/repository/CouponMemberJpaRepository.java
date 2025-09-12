package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.domain.CouponMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponMemberJpaRepository extends JpaRepository<CouponMember, Long> {

    @Query("""
    SELECT COUNT(c)
    FROM Coupon c
    WHERE c.couponId = :couponId
    """)
    int countByCouponId(@Param("couponId") Long couponId);
}
