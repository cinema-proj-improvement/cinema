package com.elice.cinema.global.batch.service.screening;

import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
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
        log.info("상영 오픈 전환 완료: SCHEDULED→OPEN={}건, from={}, to={}", updated, from, to);
    }

    @Transactional
    public void finishEndedScreenings() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        int updated = screeningRepository.bulkUpdateToFinished(now);
        log.info("상영 종료 처리 완료: →FINISHED={}건, now={}", updated, now);
    }
}
