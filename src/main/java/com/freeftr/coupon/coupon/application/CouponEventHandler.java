package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.coupon.dto.event.CouponHistoryEvent;
import com.freeftr.coupon.couponhistory.domain.CouponHistory;
import com.freeftr.coupon.couponhistory.domain.repository.CouponHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponEventHandler {

	private final CouponHistoryRepository couponHistoryRepository;

	@EventListener
	public void handleCouponHistory(CouponHistoryEvent event) {
		CouponHistory couponHistory = CouponHistory.builder()
				.type(event.type())
				.issuedDate(event.issuedDate())
				.couponMemberId(event.couponMemberId())
				.build();

		couponHistoryRepository.save(couponHistory);
	}
}
