package com.freeftr.coupon.couponhistory.domain;

import com.freeftr.coupon.common.entity.BaseEntity;
import com.freeftr.coupon.couponhistory.domain.enums.HistoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "coupon_history_id", nullable = false)
	private Long id;

	@Column(name = "coupon_member_id", nullable = false)
	private Long couponMemberId;

	@Enumerated(EnumType.STRING)
	@Column(name = "history_type", nullable = false)
	private HistoryType type;

	@Builder
	public CouponHistory(
			Long couponMemberId,
			HistoryType type
	) {
		this.couponMemberId = couponMemberId;
		this.type = type;
	}
}
