package com.freeftr.coupon.coupon.domain.repository;

import com.freeftr.coupon.coupon.domain.CouponMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponMemberJpaRepository extends JpaRepository<CouponMember, Long>, CouponMemberRepositoryCustom {

	@Query("""
	SELECT cm
	FROM CouponMember cm
	WHERE cm.couponId = :couponId AND cm.memberId = :memberId
	""")
	Optional<CouponMember> findByCouponIdAndMemberId(Long couponId, Long memberId);
}
