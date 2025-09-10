package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
}
