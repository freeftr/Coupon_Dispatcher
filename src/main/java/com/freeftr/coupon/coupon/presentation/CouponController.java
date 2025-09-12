package com.freeftr.coupon.coupon.presentation;

import com.freeftr.coupon.coupon.application.CouponService;
import com.freeftr.coupon.coupon.dto.request.CouponCreateRequest;
import com.freeftr.coupon.coupon.dto.request.PeriodUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<Long> createCoupon(
            @RequestBody CouponCreateRequest request,
            @RequestParam(name = "memberId") Long memberId
    ) {
        return ResponseEntity.ok(couponService.createCoupon(request, memberId));
    }

    @PatchMapping
    public ResponseEntity<Void> updateCouponPeriod(
            @RequestBody PeriodUpdateRequest request,
            @RequestParam(name = "memberId") Long memberId
    ) {
        couponService.updatePeriod(request, memberId);
        return ResponseEntity.noContent().build();
    }
}
