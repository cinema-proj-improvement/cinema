package com.elice.cinema.global.batch.service.screening;

import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ScreeningStatusBatchService {

    private final ScreeningRepository screeningRepository;
    private final EnvironmentPolicyService environmentPolicyService;

    @Transactional
    public void openScreeningsWithinScheduledToOpenDays() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(environmentPolicyService.getScheduledToOpenDays()).atTime(23, 59, 59, 999_999_999);

        int updated = screeningRepository.bulkUpdateScheduledToOpen(from, to);
    }

    @Transactional
    public void finishEndedScreenings() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        int updated = screeningRepository.bulkUpdateToFinished(now);
    }
}
