package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, MovieRepositoryCustom {

    Page<Movie> findUserMovies(String keyword, String sort, Pageable pageable);
    Optional<Movie> findUserMovieById(Long movieId);

}
