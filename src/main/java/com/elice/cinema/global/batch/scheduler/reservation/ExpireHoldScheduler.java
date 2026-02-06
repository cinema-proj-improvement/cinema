package com.elice.cinema.global.batch.scheduler.reservation;

import com.elice.cinema.global.batch.service.reservation.ExpireHoldBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpireHoldScheduler {
    private final ExpireHoldBatchService expireHoldBatchService;

    /**
     * HOLD 만료 처리 스케줄러
     * - fixedDelay: 이전 실행이 끝난 시점 기준으로 대기
     * - 배치 누락/중첩 실행 방지에 유리
     */
    @Scheduled(fixedDelay = 60_000, zone = "Asia/Seoul")  // 1분
    public void expireHolds() {
        expireHoldBatchService.expireHolds();
    }
}
