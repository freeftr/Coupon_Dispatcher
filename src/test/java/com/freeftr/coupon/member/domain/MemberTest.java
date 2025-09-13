package com.freeftr.coupon.member.domain;

import com.freeftr.coupon.member.domain.enums.MemberGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

	@Test
	@DisplayName("ADMIN 등급이면 관리자임을 판별한다.")
	void isAdmin_returnsTrue_forAdminGrade() {
		// given
		Member admin = Member.builder()
				.name("관리자")
				.grade(MemberGrade.ADMIN)
				.build();

		// when
		boolean result = admin.isAdmin(admin);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("ADMIN 등급이 아니면 관리자가 아님을 판별한다.")
	void isAdmin_returnsFalse_forNonAdminGrade() {
		// given
		Member user = Member.builder()
				.name("일반회원")
				.grade(MemberGrade.MEMBER)
				.build();

		// when
		boolean result = user.isAdmin(user);

		// then
		assertThat(result).isFalse();
	}
}
