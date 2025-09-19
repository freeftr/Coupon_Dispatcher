package com.freeftr.coupon.coupon.domain;

import com.freeftr.coupon.common.entity.BaseEntity;
import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.enums.CouponMemberStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "coupon_member",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "member_coupon",
                        columnNames = {"member_id", "coupon_id"}
                )
        }
)
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

    @Column(name = "valide_date", nullable = false)
    private LocalDate expireDate;

    public boolean isAuthor(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void useCoupon(LocalDate now) {
        if (this.status == CouponMemberStatus.USED) {
            throw new BadRequestException(ErrorCode.COUPON_ALREADY_USED);
        }

        if (this.expireDate.isBefore(now)) {
            this.status = CouponMemberStatus.EXPIRED;
            throw new BadRequestException(ErrorCode.COUPON_EXPIRED);
        }

        this.status = CouponMemberStatus.USED;
    }

    @Builder
    public CouponMember(
            Long couponId,
            Long memberId,
            LocalDate expireDate
    ) {
        this.couponId = couponId;
        this.memberId = memberId;
        this.status = CouponMemberStatus.ISSUED;
        this.expireDate = expireDate;
    }
}
