package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.Coupon;
import com.freeftr.coupon.coupon.domain.enums.CouponType;
import com.freeftr.coupon.coupon.domain.repository.CouponRepository;
import com.freeftr.coupon.coupon.dto.request.CouponCreateRequest;
import com.freeftr.coupon.coupon.dto.request.PeriodUpdateRequest;
import com.freeftr.coupon.member.domain.Member;
import com.freeftr.coupon.member.domain.enums.MemberGrade;
import com.freeftr.coupon.member.domain.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

	@InjectMocks
	private CouponService couponService;

	@Mock
	private CouponRepository couponRepository;

	@Mock
	private MemberRepository memberRepository;

	private Member admin() {
		return Member.builder()
				.name("관리자")
				.grade(MemberGrade.ADMIN)
				.build();
	}

	private Member member() {
		return Member.builder()
				.name("일반회원")
				.grade(MemberGrade.MEMBER)
				.build();
	}

	@Test
	@DisplayName("관리자는 쿠폰을 생성할 수 있다.")
	void admin_can_create_coupon() {
		// given
		Long memberId = 1L;
		CouponCreateRequest req = new CouponCreateRequest(
				CouponType.치킨,
				6,
				100
		);

		given(memberRepository.findById(memberId)).willReturn(Optional.of(admin()));
		given(couponRepository.saveCoupon(any(Coupon.class)))
				.willAnswer(inv -> {
					Coupon coupon = inv.getArgument(0);
					coupon = Coupon.builder()
							.type(coupon.getType())
							.validityPeriod(coupon.getValidityPeriod())
							.quantity(coupon.getQuantity())
							.build();
					return coupon;
				});

		// when
		couponService.createCoupon(req, memberId);

		// then
		verify(couponRepository).saveCoupon(any(Coupon.class));
	}

	@Test
	@DisplayName("관리자가 아니면 쿠폰을 생성할 수 없다.")
	void member_can_create_coupon() {
		// given
		Long memberId = 1L;
		CouponCreateRequest req = new CouponCreateRequest(
				CouponType.치킨,
				6,
				100
		);

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member()));

		// expect
		assertThatThrownBy(() -> couponService.createCoupon(req, memberId))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining(ErrorCode.NOT_AN_ADMIN.getMessage());
	}

	@Test
	@DisplayName("관리자는 쿠폰 유효기간을 수정할 수 있다")
	void admin_can_update_period() {
		// given
		Long memberId = 1L;
		Long couponId = 100L;
		PeriodUpdateRequest req = new PeriodUpdateRequest(couponId, 12);

		Coupon coupon = Coupon.builder()
				.type(CouponType.치킨)
				.validityPeriod(6)
				.quantity(10)
				.build();

		given(memberRepository.findById(memberId)).willReturn(Optional.of(admin()));
		given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

		// when
		couponService.updatePeriod(req, memberId);

		// then
		assertThat(coupon.getValidityPeriod()).isEqualTo(12);
	}

	@Test
	@DisplayName("관리자가 아니면 쿠폰 유효기간을 수정할 수 없다.")
	void member_can_not_update_period() {
		// given
		Long memberId = 1L;
		Long couponId = 100L;
		PeriodUpdateRequest request = new PeriodUpdateRequest(couponId, 12);

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member()));

		// when & then
		assertThatThrownBy(() -> couponService.updatePeriod(request, memberId))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining(ErrorCode.NOT_AN_ADMIN.getMessage());
	}

	@Test
	@DisplayName("존재하지 않는 쿠폰의 유효 기간을 수정할 수 없다.")
	void can_not_update_period_when_not_exist() {
		// given
		Long memberId = 1L;
		Long couponId = 100L;
		PeriodUpdateRequest req = new PeriodUpdateRequest(couponId, 12);

		given(memberRepository.findById(memberId)).willReturn(Optional.of(admin()));

		// when & then
		assertThatThrownBy(() -> couponService.updatePeriod(req, memberId))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining(ErrorCode.COUPON_NOT_FOUND.getMessage());
	}
}
