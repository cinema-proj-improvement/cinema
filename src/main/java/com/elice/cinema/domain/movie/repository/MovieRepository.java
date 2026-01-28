package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, MovieRepositoryCustom {

    Optional<Movie> findUserMovieById(Long movieId);

}
