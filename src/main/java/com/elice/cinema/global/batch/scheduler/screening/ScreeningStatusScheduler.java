package com.elice.cinema.global.batch.scheduler.screening;

import com.elice.cinema.global.batch.service.screening.ScreeningStatusBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScreeningStatusScheduler {

    private final ScreeningStatusBatchService screeningStatusBatchService;

    /**
     * 매일 자정(한국시간)에:
     * SCHEDULED 중에서 "상영일이 7일 이내"인 것들을 OPEN으로 변경
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul") // 초 분 시 일 월 요일
    public void openScheduledScreenings() {
        screeningStatusBatchService.openScreeningsWithinScheduledToOpenDays();
    }

    /**
     * 상영 종료는 각 상영의 endAt이 제각각이라 "자정 1번"은 늦을 수 있음.
     * 보통 1~5분마다 돌려서 끝난 것들을 FINISHED로 바꾸는 게 실무적으로 자연스러움.
     */
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul") // 1분마다
    public void finishEndedScreenings() {
        screeningStatusBatchService.finishEndedScreenings();
    }
}

