package com.elice.cinema.global.batch.scheduler.screening;

import com.elice.cinema.global.batch.service.screening.ScreeningStatusBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScreeningStatusScheduler {

    private final ScreeningStatusBatchService screeningStatusBatchService;

    // 서버가 여러 대이면 같은 UPDATE 쿼리를 여러 번 실행할 수 있으므로 데이터 중복 변경/버그 발생 가능
    // 그때는 스케줄 락을 걸어서 여러 서버 중 1개의 스케줄만 실행하게 만들어줘야함

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

