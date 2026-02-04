package com.elice.cinema.domain.reservation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ReservationLockRepository {
    private final RedisTemplate<String, String> redisTemplate;

    public Boolean lock(Long screeningId, Long seatId, Long memberId, long lockTime, TimeUnit timeUnit) {
        String lockKey = seatHoldKey(screeningId, seatId);
        String lockValue = seatHoldLockValue(memberId);
        return redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockTime, timeUnit);
    }

    public Boolean unlock(Long screeningId, Long seatId) {
        return redisTemplate.delete(seatHoldKey(screeningId, seatId));
    }

    public void unlockAll(Long screeningId, List<Long> seatIds) {
        List<String> keys = seatIds.stream()
                .map(seatId -> seatHoldKey(screeningId, seatId))
                .toList();
        redisTemplate.delete(keys);
    }

    public String getLockOwner(Long screeningId, Long seatId) {
        String lockKey = seatHoldKey(screeningId, seatId);
        return redisTemplate.opsForValue().get(lockKey);
    }

    // 좌석 선점키 생성
    private String seatHoldKey(Long screeningId, Long seatId) {
        return "hold:screening:" + screeningId + ":seat:" + seatId;
    }

    private String seatHoldLockValue(Long memberId) {
        return "member:" + memberId;
    }
}
