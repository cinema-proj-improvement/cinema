package com.elice.cinema.global.batch.scheduler.movie;

import com.elice.cinema.global.batch.service.movie.OrphanImageCleanupBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrphanImageCleanupScheduler {

    private final OrphanImageCleanupBatchService orphanImageCleanupBatchService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void cleanupOrphanImages() {
        orphanImageCleanupBatchService.cleanup();
    }
}
