package com.freeftr.coupon.coupon.domain;

import com.freeftr.coupon.common.entity.BaseEntity;
import com.freeftr.coupon.coupon.domain.enums.CouponType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id", nullable = false)
    private Long id;

    @Column(name = "coupon_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponType type;

    @Column(name = "validity_period", nullable = false)
    private LocalDateTime validityPeriod;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public void updatePeriod (LocalDateTime period) {
        this.validityPeriod = period;
    }

    public boolean checkQuantity(int issued) {
        return this.quantity > issued;
    }

    @Builder
    public Coupon (
            CouponType type,
            LocalDateTime validityPeriod,
            Integer quantity
    ) {
        this.type = type;
        this.validityPeriod = validityPeriod;
        this.quantity = quantity;
    }
}
