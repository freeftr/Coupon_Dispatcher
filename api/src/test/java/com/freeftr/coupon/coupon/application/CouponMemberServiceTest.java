package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.Coupon;
import com.freeftr.coupon.coupon.domain.CouponMember;
import com.freeftr.coupon.coupon.domain.enums.CouponType;
import com.freeftr.coupon.coupon.domain.repository.CouponMemberRepository;
import com.freeftr.coupon.coupon.domain.repository.CouponRepository;
import com.freeftr.coupon.member.domain.Member;
import com.freeftr.coupon.member.domain.enums.MemberGrade;
import com.freeftr.coupon.member.domain.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponMemberServiceTest {

	@InjectMocks
	private CouponMemberService couponMemberService;

	@Mock
	private CouponMemberRepository couponMemberRepository;

	@Mock
	private RedisService redisService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private CouponRepository couponRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private Member member() {
		return Member.builder()
				.name("박종하")
				.grade(MemberGrade.MEMBER)
				.build();
	}

	private Coupon coupon() {
		return Coupon.builder()
				.quantity(5)
				.validityPeriod(10)
				.type(CouponType.치킨)
				.build();
	}

	@Test
	@DisplayName("쿠폰이 이미 품절이면 예외를 던진다.")
	void issue_coupon_sold_out() {
		Long couponId = 1L;
		Long memberId = 100L;

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member()));
		given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon()));
		given(redisService.issueCoupon(couponId, memberId, 5)).willReturn("1");

		assertThatThrownBy(() -> couponMemberService.issueCoupon(couponId, memberId))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining(ErrorCode.COUPON_SOLD_OUT.getMessage());
	}

	@Test
	@DisplayName("쿠폰이 이미 발급된 회원이면 예외를 던진다.")
	void issue_coupon_already_issued() {
		Long couponId = 1L;
		Long memberId = 100L;

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member()));
		given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon()));
		given(redisService.issueCoupon(couponId, memberId, 5)).willReturn("2");

		assertThatThrownBy(() -> couponMemberService.issueCoupon(couponId, memberId))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining(ErrorCode.COUPON_ALREADY_ISSUED.getMessage());
	}

	@Test
	@DisplayName("쿠폰 사용 시 본인이 아니면 예외가 발생한다.")
	void use_coupon_not_author() {
		Long couponMemberId = 1L;
		Long otherMemberId = 200L;

		CouponMember couponMember = CouponMember.builder()
				.couponId(1L)
				.memberId(100L)
				.build();

		given(couponMemberRepository.findById(couponMemberId)).willReturn(Optional.of(couponMember));

		assertThatThrownBy(() -> couponMemberService.useCoupon(couponMemberId, otherMemberId))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining(ErrorCode.NOT_AN_COUPON_AUTHOR.getMessage());
	}
}
