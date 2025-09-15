package com.freeftr.coupon.member.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freeftr.coupon.member.application.MemberService;
import com.freeftr.coupon.member.domain.enums.MemberGrade;
import com.freeftr.coupon.member.dto.request.MemberCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MemberService memberService;

	@Test
	@DisplayName("회원을 생성한다.")
	void createMember_returnsNoContent() throws Exception {
		// given
		MemberCreateRequest request = new MemberCreateRequest("박종하", MemberGrade.ADMIN);
		doNothing().when(memberService).createMember(request);

		// when & then
		mockMvc.perform(post("/api/v1/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent());
	}
}
