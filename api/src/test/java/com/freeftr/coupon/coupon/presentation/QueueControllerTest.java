package com.freeftr.coupon.coupon.presentation;

import com.freeftr.coupon.coupon.application.CouponService;
import com.freeftr.coupon.coupon.application.QueueRedisService;
import com.freeftr.coupon.coupon.domain.enums.QueueStatus;
import com.freeftr.coupon.member.application.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueueController.class)
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueRedisService queueRedisService;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("대기열에 진입한다.")
    void enter_queue() throws Exception {
        doNothing().when(memberService).validateMemberExists(anyLong());
        given(queueRedisService.enterQueue(1L, 2L)).willReturn(0L);
        given(queueRedisService.estimateWaitSeconds(0L)).willReturn(1L);

        mockMvc.perform(post("/api/v1/coupons/{couponId}/queue", 1L)
                        .param("memberId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.estimatedWaitSeconds").value(1));
    }

    @Test
    @DisplayName("비활성 대기열에 진입하면 에러를 반환한다.")
    void enter_inactive_queue() throws Exception {
        doNothing().when(memberService).validateMemberExists(anyLong());
        given(queueRedisService.enterQueue(1L, 2L)).willReturn(-1L);

        mockMvc.perform(post("/api/v1/coupons/{couponId}/queue", 1L)
                        .param("memberId", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 진입하면 에러를 반환한다.")
    void enter_duplicate_queue() throws Exception {
        doNothing().when(memberService).validateMemberExists(anyLong());
        given(queueRedisService.enterQueue(1L, 2L)).willReturn(-2L);

        mockMvc.perform(post("/api/v1/coupons/{couponId}/queue", 1L)
                        .param("memberId", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("대기열 순번을 조회한다.")
    void get_position() throws Exception {
        given(queueRedisService.getPosition(1L, 2L)).willReturn(5L);
        given(queueRedisService.estimateWaitSeconds(5L)).willReturn(1L);

        mockMvc.perform(get("/api/v1/coupons/{couponId}/queue/position", 1L)
                        .param("memberId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(5));
    }

    @Test
    @DisplayName("대기열에 없는 사용자의 순번 조회 시 에러를 반환한다.")
    void get_position_not_found() throws Exception {
        given(queueRedisService.getPosition(1L, 2L)).willReturn(null);

        mockMvc.perform(get("/api/v1/coupons/{couponId}/queue/position", 1L)
                        .param("memberId", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("발급 결과를 조회한다.")
    void get_result() throws Exception {
        given(queueRedisService.getResult(1L, 2L)).willReturn("SUCCESS:쿠폰이 발급되었습니다.");

        mockMvc.perform(get("/api/v1/coupons/{couponId}/queue/result", 1L)
                        .param("memberId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("쿠폰이 발급되었습니다."));
    }

    @Test
    @DisplayName("결과가 없으면 에러를 반환한다.")
    void get_result_not_found() throws Exception {
        given(queueRedisService.getResult(1L, 2L)).willReturn(null);

        mockMvc.perform(get("/api/v1/coupons/{couponId}/queue/result", 1L)
                        .param("memberId", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("관리자가 대기열을 활성화한다.")
    void activate_queue() throws Exception {
        doNothing().when(memberService).validateAdmin(1L);
        given(queueRedisService.isQueueActive(1L)).willReturn(false);

        mockMvc.perform(post("/api/v1/coupons/{couponId}/queue/activate", 1L)
                        .param("memberId", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자가 대기열을 비활성화한다.")
    void deactivate_queue() throws Exception {
        doNothing().when(memberService).validateAdmin(1L);

        mockMvc.perform(post("/api/v1/coupons/{couponId}/queue/deactivate", 1L)
                        .param("memberId", "1"))
                .andExpect(status().isNoContent());
    }
}
