package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.dto.response.CouponResponse;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponMemberRepositoryCustom {

	List<CouponResponse> findCouponsByMemberId(Long memberId);
}
