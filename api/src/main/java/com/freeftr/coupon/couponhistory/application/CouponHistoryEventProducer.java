package com.freeftr.coupon.couponhistory.application;

import com.freeftr.coupon.coupon.dto.event.CouponHistoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponHistoryEventProducer {

	private final KafkaTemplate<String, CouponHistoryEvent> kafkaTemplate;

	@Value(value = "${apps.topics.coupon}")
	private String couponTopic;

	public void send(CouponHistoryEvent event) {
		String key = event.couponMemberId().toString();
		kafkaTemplate.send(couponTopic, key, event);
	}
}
