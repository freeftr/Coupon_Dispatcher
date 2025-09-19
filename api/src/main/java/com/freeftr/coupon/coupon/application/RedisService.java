package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.coupon.domain.enums.CouponType;
import com.freeftr.coupon.coupon.dto.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {

	private static final String KEY_COUPON_MEMBERS = "coupon:%s:members";
	private static final String KEY_COUPON_COUNTER = "coupon:%s:counter";
	private static final String KEY_MEMBER_COUPONS = "member:%s:coupons";
	private static final String KEY_COUPON_TYPE = "coupon:%s:type";

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

	private final RedisTemplate redisTemplate;

	/**
	 * - 0: 성공
	 * - 1: 품절
	 * - 2: 중복 발급
	 **/
	public String issueCoupon(Long couponId, Long memberId, int limit) {

		String issuedMemberSetKey = String.format(KEY_COUPON_MEMBERS, couponId);
		String couponCounterKey   = String.format(KEY_COUPON_COUNTER, couponId);

		String result = redisTemplate.execute(
				new DefaultRedisScript<>(COUPON_ISSUE_SCRIPT, Long.class),
				Arrays.asList(issuedMemberSetKey, couponCounterKey),
				limit,
				String.valueOf(memberId)
		).toString();

		return result;
	}

	public List<CouponResponse> findCouponCacheByMemberId(Long memberId) {
		String memberCouponKey = String.format(KEY_MEMBER_COUPONS, memberId);

		long today = LocalDateTime.now().toLocalDate()
				.toEpochDay();

		// 만료된 쿠폰 제거
		redisTemplate.opsForZSet().removeRangeByScore(
						memberCouponKey,
						Double.NEGATIVE_INFINITY,
				(double) today - 1
		);

		// 캐시 조회
		List<ZSetOperations.TypedTuple<String>> couponInfos = redisTemplate.opsForZSet()
						.rangeByScoreWithScores(
								memberCouponKey,
								(double) today,
								Double.POSITIVE_INFINITY).
				stream()
				.toList();

		
		// Cache Miss
		if (couponInfos == null || couponInfos.isEmpty()) {
			return null;
		}
		List<Long> couponIds = couponInfos.stream()
				.map(info -> Long.parseLong(info.getValue()))
				.toList();
		List<String> couponIdKeys = couponInfos.stream()
				.map(info -> String.format(KEY_COUPON_TYPE, info.getValue()))
				.toList();

		List<String> couponTypes = redisTemplate.opsForValue().multiGet(couponIdKeys);
		Map<Long, String> couponTypeWithIds = new HashMap<>();

		for (int i = 0; i < couponTypes.size(); i++) {
			Long couponId = couponIds.get(i);
			String couponType = couponTypes.get(i);

			couponTypeWithIds.put(couponId, couponType);
		}


		return couponInfos.stream()
				.map(info -> {
					Long couponId = Long.valueOf(String.valueOf(info.getValue()));
					long epochDay = info.getScore().longValue();
					LocalDate expireTime = LocalDate.ofEpochDay(epochDay);
					CouponType couponType = CouponType.from(couponTypeWithIds.get(couponId));
					return new CouponResponse(
							couponId,
							couponType,
							expireTime
					);
				})
				.toList();
	}


	public void cacheCoupon(List<CouponResponse> couponInfo, Long memberId) {
		String memberCouponKey = String.format(KEY_MEMBER_COUPONS, memberId);

		LocalDate now = LocalDate.now();
		Set<ZSetOperations.TypedTuple<Object>> validCoupons = new HashSet<>();
		Map<String, String> couponTypes = new HashMap<>();

		for (CouponResponse info : couponInfo) {
			// 만료된 쿠폰 다시 검증
			if (!info.expireDate().isAfter(now)) continue;

			// 남은 기간 기준으로 점수 부여 (날짜 단위)
			long score = info.expireDate()
					.toEpochDay();

			validCoupons.add(new DefaultTypedTuple<>(
					String.valueOf(info.couponId()),
					(double) score));

			String key = String.format(KEY_COUPON_TYPE, info.couponId());

			couponTypes.put(key, info.couponType().toString());
		}

		redisTemplate.delete(memberCouponKey);

		if (validCoupons.isEmpty()) {
			return;
		}

		redisTemplate.opsForZSet().add(memberCouponKey, validCoupons);
		redisTemplate.expire(memberCouponKey, 3, TimeUnit.DAYS);
		redisTemplate.opsForValue().multiSet(couponTypes);
	}
}
