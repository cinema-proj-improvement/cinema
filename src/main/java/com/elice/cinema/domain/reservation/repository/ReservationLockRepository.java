package com.elice.cinema.domain.reservation.repository;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ReservationLockRepository {
    private final StringRedisTemplate redisTemplate;

    @Observed(name = "seat.hold", contextualName = "seat-hold-lock")
    public Boolean lock(Long screeningId, Long seatId, Long memberId, long lockTime, TimeUnit timeUnit) {
        String lockKey = seatHoldKey(screeningId, seatId);
        String lockValue = seatHoldLockValue(memberId);
        return redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockTime, timeUnit);
    }
    private static final RedisScript<Long> LOCK_SCRIPT = loadScript("scripts/seat-lock.lua");
    private static final RedisScript<Long> UNLOCK_SCRIPT = loadScript("scripts/seat-unlock.lua");

    private static RedisScript<Long> loadScript(String classpathLocation) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(classpathLocation));
        script.setResultType(Long.class);
        return script;
    }

    // 좌석 전체를 원자적으로 잠금(하나라도 실패하면 이번 호출에서 잠근 것들은 자동 롤백)
    public boolean lockAll(Long screeningId, List<Long> seatIds, Long memberId, long ttl, TimeUnit timeUnit) {
        List<String> keys = seatIds.stream()
                .map(seatId -> seatHoldKey(screeningId, seatId))
                .toList();

        Long result = redisTemplate.execute(
                LOCK_SCRIPT,
                keys,
                seatHoldLockValue(memberId),
                String.valueOf(timeUnit.toMillis(ttl))
        );

        return Long.valueOf(1L).equals(result);
    }

    // 내가 잡은 좌석들만(값이 내 것일 때만) 안전하게 해제
    public void unlockAllSafely(Long screeningId, List<Long> seatIds, Long memberId) {
        List<String> keys = seatIds.stream()
                .map(seatId -> seatHoldKey(screeningId, seatId))
                .toList();

        redisTemplate.execute(UNLOCK_SCRIPT, keys, seatHoldLockValue(memberId));
    }

    // 좌석 선점키 생성
    private String seatHoldKey(Long screeningId, Long seatId) {
        return "hold:screening:" + screeningId + ":seat:" + seatId;
    }

    private String seatHoldLockValue(Long memberId) {
        return "member:" + memberId;
    }
}
