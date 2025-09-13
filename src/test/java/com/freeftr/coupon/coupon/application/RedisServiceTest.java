package com.freeftr.coupon.coupon.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RedisServiceTest {

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
	RedisService redisService;

	@Autowired
	RedisTemplate redisTemplate;

	@BeforeEach
	void clean() {
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	@Test
	@DisplayName("Redis로 쿠폰 발급 가능을 확인할 수 있다.")
	void can_issue_coupon() {
		Long couponId = 1L;
		Long member1 = 100L;

		int limit = 1;

		int success = redisService.issueCoupon(couponId, member1, limit);

		assertThat(success).isEqualTo(0);
	}

	@Test
	@DisplayName("중복된 쿠폰은 발급받지 못한다.")
	void can_not_issue_duplicate_coupon() {
		Long couponId = 1L;
		Long member1 = 100L;

		int limit = 2;

		int initial = redisService.issueCoupon(couponId, member1, limit);
		int duplicate = redisService.issueCoupon(couponId, member1, limit);

		assertThat(duplicate).isEqualTo(2);
	}

	@Test
	@DisplayName("발급 한도를 넘은 쿠폰은 발급받지 못한다.")
	void can_not_issue_sold_out_coupon() {
		Long couponId = 1L;
		Long member1 = 100L;

		int limit = 1;

		int initial = redisService.issueCoupon(couponId, member1, limit);
		int soldOut = redisService.issueCoupon(couponId, member1, limit);

		assertThat(soldOut).isEqualTo(1);
	}

	@Test
	@DisplayName("쿠폰 발급은 limit까지만 가능하다.")
	void check_coupon_limit() {
		Long couponId = 2L;
		int limit = 5;

		int successCount = 0;
		int soldOutCount = 0;

		for (int i = 0; i < 10; i++) {
			int result = redisService.issueCoupon(couponId, (long) i, limit);
			if (result == 0) successCount++;
			else if (result == 1) soldOutCount++;
		}

		assertThat(successCount).isEqualTo(limit);
		assertThat(soldOutCount).isEqualTo(10 - limit);
	}
}
