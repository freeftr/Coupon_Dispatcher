package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.coupon.domain.enums.CouponIssueResult;
import com.freeftr.coupon.coupon.domain.enums.CouponType;
import com.freeftr.coupon.coupon.dto.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

	private static final String KEY_COUPON_MEMBERS = "coupon:%s:members";
	private static final String KEY_COUPON_COUNTER = "coupon:%s:counter";
	private static final String KEY_MEMBER_COUPONS = "member:%s:coupons";
	private static final String KEY_COUPON_TYPE = "coupon:%s:type";
	private static final String KEY_LOCK = "lock:%s";

	private static final long COUPON_TYPE_TTL_DAYS = 7;
	private static final long CACHE_TTL_DAYS = 3;
	private static final long LOCK_TTL_SECONDS = 5;

	private static final String COUPON_ISSUE_SCRIPT =
			"""
					local issuedMemberSet = KEYS[1]
					local couponCounter = KEYS[2]
					local limit = tonumber(ARGV[1])
					local memberId = ARGV[2]

					local issued = tonumber(redis.call('GET', couponCounter) or '0')

					if issued >= limit then
						return 1
					end

					if redis.call('SISMEMBER', issuedMemberSet, memberId) == 1 then
						return 2
					end

					redis.call('SADD', issuedMemberSet, memberId)
					redis.call('INCR', couponCounter)

					return 0
					""";

	private static final String ROLLBACK_ISSUE_SCRIPT =
			"""
					local issuedMemberSet = KEYS[1]
					local couponCounter = KEYS[2]
					local memberId = ARGV[1]

					if redis.call('SISMEMBER', issuedMemberSet, memberId) == 1 then
						redis.call('SREM', issuedMemberSet, memberId)
						redis.call('DECR', couponCounter)
						return 1
					end
					return 0
					""";

	private final RedisTemplate<String, String> redisTemplate;

	public CouponIssueResult issueCoupon(Long couponId, Long memberId, int limit) {
		String issuedMemberSetKey = String.format(KEY_COUPON_MEMBERS, couponId);
		String couponCounterKey = String.format(KEY_COUPON_COUNTER, couponId);

		Long result = redisTemplate.execute(
				new DefaultRedisScript<>(COUPON_ISSUE_SCRIPT, Long.class),
				Arrays.asList(issuedMemberSetKey, couponCounterKey),
				String.valueOf(limit),
				String.valueOf(memberId)
		);

		return CouponIssueResult.from(result);
	}

	public void rollbackIssueCoupon(Long couponId, Long memberId) {
		String issuedMemberSetKey = String.format(KEY_COUPON_MEMBERS, couponId);
		String couponCounterKey = String.format(KEY_COUPON_COUNTER, couponId);

		redisTemplate.execute(
				new DefaultRedisScript<>(ROLLBACK_ISSUE_SCRIPT, Long.class),
				Arrays.asList(issuedMemberSetKey, couponCounterKey),
				String.valueOf(memberId)
		);

		log.info("Redis rollback executed for couponId={}, memberId={}", couponId, memberId);
	}

	public List<CouponResponse> findCouponCacheByMemberId(Long memberId) {
		String memberCouponKey = String.format(KEY_MEMBER_COUPONS, memberId);

		long today = LocalDateTime.now().toLocalDate().toEpochDay();

		redisTemplate.opsForZSet().removeRangeByScore(
				memberCouponKey,
				Double.NEGATIVE_INFINITY,
				(double) today - 1
		);

		Set<ZSetOperations.TypedTuple<String>> couponInfoSet = redisTemplate.opsForZSet()
				.rangeByScoreWithScores(
						memberCouponKey,
						(double) today,
						Double.POSITIVE_INFINITY
				);

		if (couponInfoSet == null || couponInfoSet.isEmpty()) {
			return null;
		}

		List<ZSetOperations.TypedTuple<String>> couponInfos = couponInfoSet.stream().toList();

		List<Long> couponIds = couponInfos.stream()
				.map(info -> Long.parseLong(info.getValue()))
				.toList();
		List<String> couponIdKeys = couponInfos.stream()
				.map(info -> String.format(KEY_COUPON_TYPE, info.getValue()))
				.toList();

		List<String> couponTypes = redisTemplate.opsForValue().multiGet(couponIdKeys);
		Map<Long, String> couponTypeWithIds = new HashMap<>();

		if (couponTypes != null) {
			for (int i = 0; i < couponTypes.size(); i++) {
				couponTypeWithIds.put(couponIds.get(i), couponTypes.get(i));
			}
		}

		return couponInfos.stream()
				.map(info -> {
					Long couponId = Long.valueOf(info.getValue());
					long epochDay = info.getScore().longValue();
					LocalDate expireTime = LocalDate.ofEpochDay(epochDay);
					CouponType couponType = CouponType.from(couponTypeWithIds.get(couponId));
					return new CouponResponse(couponId, couponType, expireTime);
				})
				.toList();
	}

	@SuppressWarnings("unchecked")
	public void cacheCoupon(List<CouponResponse> couponInfo, Long memberId) {
		String memberCouponKey = String.format(KEY_MEMBER_COUPONS, memberId);

		LocalDate now = LocalDate.now();
		Set<ZSetOperations.TypedTuple<String>> validCoupons = new HashSet<>();
		Map<String, String> couponTypes = new HashMap<>();

		for (CouponResponse info : couponInfo) {
			if (!info.expireDate().isAfter(now)) continue;

			long score = info.expireDate().toEpochDay();

			validCoupons.add(new DefaultTypedTuple<>(
					String.valueOf(info.couponId()),
					(double) score));

			String key = String.format(KEY_COUPON_TYPE, info.couponId());
			couponTypes.put(key, info.couponType().toString());
		}

		if (validCoupons.isEmpty()) {
			redisTemplate.delete(memberCouponKey);
			return;
		}

		redisTemplate.execute(new SessionCallback<List<Object>>() {
			@Override
			public List<Object> execute(RedisOperations operations) throws DataAccessException {
				operations.multi();
				operations.delete(memberCouponKey);
				operations.opsForZSet().add(memberCouponKey, validCoupons);
				operations.expire(memberCouponKey, CACHE_TTL_DAYS, TimeUnit.DAYS);
				return operations.exec();
			}
		});

		// 쿠폰 타입 키에 TTL 설정
		redisTemplate.opsForValue().multiSet(couponTypes);
		couponTypes.keySet().forEach(key ->
				redisTemplate.expire(key, COUPON_TYPE_TTL_DAYS, TimeUnit.DAYS)
		);
	}

	public void removeCouponFromCache(Long memberId, Long couponId) {
		String memberCouponKey = String.format(KEY_MEMBER_COUPONS, memberId);
		redisTemplate.opsForZSet().remove(memberCouponKey, String.valueOf(couponId));
	}

	@SuppressWarnings("unchecked")
	public void addCouponToCache(CouponResponse couponResponse, Long memberId) {
		String memberCouponKey = String.format(KEY_MEMBER_COUPONS, memberId);
		String typeKey = String.format(KEY_COUPON_TYPE, couponResponse.couponId());

		LocalDate today = LocalDate.now();
		LocalDate expire = couponResponse.expireDate();

		if (!expire.isAfter(today)) return;

		double score = (double) expire.toEpochDay();

		redisTemplate.execute(new SessionCallback<List<Object>>() {
			@Override
			public List<Object> execute(RedisOperations operations) throws DataAccessException {
				operations.multi();
				operations.opsForZSet().add(memberCouponKey, String.valueOf(couponResponse.couponId()), score);
				operations.expire(memberCouponKey, CACHE_TTL_DAYS, TimeUnit.DAYS);
				operations.opsForValue().set(typeKey, couponResponse.couponType().toString());
				operations.expire(typeKey, COUPON_TYPE_TTL_DAYS, TimeUnit.DAYS);
				return operations.exec();
			}
		});

		log.info("cache added");
	}

	public boolean tryLock(String key) {
		String lockKey = String.format(KEY_LOCK, key);
		Boolean acquired = redisTemplate.opsForValue()
				.setIfAbsent(lockKey, "1", Duration.ofSeconds(LOCK_TTL_SECONDS));
		return Boolean.TRUE.equals(acquired);
	}

	public void unlock(String key) {
		String lockKey = String.format(KEY_LOCK, key);
		redisTemplate.delete(lockKey);
	}
}
