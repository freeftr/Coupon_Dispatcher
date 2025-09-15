package com.freeftr.coupon.couponhistory.application;

import com.freeftr.coupon.coupon.dto.event.CouponHistoryEvent;
import com.freeftr.coupon.couponhistory.domain.CouponHistory;
import com.freeftr.coupon.couponhistory.domain.repository.CouponHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CouponHistoryEventHandler {

	private final CouponHistoryEventProducer couponHistoryEventProducer;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleCouponHistory(CouponHistoryEvent event) {
		couponHistoryEventProducer.send(event);
	}
}
