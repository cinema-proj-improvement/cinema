package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationQueryRepository {
    @Query("""
        select r
        from Reservation r
        join fetch r.screening s
        join fetch s.movie m
        where r.id = :reservationId
    """)
    Optional<Reservation> findByIdWithScreeningAndMovie(@Param("reservationId") Long reservationId);

}
