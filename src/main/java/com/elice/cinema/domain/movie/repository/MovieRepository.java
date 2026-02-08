package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.global.home.dto.response.HomeMovieJoinRowResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Query("""
        select distinct m
        from Movie m
        join Screening s on s.movie = m
        where s.startAt >= :from
          and s.startAt < :toExclusive
          and s.screeningStatus = :status
        order by m.title asc
    """)
    List<Movie> findDistinctMoviesHavingScreeningsBetween(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("status") ScreeningStatus status
    );

    default List<Movie> findDistinctMoviesHavingScreeningsBetween(LocalDateTime from, LocalDateTime toExclusive) {
        return findDistinctMoviesHavingScreeningsBetween(from, toExclusive, ScreeningStatus.OPEN);
    }

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

    @Query("""
    select new com.elice.cinema.global.home.dto.response.HomeMovieJoinRowResponse(
        m.id,
        m.title,
        m.synopsis,
        m.advanceReservationRate,
        i.imageUrl
    )
    from Movie m
    left join MovieImage i
        on i.movie = m
        and i.displayOrder = 0
    where m.status = com.elice.cinema.domain.movie.entity.MovieStatus.NOW_SHOWING
    order by m.advanceReservationRate desc
""")
    List<HomeMovieJoinRowResponse> findTopHomeMovies(Pageable pageable);



}
