package com.freeftr.coupon.coupon.application;

import com.freeftr.coupon.coupon.domain.enums.QueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueRedisService {

    private static final String KEY_QUEUE = "coupon:%s:queue";
    private static final String KEY_QUEUE_RESULT = "coupon:%s:queue:result:%s";
    private static final String KEY_ACTIVE_QUEUES = "queue:active:coupons";

    /**
     * Lua 스크립트: 대기열 진입
     * KEYS[1] = active set, KEYS[2] = queue zset
     * ARGV[1] = couponId, ARGV[2] = memberId, ARGV[3] = timestamp
     * return: -1 (비활성), -2 (중복), 0+ (순번)
     */
    private static final String ENTER_QUEUE_SCRIPT =
            """
                    local activeSet = KEYS[1]
                    local queue = KEYS[2]
                    local couponId = ARGV[1]
                    local memberId = ARGV[2]
                    local timestamp = tonumber(ARGV[3])

                    if redis.call('SISMEMBER', activeSet, couponId) == 0 then
                        return -1
                    end

                    if redis.call('ZSCORE', queue, memberId) then
                        return -2
                    end

                    redis.call('ZADD', queue, timestamp, memberId)
                    return redis.call('ZRANK', queue, memberId)
                    """;

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.queue.result-ttl-minutes}")
    private int resultTtlMinutes;

    @Value("${app.queue.estimated-process-rate}")
    private int estimatedProcessRate;

    public Long enterQueue(Long couponId, Long memberId) {
        String queueKey = String.format(KEY_QUEUE, couponId);
        long timestamp = System.currentTimeMillis();

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(ENTER_QUEUE_SCRIPT, Long.class),
                Arrays.asList(KEY_ACTIVE_QUEUES, queueKey),
                String.valueOf(couponId),
                String.valueOf(memberId),
                String.valueOf(timestamp)
        );

        return result;
    }

    public Long getPosition(Long couponId, Long memberId) {
        String queueKey = String.format(KEY_QUEUE, couponId);
        return redisTemplate.opsForZSet().rank(queueKey, String.valueOf(memberId));
    }

    public List<ZSetOperations.TypedTuple<String>> popFromQueue(Long couponId, int count) {
        String queueKey = String.format(KEY_QUEUE, couponId);
        Set<ZSetOperations.TypedTuple<String>> members = redisTemplate.opsForZSet()
                .popMin(queueKey, count);

        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream().toList();
    }

    public void saveResult(Long couponId, Long memberId, QueueStatus status, String message) {
        String resultKey = String.format(KEY_QUEUE_RESULT, couponId, memberId);
        String value = status.name() + ":" + message;
        redisTemplate.opsForValue().set(resultKey, value, resultTtlMinutes, TimeUnit.MINUTES);
    }

    public String getResult(Long couponId, Long memberId) {
        String resultKey = String.format(KEY_QUEUE_RESULT, couponId, memberId);
        return redisTemplate.opsForValue().get(resultKey);
    }

    public void activateQueue(Long couponId) {
        redisTemplate.opsForSet().add(KEY_ACTIVE_QUEUES, String.valueOf(couponId));
    }

    public void deactivateQueue(Long couponId) {
        redisTemplate.opsForSet().remove(KEY_ACTIVE_QUEUES, String.valueOf(couponId));
    }

    public boolean isQueueActive(Long couponId) {
        Boolean isMember = redisTemplate.opsForSet().isMember(KEY_ACTIVE_QUEUES, String.valueOf(couponId));
        return Boolean.TRUE.equals(isMember);
    }

    public Set<String> getActiveCouponIds() {
        Set<String> members = redisTemplate.opsForSet().members(KEY_ACTIVE_QUEUES);
        return members != null ? members : Collections.emptySet();
    }

    public void drainQueueAsSoldOut(Long couponId) {
        String queueKey = String.format(KEY_QUEUE, couponId);

        while (true) {
            Set<ZSetOperations.TypedTuple<String>> batch = redisTemplate.opsForZSet().popMin(queueKey, 100);
            if (batch == null || batch.isEmpty()) break;

            for (ZSetOperations.TypedTuple<String> entry : batch) {
                Long memberId = Long.parseLong(entry.getValue());
                saveResult(couponId, memberId, QueueStatus.SOLD_OUT, "쿠폰이 품절되었습니다.");
            }
        }

        deactivateQueue(couponId);
    }

    public long estimateWaitSeconds(long position) {
        if (estimatedProcessRate <= 0) return 0;
        return (position / estimatedProcessRate) + 1;
    }

}
