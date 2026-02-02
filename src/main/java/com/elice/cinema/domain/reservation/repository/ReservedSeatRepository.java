package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Collection;
import java.util.List;

public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {

    // ✅ N+1 방지: reservationId들 한번에 좌석 조회
    List<ReservedSeat> findByReservationIdIn(Collection<Long> reservationIds);
    // 상세 모달용
    List<ReservedSeat> findByReservationId(Long reservationId);
    int countAllByScreening_Id(Long screeningId);
    @Query("""
        select rs.seatCode
        from ReservedSeat rs
        where rs.reservation.id = :reservationId
    """)
    List<String> findSeatCodesByReservationId(@Param("reservationId") Long reservationId);
}
