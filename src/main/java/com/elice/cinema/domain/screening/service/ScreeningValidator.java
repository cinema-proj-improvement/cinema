package com.elice.cinema.domain.screening.service;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.domain.screening.dto.request.ScreeningCreateRequest;
import com.elice.cinema.domain.screening.dto.request.ScreeningUpdateRequest;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ScreeningValidator {
    private final ScreeningRepository screeningRepository;
    private final EnvironmentPolicyService environmentPolicyService;

    public void validateCreate(ScreeningCreateRequest req, Movie movie, Screen screen, LocalDateTime endAtWithCleaning) {
        validateScreeningTypeForMovie(movie, req.getScreeningType());
        validateScreeningTypeForScreen(screen, req.getScreeningType());

        validateStartAtPolicy(movie, req.getStartAt());
        validateTimeConflict(screen.getId(), req.getStartAt(), endAtWithCleaning);
    }

    /**
     * 상영 수정 정책:
     * - 현재 상태가 SCHEDULED일 때만 변경 가능
     * - 변경 가능한 상태는 CANCELED(또는 너희 enum명)만 가능
     */
    // TODO: 환불 만들고 상영 수정 정책 수정 필요
    public void validateUpdate(Screening screening, ScreeningUpdateRequest req) {
        if (screening.getScreeningStatus() != ScreeningStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.SCREENING_STATUS_CHANGE_NOT_ALLOWED);
        }

        // enum이 진짜 CANCELED이면 여기만 맞춰 바꿔주면 됨
        if (req.getScreeningStatus() != ScreeningStatus.CANCELED) {
            throw new BusinessException(ErrorCode.SCREENING_ONLY_CAN_CANCEL);
        }
    }

    /**
     * 상영 삭제 정책:
     * - 현재 상태가 SCHEDULED일 때만 삭제 가능
     */
    public void validateDelete(Screening screening) {
        if (screening.getScreeningStatus() != ScreeningStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.SCREENING_DELETE_NOT_ALLOWED);
        }
    }

    /* ===== validate ===== */
    // 영화가 해당 상영 타입을 지원하는지
    private void validateScreeningTypeForMovie(Movie movie, ScreeningType type) {
        if (type == null || !movie.getScreeningTypes().contains(type)) {
            throw new BusinessException(ErrorCode.SCREENING_TYPE_NOT_SUPPORTED_BY_MOVIE);
        }
    }

    // 상영관이 해당 상영 타입인지 (상영관은 1개 타입만 가능하다고 했으니 == 비교)
    private void validateScreeningTypeForScreen(Screen screen, ScreeningType type) {
        if (type == null || screen.getScreeningType() != type) {
            throw new BusinessException(ErrorCode.SCREENING_TYPE_NOT_MATCH_SCREEN);
        }
    }

    // 개봉일/종료일 정책
    private void validateStartAtPolicy(Movie movie, LocalDateTime startAt) {
        if (startAt == null) {
            throw new BusinessException(ErrorCode.SCREENING_START_AT_REQUIRED);
        }

        if (startAt.toLocalDate().isBefore(movie.getReleaseDate())) {
            throw new BusinessException(ErrorCode.SCREENING_BEFORE_RELEASE_DATE);
        }

        if (movie.getEndDate() != null && startAt.toLocalDate().isAfter(movie.getEndDate())) {
            throw new BusinessException(ErrorCode.SCREENING_AFTER_END_DATE);
        }
    }

    // 시간 충돌 (겹침)
    private void validateTimeConflict(Long screenId, LocalDateTime startAt, LocalDateTime endAtWithCleaning) {
        boolean conflict = screeningRepository.existsTimeConflict(screenId, startAt, endAtWithCleaning);
        if (conflict) {
            throw new BusinessException(ErrorCode.SCREENING_TIME_CONFLICT);
        }
    }

    public ScreeningStatus determineInitialStatus(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 1) 상영 종료 이후면 생성 불가 (이건 시간 기준 유지)
        if (now.isAfter(endAt)) {
            throw new BusinessException(ErrorCode.SCREENING_ALREADY_ENDED);
        }

        // 2) 날짜 기준 D-7 오픈 정책
        LocalDate today = now.toLocalDate();
        LocalDate openDate = startAt
                .toLocalDate()
                .minusDays(environmentPolicyService.getScheduledToOpenDays());

        // 오늘이 오픈 날짜보다 이전이면 → SCHEDULED
        if (today.isBefore(openDate)) {
            return ScreeningStatus.SCHEDULED;
        }

        return ScreeningStatus.OPEN;
    }
}
