package com.elice.cinema.global.batch.scheduler.movie;

import com.elice.cinema.global.batch.service.movie.MovieStatusBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieStatusScheduler {

    private final MovieStatusBatchService movieStatusBatchService;

    /**
     * 매일 자정(한국시간)에:
     * - releaseDate == today: UPCOMING -> NOW_SHOWING
     * - endDate == today:     -> ENDED
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void updateMovieStatusesDaily() {
        movieStatusBatchService.updateStatusesAtMidnight();
    }
}
