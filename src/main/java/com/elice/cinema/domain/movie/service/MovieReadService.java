package com.elice.cinema.domain.movie.service;

import com.elice.cinema.domain.movie.dto.res.MovieResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieReadService {

    private final MovieRepository movieRepository;

    // 관리자 영화 목록 조회 (페이지네이션 + 정렬)
    public Page<MovieResponse> getMovies(Pageable pageable) {
        return movieRepository.findAll(pageable)
                .map(MovieResponse::from);
    }

    // 상태별 영화 목록 조회 (페이지네이션 + 정렬)
    public Page<MovieResponse> getMovies(MovieStatus status, Pageable pageable) {
        return movieRepository.findByStatus(status, pageable)
                .map(MovieResponse::from);
    }

    // 영화 검색 조회 (페이지네이션 + 정렬)
    public Page<MovieResponse> searchMovies(String keyword, Pageable pageable) {
        return movieRepository.findByTitleContainingIgnoreCase(keyword, pageable)
                .map(MovieResponse::from);
    }

    // 상세 조회
    public MovieResponse getMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다."));
        return MovieResponse.from(movie);
    }
}
