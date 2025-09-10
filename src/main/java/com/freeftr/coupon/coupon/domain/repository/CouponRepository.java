package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRepository {

    private final CouponJpaRepository couponJpaRepository;

    public Coupon saveCoupon(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }
}
