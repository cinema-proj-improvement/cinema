package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.movie.dto.response.ReservationMovieSelectResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {
    private static final int DAYS_RANGE_INCLUSIVE = 6; //TODO: 이것도 환경 변수 테이블에 넣을지 고민

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    public List<ReservationMovieSelectResponse> getMoviesWithScreeningsWithin() {
        LocalDate today = LocalDate.now();

        LocalDateTime from = today.atStartOfDay();
        LocalDateTime toExclusive = today.plusDays(DAYS_RANGE_INCLUSIVE + 1).atStartOfDay();

        List<Movie> movies = movieRepository.findDistinctMoviesHavingScreeningsBetween(from, toExclusive);

        return movies.stream()
                .map(movieMapper::toReservationMovieSelectResponse)
                .toList();
    }
}
