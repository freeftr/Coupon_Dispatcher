package com.freeftr.batch.couponhistory.application;

import com.freeftr.batch.couponhistory.domain.repository.CouponHistoryJdbcRepository;
import com.freeftr.batch.couponhistory.dto.event.CouponHistoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponHistoryEventConsumer {

	private final CouponHistoryJdbcRepository couponHistoryJdbcRepository;

	@KafkaListener(
			topics = "${app.topics.coupon}",
			containerFactory = "couponBatchListenerFactory"
	)
	public void consume(List<CouponHistoryEvent> histories) {
		couponHistoryJdbcRepository.bulkInsert(histories);
	}
}
