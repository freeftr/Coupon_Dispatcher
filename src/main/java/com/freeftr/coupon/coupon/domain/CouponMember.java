package com.freeftr.coupon.coupon.domain;

import com.freeftr.coupon.common.entity.BaseEntity;
import com.freeftr.coupon.coupon.domain.enums.CouponMemberStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_member_id", nullable = false)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponMemberStatus status;

    @Builder
    public CouponMember(
            Long couponId,
            Long memberId,
            CouponMemberStatus status
    ) {
        this.couponId = couponId;
        this.memberId = memberId;
        this.status = CouponMemberStatus.ISSUED;
    }
}
