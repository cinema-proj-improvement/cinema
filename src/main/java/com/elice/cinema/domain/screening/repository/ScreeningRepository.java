package com.elice.cinema.domain.screening.repository;

import com.elice.cinema.domain.screening.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {

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
}
