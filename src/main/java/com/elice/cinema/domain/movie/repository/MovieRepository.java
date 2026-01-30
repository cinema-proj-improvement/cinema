package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    // releaseDate == today AND status == UPCOMING 인 영화들을 NOW_SHOWING 으로 변경
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Movie m
           set m.status = :to
         where m.status = :from 
           and m.releaseDate = :today
    """)
    int bulkUpdateUpcomingToNowShowing(@Param("from") MovieStatus from,
                                       @Param("to") MovieStatus to,
                                       @Param("today") LocalDate today);

    // endDate == today 인 영화들을 ENDED 로 변경 (endDate가 되면 무조건 ENDED)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Movie m
           set m.status = :to
         where m.endDate = :today
    """)
    int bulkUpdateToEndedByEndDate(@Param("to") MovieStatus to, @Param("today") LocalDate today);
}
