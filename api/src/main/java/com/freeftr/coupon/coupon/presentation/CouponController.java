package com.freeftr.coupon.coupon.presentation;

import com.freeftr.coupon.coupon.application.CouponMemberService;
import com.freeftr.coupon.coupon.application.CouponService;
import com.freeftr.coupon.coupon.domain.Coupon;
import com.freeftr.coupon.coupon.dto.request.CouponCreateRequest;
import com.freeftr.coupon.coupon.dto.request.PeriodUpdateRequest;
import com.freeftr.coupon.coupon.dto.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponMemberService couponMemberService;

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

    @PostMapping("/{couponId}")
    public ResponseEntity<Void> issueCoupon(
            @PathVariable(name = "couponId") Long couponId,
            @RequestParam(name = "memberId") Long memberId
    ) {
        couponMemberService.issueCoupon(couponId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/issued/{couponMemberId}")
    public ResponseEntity<Void> updateCouponMember(
            @PathVariable(name = "couponMemberId") Long couponMemberId,
            @RequestParam(name = "memberId") Long memberId
    ) {
        couponMemberService.useCoupon(couponMemberId, memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<CouponResponse>> findCoupons(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(couponMemberService.findCouponsByMemberId(memberId));
    }

}
