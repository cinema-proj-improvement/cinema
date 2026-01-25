package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long>, MovieRepositoryCustom {
}
