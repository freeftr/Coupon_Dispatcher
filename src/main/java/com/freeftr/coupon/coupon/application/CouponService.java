package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.Coupon;
import com.freeftr.coupon.coupon.domain.repository.CouponRepository;
import com.freeftr.coupon.coupon.dto.request.CouponCreateRequest;
import com.freeftr.coupon.coupon.dto.request.PeriodUpdateRequest;
import com.freeftr.coupon.member.domain.Member;
import com.freeftr.coupon.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;

    public Long createCoupon(CouponCreateRequest request, Long memberId) {

        Member member = getMember(memberId);

        checkAdmin(member);

        Coupon coupon = Coupon.builder()
                .type(request.type())
                .validityPeriod(request.validityPeriod())
                .build();

        return couponRepository.saveCoupon(coupon).getId();
    }

    @Transactional
    public void updatePeriod(PeriodUpdateRequest request, Long memberId) {

        Member member = getMember(memberId);

        checkAdmin(member);

        Coupon coupon = couponRepository.findById(request.couponId())
                .orElseThrow(() -> new BadRequestException(ErrorCode.COUPON_NOT_FOUND));

        coupon.updatePeriod(request.newPeriod());
    }

    private static void checkAdmin(Member member) {
        if (!member.isAdmin(member)) {
            throw new BadRequestException(ErrorCode.NOT_AN_ADMIN);
        }
    }

    private Member getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.MEMBER_NOT_FOUND));
        return member;
    }
}
