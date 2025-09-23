package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.common.exception.BadRequestException;
import com.freeftr.coupon.common.exception.ErrorCode;
import com.freeftr.coupon.coupon.domain.Coupon;
import com.freeftr.coupon.coupon.domain.repository.CouponRepository;
import com.freeftr.coupon.coupon.dto.request.CouponCreateRequest;
import com.freeftr.coupon.coupon.dto.request.PeriodUpdateRequest;
import com.freeftr.coupon.coupon.dto.response.CouponCreateResponse;
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

    public CouponCreateResponse createCoupon(CouponCreateRequest request, Long memberId) {

        Member member = getMember(memberId);

        checkAdmin(member);

        Coupon coupon = Coupon.builder()
                .type(request.type())
                .validityPeriod(request.validityPeriod())
                .quantity(request.quantity())
                .build();

        return new CouponCreateResponse(couponRepository.saveCoupon(coupon).getId());
    }

    @Transactional
    public void updatePeriod(PeriodUpdateRequest request, Long memberId) {

        Member member = getMember(memberId);

        checkAdmin(member);

        /**
         ErrorResponse로 바로 설정하는 방식 => 아래 처럼 ErrorCode가 드러나지 않아 유지보수시 다른 팀원이 다른 클래스가 있나?
         라고 볼필요가 없다. => EnumCode에서만 찾으면 되기 때문에.
         현재 방식은 대신 ErrorCode라고 명시적으로 작성함으로써 여기안에 있는 친구라고 명시적으로 알려주는 방식이다.
         만약 ErrorCode말고 다른 ~Code들이 생겨나게 된다면 클래스 개수 자체가 늘어날 수 있다는 단점이 있다.

          */
        Coupon coupon = couponRepository.findById(request.couponId())
                .orElseThrow(() -> new BadRequestException(ErrorCode.COUPON_NOT_FOUND));

        coupon.updatePeriod(request.newPeriod());
    }

    private void checkAdmin(Member member) {
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
