package com.elice.cinema.global.batch.scheduler.movie;

import com.elice.cinema.global.batch.service.movie.MovieReservationRateBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieReservationRateScheduler {

    private final MovieReservationRateBatchService batchService;

    // 하루 1회 (자정으로 기준했으나, 더미가 뒤에 올라와  30초로 수정)
    @Scheduled(cron = "*/30 * * * * *")
    public void run() {
        batchService.updateReservationRate();
    }
}
