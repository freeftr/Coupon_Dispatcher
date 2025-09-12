package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.domain.CouponMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponMemberJpaRepository extends JpaRepository<CouponMember, Long> {
}
