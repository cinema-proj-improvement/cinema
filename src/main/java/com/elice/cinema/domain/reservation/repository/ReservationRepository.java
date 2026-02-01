package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // ✅ summary 계산용 (전체)
    List<Reservation> findByScreeningIdOrderByReservedAtDesc(Long screeningId);

    // ✅ 페이지네이션 (필터 없음)
    Page<Reservation> findByScreeningId(
            Long screeningId,
            Pageable pageable
    );

    // ✅ 페이지네이션 + 상태 필터
    Page<Reservation> findByScreeningIdAndStatus(
            Long screeningId,
            ReservationStatus status,
            Pageable pageable
    );
}