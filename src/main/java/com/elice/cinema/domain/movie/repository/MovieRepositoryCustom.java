package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.dto.response.MovieListResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MovieRepositoryCustom {

    Page<MovieListResponse> findUserMovies(String keyword, String sort, Pageable pageable);

    Optional<Movie> findUserMovieById(Long movieId);

    List<Long> findAdminMovieIds(AdminMovieSearchRequest search, Pageable pageable);

    long countAdminMovies(AdminMovieSearchRequest search);

}
