package com.elice.cinema.domain.screening.service;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.domain.screen.repository.ScreenRepository;
import com.elice.cinema.domain.screening.dto.request.ScreeningCreateRequest;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    @Mock ScreeningRepository screeningRepository;
    @Mock MovieRepository movieRepository;
    @Mock ScreenRepository screenRepository;
    @Mock ScreeningMapper screeningMapper; // createScreening에선 직접 사용 X (의존성 주입용)
    @Mock ScreeningValidator screeningValidator;
    @Mock EnvironmentPolicyService environmentPolicyService;

    @InjectMocks ScreeningService screeningService;

    @Test
    @DisplayName("상영 생성 성공 - endAt/endAtWithCleaning 계산 후 검증 통과하면 상영이 screen에 추가된다")
    void createScreening_success_addsScreeningToScreen() {
        // given
        Long movieId = 1L;
        Long screenId = 10L;

        LocalDateTime startAt = LocalDateTime.of(2026, 2, 10, 10, 0);

        ScreeningCreateRequest req = mock(ScreeningCreateRequest.class);
        when(req.getMovieId()).thenReturn(movieId);
        when(req.getScreenId()).thenReturn(screenId);
        when(req.getStartAt()).thenReturn(startAt);
        when(req.getScreeningType()).thenReturn(ScreeningType.TWO_D);

        Movie movie = mock(Movie.class);
        when(movie.getRunningTimeMinutes()).thenReturn(100); // 100분 러닝타임

        Screen screen = Screen.of("1관", ScreeningType.TWO_D, 50, true);

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(screenRepository.findById(screenId)).thenReturn(Optional.of(screen));

        when(environmentPolicyService.getCleaningMinutes()).thenReturn(15);

        LocalDateTime expectedEndAt = startAt.plusMinutes(100);
        LocalDateTime expectedEndAtWithCleaning = expectedEndAt.plusMinutes(15);

        // validateCreate는 통과
        doNothing().when(screeningValidator)
                .validateCreate(req, movie, screen, expectedEndAtWithCleaning);

        // 초기 상태 결정
        when(screeningValidator.determineInitialStatus(startAt, expectedEndAt))
                .thenReturn(ScreeningStatus.SCHEDULED);

        // when
        screeningService.createScreening(req);

        // then
        assertEquals(1, screen.getScreenings().size());

        Screening created = screen.getScreenings().get(0);
        assertSame(movie, created.getMovie());
        assertSame(screen, created.getScreen());
        assertEquals(ScreeningType.TWO_D, created.getScreeningType());
        assertEquals(startAt, created.getStartAt());
        assertEquals(expectedEndAt, created.getEndAt());
        assertEquals(expectedEndAtWithCleaning, created.getEndAtWithCleaning());
        assertEquals(ScreeningStatus.SCHEDULED, created.getScreeningStatus());

        verify(movieRepository).findById(movieId);
        verify(screenRepository).findById(screenId);
        verify(environmentPolicyService).getCleaningMinutes();
        verify(screeningValidator).validateCreate(req, movie, screen, expectedEndAtWithCleaning);
        verify(screeningValidator).determineInitialStatus(startAt, expectedEndAt);
        verifyNoInteractions(screeningRepository); // createScreening은 screen.addScreening으로만 생성 (repo save 없음)
    }

    @Test
    @DisplayName("상영 생성 실패 - 영화가 존재하지 않으면 예외(MOVIE_NOT_FOUND)로 실패한다")
    void createScreening_fail_whenMovieNotFound() {
        // given
        Long movieId = 999L;

        ScreeningCreateRequest req = mock(ScreeningCreateRequest.class);
        when(req.getMovieId()).thenReturn(movieId);

        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(BusinessException.class, () -> screeningService.createScreening(req));

        verify(movieRepository).findById(movieId);
        verifyNoInteractions(screenRepository, environmentPolicyService, screeningValidator, screeningRepository);
    }

    @Test
    @DisplayName("상영 생성 실패 - 검증(validateCreate)에서 예외 발생 시 상영이 추가되지 않는다")
    void createScreening_fail_whenValidationFails() {
        // given
        Long movieId = 1L;
        Long screenId = 10L;

        LocalDateTime startAt = LocalDateTime.of(2026, 2, 10, 10, 0);

        ScreeningCreateRequest req = mock(ScreeningCreateRequest.class);
        when(req.getMovieId()).thenReturn(movieId);
        when(req.getScreenId()).thenReturn(screenId);
        when(req.getStartAt()).thenReturn(startAt);

        Movie movie = mock(Movie.class);
        when(movie.getRunningTimeMinutes()).thenReturn(100);

        Screen screen = Screen.of("1관", ScreeningType.TWO_D, 50, true);

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(screenRepository.findById(screenId)).thenReturn(Optional.of(screen));
        when(environmentPolicyService.getCleaningMinutes()).thenReturn(15);

        LocalDateTime endAt = startAt.plusMinutes(100);
        LocalDateTime endAtWithCleaning = endAt.plusMinutes(15);

        doThrow(BusinessException.class).when(screeningValidator)
                .validateCreate(req, movie, screen, endAtWithCleaning);

        // when & then
        assertThrows(BusinessException.class, () -> screeningService.createScreening(req));

        // 검증에서 터졌으니 추가되면 안 됨
        assertEquals(0, screen.getScreenings().size());

        verify(movieRepository).findById(movieId);
        verify(screenRepository).findById(screenId);
        verify(environmentPolicyService).getCleaningMinutes();
        verify(screeningValidator).validateCreate(req, movie, screen, endAtWithCleaning);
        verify(screeningValidator, never()).determineInitialStatus(any(), any()); // 검증 통과 못했으니 호출 X
        verifyNoInteractions(screeningRepository);
    }
}
