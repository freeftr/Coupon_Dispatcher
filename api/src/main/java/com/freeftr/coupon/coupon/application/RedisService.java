package com.freeftr.coupon.coupon.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class RedisService {

	private static final String COUPON_ISSUE_SCRIPT =
			"""
			-- 발급한 사용자 검증용
			local issuedMemberSet = KEYS[1]
			-- 쿠폰 한도 검증용
			local couponCounter = KEYS[2]
			local limit = tonumber(ARGV[1])
			local memberId = ARGV[2]
			
			-- 발급된 수량, 기본값 0
			local issued = tonumber(redis.call('GET', couponCounter) or '0')
	
			-- 발급 한도 검증
			if issued >= limit then 
				-- 1: 이미 품절된 쿠폰입니다.
				return 1
			end
	
			-- 중복 발급 검증
			if redis.call('SISMEMBER', issuedMemberSet, memberId) == 1 then 
				-- 2: 이미 발급한 쿠폰입니다.
				return 2
			end
	
			redis.call('SADD', issuedMemberSet, memberId)
			redis.call('INCR', couponCounter)
	
			-- 쿠폰 발급에 성공
			return 0
			""";

	private final StringRedisTemplate redisTemplate;

	/*
	- 0: 성공
	- 1: 품절
	- 2: 중복 발급
	 */
	public int issueCoupon(Long couponId, Long memberId, int limit) {

		String issuedMemberSetKey = "coupon:" + couponId + ":members";
		String couponCounterKey   = "coupon:" + couponId + ":counter";

		Long result = redisTemplate.execute(
				new DefaultRedisScript<>(COUPON_ISSUE_SCRIPT, Long.class),
				Arrays.asList(issuedMemberSetKey, couponCounterKey),
				String.valueOf(limit),
				String.valueOf(memberId)
		);

		return result.intValue();
	}
}
