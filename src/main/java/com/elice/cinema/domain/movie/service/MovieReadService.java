package com.elice.cinema.domain.movie.service;

import com.elice.cinema.domain.movie.dto.res.MovieResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieReadService {

    private final MovieRepository movieRepository;

    // status 없는 전체 조회
    public List<MovieResponse> getMovies() {
        return movieRepository.findAll()
                .stream()
                .map(MovieResponse::from)
                .toList();
    }

    // status 조건 조회
    public List<MovieResponse> getMovies(MovieStatus status) {
        return movieRepository.findByStatus(status)
                .stream()
                .map(MovieResponse::from)
                .toList();
    }

    // 상세 조회
    public MovieResponse getMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다."));
        return MovieResponse.from(movie);
    }

    // 영화 검색 조회
    public List<MovieResponse> searchMovies(String keyword) {
        return movieRepository.findByTitleContainingIgnoreCase(keyword)
                .stream()
                .map(MovieResponse::from)
                .toList();
    }
}
