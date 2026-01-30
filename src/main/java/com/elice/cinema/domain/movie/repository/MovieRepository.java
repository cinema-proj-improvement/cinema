package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, MovieRepositoryCustom {

    Optional<Movie> findUserMovieById(Long movieId);
    List<Movie> findAllByStatusNot(MovieStatus movieStatus);

    @Query("""
      select m from Movie m
      left join fetch m.screeningTypes
      where m.id = :movieId
    """)
    Optional<Movie> findByIdWithScreeningTypes(@Param("movieId") Long movieId);
}
