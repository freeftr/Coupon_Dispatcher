package com.freeftr.coupon.member.dto.request;

import com.freeftr.coupon.member.domain.enums.MemberGrade;

public record MemberCreateRequest(
        String name,
        MemberGrade grade
) {
}
