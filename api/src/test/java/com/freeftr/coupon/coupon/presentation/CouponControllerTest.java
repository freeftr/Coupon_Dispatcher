package com.freeftr.coupon.coupon.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freeftr.coupon.coupon.application.CouponMemberService;
import com.freeftr.coupon.coupon.application.CouponService;
import com.freeftr.coupon.coupon.domain.enums.CouponType;
import com.freeftr.coupon.coupon.dto.request.CouponCreateRequest;
import com.freeftr.coupon.coupon.dto.request.PeriodUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CouponService couponService;

	@MockitoBean
	private CouponMemberService couponMemberService;

	@Test
	@DisplayName("쿠폰을 생성한다.")
	void create_coupon() throws Exception {
		// given
		CouponCreateRequest request = new CouponCreateRequest(
				CouponType.치킨,
				10,
				6
		);
		given(couponService.createCoupon(any(CouponCreateRequest.class), anyLong()))
				.willReturn(1L);

		// when & then
		mockMvc.perform(post("/api/v1/coupons")
						.param("memberId", "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("쿠폰 유효기간을 수정한다")
	void update_coupon_period() throws Exception {
		// given
		PeriodUpdateRequest request = new PeriodUpdateRequest(1L, 12);
		doNothing().when(couponService).updatePeriod(any(PeriodUpdateRequest.class), anyLong());

		// when & then
		mockMvc.perform(patch("/api/v1/coupons")
						.param("memberId", "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("쿠폰을 발급받는다")
	void issue_coupon() throws Exception {
		// given
		doNothing().when(couponMemberService).issueCoupon(anyLong(), anyLong());

		// when & then
		mockMvc.perform(post("/api/v1/coupons/{couponId}", 1L)
						.param("memberId", "1"))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("발급받은 쿠폰을 사용한다.")
	void updateCouponMember_returnsNoContent() throws Exception {
		// given
		doNothing().when(couponMemberService).useCoupon(anyLong(), anyLong());

		// when & then
		mockMvc.perform(patch("/api/v1/coupons/issued/{couponMemberId}", 1L)
						.param("memberId", "1"))
				.andExpect(status().isNoContent());
	}
}
