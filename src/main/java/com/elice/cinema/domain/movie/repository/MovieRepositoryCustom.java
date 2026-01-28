package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.dto.MovieWithThumbnail;
import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MovieRepositoryCustom {

    Page<Movie> findAdminMovieList(AdminMovieSearchRequest search, Pageable pageable);

    Page<MovieWithThumbnail> findUserMovies(String keyword, String sort, Pageable pageable);

    Optional<Movie> findUserMovieById(Long movieId);
}
