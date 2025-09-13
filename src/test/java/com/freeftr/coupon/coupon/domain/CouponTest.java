package com.freeftr.coupon.coupon.domain;

import com.freeftr.coupon.coupon.domain.enums.CouponType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {

	@Test
	@DisplayName("쿠폰 유효기간을 수정할 수 있다")
	void can_update_coupon_period() {
		// given
		Coupon coupon = Coupon.builder()
				.type(CouponType.치킨)
				.validityPeriod(6)
				.quantity(100)
				.build();

		// when
		coupon.updatePeriod(12);

		// then
		assertThat(coupon.getValidityPeriod()).isEqualTo(12);
	}
}
