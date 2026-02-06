package com.elice.cinema.global.batch.scheduler.movie;

import com.elice.cinema.global.batch.service.movie.MovieReservationRateBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieReservationRateScheduler {

    private final MovieReservationRateBatchService batchService;

    // 하루 1회 (자정)
    @Scheduled(cron = "0 0 0 * * ?")
    public void run() {
        batchService.updateReservationRate();
    }
}
