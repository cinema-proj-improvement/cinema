package com.elice.cinema.domain.screening.service;

import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.domain.screen.repository.ScreenRepository;
import com.elice.cinema.domain.screening.dto.reponse.ScreeningDetailResponse;
import com.elice.cinema.domain.screening.dto.reponse.ScreeningTimetableResponse;
import com.elice.cinema.domain.screening.dto.request.ScreeningCreateRequest;
import com.elice.cinema.domain.screening.dto.request.ScreeningUpdateRequest;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

        screeningValidator.validateUpdate(screening.getScreeningStatus(), req);

        screening.updateScreeningStatus(req.getScreeningStatus());
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
