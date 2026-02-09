package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.coupon.domain.enums.QueueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class QueueRedisServiceTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    QueueRedisService queueRedisService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void clean() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("활성화된 대기열에 진입할 수 있다.")
    void enter_active_queue() {
        Long couponId = 1L;
        Long memberId = 100L;

        queueRedisService.activateQueue(couponId);

        Long position = queueRedisService.enterQueue(couponId, memberId);

        assertThat(position).isEqualTo(0L);
    }

    @Test
    @DisplayName("비활성 대기열에 진입하면 -1을 반환한다.")
    void enter_inactive_queue() {
        Long couponId = 1L;
        Long memberId = 100L;

        Long result = queueRedisService.enterQueue(couponId, memberId);

        assertThat(result).isEqualTo(-1L);
    }

    @Test
    @DisplayName("중복 진입 시 -2를 반환한다.")
    void enter_duplicate() {
        Long couponId = 1L;
        Long memberId = 100L;

        queueRedisService.activateQueue(couponId);
        queueRedisService.enterQueue(couponId, memberId);

        Long result = queueRedisService.enterQueue(couponId, memberId);

        assertThat(result).isEqualTo(-2L);
    }

    @Test
    @DisplayName("대기열 순번을 조회할 수 있다.")
    void get_position() {
        Long couponId = 1L;

        queueRedisService.activateQueue(couponId);
        queueRedisService.enterQueue(couponId, 1L);
        queueRedisService.enterQueue(couponId, 2L);
        queueRedisService.enterQueue(couponId, 3L);

        Long position = queueRedisService.getPosition(couponId, 3L);

        assertThat(position).isEqualTo(2L);
    }

    @Test
    @DisplayName("대기열에 없는 사용자의 순번은 null이다.")
    void get_position_not_found() {
        Long couponId = 1L;

        Long position = queueRedisService.getPosition(couponId, 999L);

        assertThat(position).isNull();
    }

    @Test
    @DisplayName("대기열에서 batch size만큼 pop한다.")
    void pop_from_queue() {
        Long couponId = 1L;

        queueRedisService.activateQueue(couponId);
        for (long i = 1; i <= 5; i++) {
            queueRedisService.enterQueue(couponId, i);
        }

        List<ZSetOperations.TypedTuple<String>> popped = queueRedisService.popFromQueue(couponId, 3);

        assertThat(popped).hasSize(3);
    }

    @Test
    @DisplayName("발급 결과를 저장하고 조회할 수 있다.")
    void save_and_get_result() {
        Long couponId = 1L;
        Long memberId = 100L;

        queueRedisService.saveResult(couponId, memberId, QueueStatus.SUCCESS, "쿠폰이 발급되었습니다.");

        String result = queueRedisService.getResult(couponId, memberId);

        assertThat(result).isEqualTo("SUCCESS:쿠폰이 발급되었습니다.");
    }

    @Test
    @DisplayName("대기열 활성화/비활성화를 확인할 수 있다.")
    void activate_deactivate_queue() {
        Long couponId = 1L;

        assertThat(queueRedisService.isQueueActive(couponId)).isFalse();

        queueRedisService.activateQueue(couponId);
        assertThat(queueRedisService.isQueueActive(couponId)).isTrue();

        queueRedisService.deactivateQueue(couponId);
        assertThat(queueRedisService.isQueueActive(couponId)).isFalse();
    }

    @Test
    @DisplayName("품절 시 남은 대기열을 모두 drain한다.")
    void drain_queue_as_sold_out() {
        Long couponId = 1L;

        queueRedisService.activateQueue(couponId);
        for (long i = 1; i <= 5; i++) {
            queueRedisService.enterQueue(couponId, i);
        }

        queueRedisService.drainQueueAsSoldOut(couponId);

        assertThat(queueRedisService.isQueueActive(couponId)).isFalse();

        for (long i = 1; i <= 5; i++) {
            String result = queueRedisService.getResult(couponId, i);
            assertThat(result).startsWith("SOLD_OUT:");
        }
    }
}
