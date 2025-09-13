package com.freeftr.coupon.member.application;


import com.freeftr.coupon.member.domain.Member;
import com.freeftr.coupon.member.domain.enums.MemberGrade;
import com.freeftr.coupon.member.domain.repository.MemberRepository;
import com.freeftr.coupon.member.dto.request.MemberCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@InjectMocks
	private MemberService memberService;

	@Mock
	private MemberRepository memberRepository;

	@Test
	@DisplayName("회원가입할 수 있다.")
	void can_create_member() {
		//given
		Member member = Member.builder()
				.name("박종하")
				.grade(MemberGrade.ADMIN)
				.build();

		MemberCreateRequest memberCreateRequest = new MemberCreateRequest(
				"박종하",
				MemberGrade.ADMIN
		);

		//when
		memberService.createMember(memberCreateRequest);

		//then
		verify(memberRepository).saveMember(any(Member.class));
	}
}