package com.elice.cinema.domain.screening.repository;

import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScreeningRepository extends JpaRepository<Screening, Long>, ScreeningRepositoryCustom {

    @Query("""
        select s
        from Screening s
        join fetch s.movie m
        join fetch s.screen sc
        where sc.id = :screenId
          and s.startAt >= :fromInclusive
          and s.startAt < :toExclusive
        order by s.startAt asc
    """)
    List<Screening> findTimetableByScreenAndDate(
            @Param("screenId") Long screenId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query("""
        select count(s) > 0
        from Screening s
        where s.screen.id = :screenId
          and s.startAt < :newEndAtWithCleaning
          and s.endAtWithCleaning > :newStartAt
    """)
    boolean existsTimeConflict(Long screenId,
                               LocalDateTime newStartAt,
                               LocalDateTime newEndAtWithCleaning);

    @Query("""
        select s
        from Screening s
        join fetch s.movie m
        join fetch s.screen sc
        where s.startAt >= :startOfDay
          and s.startAt < :endOfDay
          and s.screeningStatus = :status
          and (:movieId is null or m.id = :movieId)
        order by s.startAt asc
    """)
    List<Screening> findSchedulesByDate(
            @Param("startOfDay") LocalDateTime from,
            @Param("endOfDay") LocalDateTime toExclusive,
            @Param("status") ScreeningStatus status,
            @Param("movieId") Long movieId
    );

    default List<Screening> findSchedulesByDate(LocalDateTime from, LocalDateTime toExclusive, Long movieId) {
        return findSchedulesByDate(from, toExclusive, ScreeningStatus.OPEN, movieId);
    }

    // flushAutomatically = true - 실행 전에 영속성 컨텍스트의 변경 사항을 DB에 반영
    // clearAutomatically = true - 1차 캐시를 비워버림, 같은 트랜잭션 안 1차 캐시에 들고 있는 엔티티와 DB와 불일치를 없애기 위함
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Screening s
           set s.screeningStatus = com.elice.cinema.domain.screening.entity.ScreeningStatus.OPEN
         where s.screeningStatus = com.elice.cinema.domain.screening.entity.ScreeningStatus.SCHEDULED
           and s.startAt between :from and :to
    """)
    int bulkUpdateScheduledToOpen(LocalDateTime from, LocalDateTime to);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Screening s
           set s.screeningStatus = com.elice.cinema.domain.screening.entity.ScreeningStatus.FINISHED
         where s.screeningStatus <> com.elice.cinema.domain.screening.entity.ScreeningStatus.FINISHED
           and s.endAt < :now
    """)
    int bulkUpdateToFinished(LocalDateTime now);

    @Query("""
        select s
        from Screening s
        join fetch s.movie m
        join fetch s.screen sc
        where s.id = :id
""")
    Optional<Screening> findByIdWithMovieAndScreen(@Param("id") Long id);

    boolean existsByScreenIdAndScreeningStatusNot(Long screenId, ScreeningStatus status);

    boolean existsByMovieId(Long movieId);
}
