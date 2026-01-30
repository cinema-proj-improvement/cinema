package com.elice.cinema.domain.screening.service;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.screen.dto.response.ScreenSelectResponse;
import com.elice.cinema.domain.screen.mapper.ScreenMapper;
import com.elice.cinema.domain.screen.repository.ScreenRepository;
import com.elice.cinema.domain.screening.dto.response.ScreeningMovieOptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreeningOptionService {

    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ScreenMapper screenMapper;

    /**
     * 1) 영화 선택 → 해당 영화가 지원하는 상영 타입만 반환
     */
    public ScreeningMovieOptionResponse getScreeningTypesByMovie(Long movieId) {
        Movie movie = movieRepository.findByIdWithScreeningTypes(movieId)
                .orElseThrow(() -> new IllegalArgumentException("movie not found"));
        return ScreeningMovieOptionResponse.ofTypes(movie.getScreeningTypes());
    }

    /**
     * 2) (영화 + 상영 타입) 선택 → 해당 타입을 지원하는 상영관 목록 반환
     * - 상영관은 타입 1개만 보유하므로 screeningType으로 필터링
     * - 운영 중(isOperating=true) 상영관만 노출
     */
    public ScreeningMovieOptionResponse getScreensByMovieAndType(Long movieId, ScreeningType screeningType) {
        Movie movie = findMovieById(movieId);

        // 안전장치: 영화가 지원하지 않는 타입이면 빈 목록 반환(원하면 예외로 바꿔도 됨)
        if (movie.getScreeningTypes() == null || !movie.getScreeningTypes().contains(screeningType)) {
            return ScreeningMovieOptionResponse.ofScreens(List.of());
        }

        List<ScreenSelectResponse> screens = screenRepository
                .findByOperatingTrueAndScreeningTypeIn(Set.of(screeningType))
                .stream()
                .map(screenMapper::toScreenSelectResponse)
                .toList();

        return ScreeningMovieOptionResponse.ofScreens(screens);
    }

    private Movie findMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("movie not found"));
    }
}
