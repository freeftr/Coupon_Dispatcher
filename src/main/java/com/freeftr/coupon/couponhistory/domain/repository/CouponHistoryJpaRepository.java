package com.freeftr.coupon.couponhistory.domain.repository;

import com.freeftr.coupon.couponhistory.domain.CouponHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponHistoryJpaRepository extends JpaRepository<CouponHistory, Long> {
}
