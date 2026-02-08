package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.dto.SeatLockInfoDto;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {
    // 넘어온 예매들 각각에 해당하는 예매 좌석들 중 HOLD 상태인 것들을 대상으로 redis lock 관련 정보 조회하는 query (상영 id, 좌석 id)
    @Query("""
        select new com.elice.cinema.domain.reservation.dto.SeatLockInfoDto(
            rs.screening.id,
            rs.seat.id
        )
        from ReservedSeat rs
        where rs.reservation.id in :reservationIds
          and rs.status = com.elice.cinema.domain.reservation.entity.ReservationStatus.HOLD
    """)
    List<SeatLockInfoDto> findSeatLocksByReservationIds(@Param("reservationIds") List<Long> reservationIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from ReservedSeat rs
        where rs.reservation.id in :reservationIds
          and rs.status = com.elice.cinema.domain.reservation.entity.ReservationStatus.HOLD
    """)
    int bulkDeleteHoldSeatsByReservationIds(@Param("reservationIds") List<Long> reservationIds);

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

    // 특정 상영 회차에서 선택 불가능한 좌석들의 id 목록을 반환
    @Query("""
        select rs.seat.id
        from ReservedSeat rs
        where rs.screening.id = :screeningId
            and rs.status in :blockedCondition
""")
    List<Long> findBlockedSeatIds(@Param("screeningId") Long screeningId, List<ReservationStatus> blockedCondition);

    // 좌석 선택 후 들어오는 예매/예매좌석 생성 요청에서 이미 HOLD,CONFIRMED 상태로 있는 좌석 id들을 찾아서 반환 (유효성 검사용)
    @Query("""
    select rs.seat.id
    from ReservedSeat rs
    where rs.screening.id = :screeningId
      and rs.seat.id in :seatIds
      and rs.status in :blockedCondition
""")
    List<Long> findBlockedSeatIdsIn(
            @Param("screeningId") Long screeningId,
            @Param("seatIds") List<Long> seatIds,
            @Param("blockedCondition") List<ReservationStatus> blockedCondition
    );

    @Query("""
        select rs.screening.id as screeningId, count(rs.id) as reservedCount
        from ReservedSeat rs
        where rs.screening.id in :screeningIds
        group by rs.screening.id
    """)
    List<ReservedCountRow> countByScreeningIds(@Param("screeningIds") List<Long> screeningIds);

    interface ReservedCountRow {
        Long getScreeningId();
        Long getReservedCount();
    }
}
