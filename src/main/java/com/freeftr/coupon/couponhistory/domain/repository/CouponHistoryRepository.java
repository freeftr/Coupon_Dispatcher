package com.freeftr.coupon.couponhistory.domain.repository;

import com.freeftr.coupon.couponhistory.domain.CouponHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponHistoryRepository {

	private final CouponHistoryJpaRepository couponHistoryJpaRepository;

	public CouponHistory save(CouponHistory couponHistory) {
		return couponHistoryJpaRepository.save(couponHistory);
	}
}
