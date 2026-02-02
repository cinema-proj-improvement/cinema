package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {
    int countAllByScreening_Id(Long screeningId);
    @Query("""
        select rs.seatCode
        from ReservedSeat rs
        where rs.reservation.id = :reservationId
    """)
    List<String> findSeatCodesByReservationId(@Param("reservationId") Long reservationId);
}
