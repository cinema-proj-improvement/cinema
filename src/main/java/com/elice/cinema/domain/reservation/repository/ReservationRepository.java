package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.dto.CancelReservationInfoDto;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, AdminReservationQueryRepositoryCustom {
    @Query("""
                select r
                from Reservation r
                join fetch r.screening s
                join fetch s.movie m
                where r.id = :reservationId
            """)
    Optional<Reservation> findByIdWithScreeningAndMovie(@Param("reservationId") Long reservationId);

    Optional<Reservation> findByReservationCode(String reservationCode);

    Optional<Reservation> findByReservationCodeAndStatus(String reservationCode, ReservationStatus status);

    @Query("""
            select r
            from Reservation r
            join fetch r.member
            where r.reservationCode = :code
            and r.status = :status
            """)
    Optional<Reservation> findByReservationCodeAndStatusWithMember(
            @Param("code") String code,
            @Param("status") ReservationStatus status
    );

    @EntityGraph(attributePaths = {"reservedSeats"})
    Optional<Reservation> findWithReservedSeatsById(Long id);

    Optional<Reservation> findByIdAndStatus(Long id, ReservationStatus status);

    @EntityGraph(attributePaths = "reservedSeats")
    Page<Reservation> findByMemberId(
            Long memberId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "reservedSeats")
    List<Reservation> findTop3ByMemberIdOrderByReservedAtDesc(Long memberId);

    @EntityGraph(attributePaths = "reservedSeats")
    @Query("""
                select r
                from Reservation r
                where r.member.id = :memberId
                  and r.reservedAt between :from and :to
                order by r.reservedAt desc
            """)
    Slice<Reservation> findMyReservationsByPeriod(
            @Param("memberId") Long memberId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // 넘어온 시간을 기준으로 만료된 reservation들에서 예매 id와 해당되는 상영 id를 가져오는 query
    @Query("""
                    select r.id
                    from Reservation r
                    where r.status = com.elice.cinema.domain.reservation.entity.ReservationStatus.HOLD
                        and r.holdExpiresAt < :now
                    order by r.id
            """)
    List<Long> findExpiredHoldReservationIds(@Param("now") LocalDateTime now, Pageable pageable);

    // 넘어온 id들에 해당하는 reservation들 중 HOLD 상태인 것들을 EXPIRED 상태로 수정해주는 bulk update query
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                update Reservation r
                set r.status = com.elice.cinema.domain.reservation.entity.ReservationStatus.EXPIRED
                where r.id in :ids
                  and r.status = com.elice.cinema.domain.reservation.entity.ReservationStatus.HOLD
            """)
    int bulkExpireHoldReservations(@Param("ids") List<Long> ids);

    @Query("""
                    select r.member.id as memberId,
                           r.status as status
                    from Reservation r
                    where r.id = :reservationId
            """)
    Optional<CancelReservationInfoDto> findCancelReservationInfo(@Param("reservationId") Long reservationId);
}
