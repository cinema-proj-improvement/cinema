package com.elice.cinema.global.batch.service.movie;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieReservationRateBatchService {

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * 영화별 예매율 갱신
     * - CONFIRMED 좌석 기준
     */
    @Transactional
    public void updateReservationRate() {

        List<Object[]> rows = entityManager
                .createNativeQuery("""
                    select
                        m.id,
                        case
                            when coalesce(sum(sc.total_seats), 0) = 0 then 0
                            else count(rs.id) * 1.0 / coalesce(sum(sc.total_seats), 0) * 100
                            end as rate
                    from movies m
                    left join screenings s on s.movie_id = m.id
                    left join screens sc on sc.id = s.screen_id
                    left join reserved_seats rs
                        on rs.screening_id = s.id
                       and rs.status = 'CONFIRMED'
                    group by m.id
                """)
                .getResultList();

        for (Object[] row : rows) {
            Long movieId = ((Number) row[0]).longValue();

            Number rateNumber = (Number) row[1];
            double rate = rateNumber == null ? 0.0 : rateNumber.doubleValue();

            entityManager.createQuery("""
                update Movie m
                set m.advanceReservationRate = :rate
                where m.id = :movieId
            """)
            .setParameter("rate", rate)
            .setParameter("movieId", movieId)
            .executeUpdate();
        }
    }
}