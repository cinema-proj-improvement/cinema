package com.elice.cinema.domain.screening.service;

import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.domain.screen.repository.ScreenRepository;
import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.dto.request.ScreeningCreateRequest;
import com.elice.cinema.domain.screening.dto.request.ScreeningUpdateRequest;
import com.elice.cinema.domain.screening.dto.response.*;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreeningService {
    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ScreeningMapper screeningMapper;
    private final ScreeningValidator screeningValidator;
    private final EnvironmentPolicyService environmentPolicyService;

    public List<ScreeningTimetableResponse> getTimetable(Long screenId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        List<Screening> screenings =
                screeningRepository.findTimetableByScreenAndDate(
                        screenId, from, to
                );

        return screenings.stream()
                .map(screeningMapper::toScreeningTimetableResponse)
                .toList();
    }

    public ScreeningDetailResponse getScreeningDetail(Long screeningId) {
        Screening screening = findScreeningById(screeningId);

        return screeningMapper.toScreeningDetailResponse(screening);
    }


    @Transactional
    public void createScreening(ScreeningCreateRequest req) {
        Movie movie = findMovieById(req.getMovieId());
        Screen screen = findScreenById(req.getScreenId());

        LocalDateTime endAt = calculateEndAt(movie, req.getStartAt());
        LocalDateTime endAtWithCleaning = calculateEndAtWithCleaning(endAt);

        screeningValidator.validateCreate(req, movie, screen, endAtWithCleaning);
        ScreeningStatus screeningStatus = screeningValidator.determineInitialStatus(req.getStartAt(), endAt);

        screen.addScreening(
                movie,
                req.getScreeningType(),
                req.getStartAt(),
                endAt,
                endAtWithCleaning,
                screeningStatus);
    }

    @Transactional
    public void updateScreening(Long screeningId, ScreeningUpdateRequest req) {
        Screening screening = findScreeningById(screeningId);

        screeningValidator.validateUpdate(screening, req);

        screening.updateScreeningStatus(req.getScreeningStatus());
    }

    // 관리자 상영 리스트
    public Page<AdminScreeningResponse> searchAdmin(
            AdminScreeningSearchRequest request,
            Pageable pageable
    ) {
        applyDefaultDateRange(request);

        Page<Screening> page = screeningRepository.searchAdmin(request, pageable);

        List<Long> screeningIds = page.getContent().stream()
                .map(Screening::getId)
                .toList();

        Map<Long, AdminScreeningSeatSummaryResponse> seatSummaryMap =
                loadSeatSummaryMap(screeningIds);

        return page.map(screening -> {
            AdminScreeningResponse base =
                    screeningMapper.toAdminListResponse(screening);

            AdminScreeningSeatSummaryResponse summary =
                    seatSummaryMap.get(screening.getId());

            if (summary == null) {
                long totalSeats = screening.getScreen().getTotalSeats();

                summary = AdminScreeningSeatSummaryResponse.of(
                        totalSeats, // total
                        0,          // confirmed
                        0           // hold
                );
            }

            return AdminScreeningResponse.withSeatSummary(base, summary);
        });
    }

    // 좌석 현황
    private Map<Long, AdminScreeningSeatSummaryResponse> loadSeatSummaryMap(
            List<Long> screeningIds
    ) {
        return screeningRepository
                .findSeatSummaryByScreeningIds(screeningIds)
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(0, Long.class),
                        t -> AdminScreeningSeatSummaryResponse.of(
                                t.get(1, Long.class),
                                t.get(2, Long.class),
                                t.get(3, Long.class)
                        )
                ));
    }

    public List<AdminScreeningFilterOptionResponse> getMovieFilterOptions() {
        return screeningRepository.findAdminScreeningMovieFilterOptions();
    }

    public List<AdminScreeningFilterOptionResponse> getScreenFilterOptions() {
        return screeningRepository.findAdminScreeningScreenFilterOptions();
    }

    // 상영별 좌석 목록 조회
    public List<AdminScreeningSeatResponse> getSeats(Long screeningId) {
        return screeningRepository.findAdminSeatsByScreeningId(screeningId);
    }

    // 상영 좌석 요약 조회
    public AdminScreeningSeatSummaryResponse getSeatSummary(Long screeningId) {
        return screeningRepository.findAdminSeatSummaryByScreeningId(screeningId);
    }

    @Transactional
    public void deleteScreening(Long screeningId) {
        Screening screening = findScreeningById(screeningId);
        screeningValidator.validateDelete(screening);
        screeningRepository.delete(screening);
    }

    public Screening getScreeningWithMovieAndScreen(Long screeningId) {  // ReservationService에 위치했던 Helper method를 이 위치로 옮겼습니다.
        return screeningRepository.findByIdWithMovieAndScreen(screeningId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCREENING_NOT_FOUND));
    }

    public boolean existsScreeningByMovieId(Long movieId) {
        return screeningRepository.existsByMovieId(movieId);
    }


    // 헬퍼 메서드
    private void applyDefaultDateRange(AdminScreeningSearchRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            LocalDate today = LocalDate.now();
            request.setStartDate(today);
            request.setEndDate(today.plusDays(7));
        }
    }

    private AdminScreeningResponse toAdminResponse(Screening screening) {
        return screeningMapper.toAdminListResponse(screening);
    }

    private Screening findScreeningById(Long screeningId) {
        return screeningRepository.findById(screeningId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCREENING_NOT_FOUND));
    }

    private Movie findMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
    }

    private Screen findScreenById(Long screenId) {
        return screenRepository.findById(screenId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCREEN_NOT_FOUND));
    }

    private LocalDateTime calculateEndAt(Movie movie, LocalDateTime startAt) {
        return startAt.plusMinutes(movie.getRunningTimeMinutes());
    }

    private LocalDateTime calculateEndAtWithCleaning(LocalDateTime endAt) {
        int cleaningMinutes = environmentPolicyService.getCleaningMinutes();
        return endAt.plusMinutes(cleaningMinutes);
    }
}