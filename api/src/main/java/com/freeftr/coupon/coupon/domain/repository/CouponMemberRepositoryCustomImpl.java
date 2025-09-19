package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.dto.response.CouponResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.freeftr.coupon.coupon.domain.QCoupon.coupon;
import static com.freeftr.coupon.coupon.domain.QCouponMember.couponMember;

@Repository
@RequiredArgsConstructor
public class CouponMemberRepositoryCustomImpl implements CouponMemberRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<CouponResponse> findCouponsByMemberId(Long memberId) {
		return queryFactory.select(
				Projections.constructor(
						CouponResponse.class,
						couponMember.couponId,
						coupon.type,
						couponMember.expireDate
				))
				.from(couponMember)
				.join(coupon).on(couponMember.couponId.eq(coupon.id))
				.where(couponMember.memberId.eq(memberId)
						.and(couponMember.expireDate.after(LocalDate.now())))
				.fetch();
	}
}
