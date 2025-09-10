package com.freeftr.coupon.member.application;

import com.freeftr.coupon.member.domain.Member;
import com.freeftr.coupon.member.domain.repository.MemberRepository;
import com.freeftr.coupon.member.dto.request.MemberCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private MemberRepository memberRepository;

    public void createMember(MemberCreateRequest request) {
        Member member = Member.builder()
                .name(request.name())
                .grade(request.grade())
                .build();

        memberRepository.saveMember(member);
    }
}
