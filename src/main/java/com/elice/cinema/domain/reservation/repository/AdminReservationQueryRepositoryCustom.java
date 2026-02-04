package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.dto.response.AdminReservationDetailResponse;
import com.elice.cinema.domain.reservation.dto.response.AdminReservationPageResponse;
import com.elice.cinema.domain.reservation.dto.response.AdminReservationSummaryResponse;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AdminReservationQueryRepositoryCustom {
    Page<AdminReservationPageResponse> findAdminReservationPage(
            Long screeningId,
            ReservationStatus status,
            Pageable pageable
    );

    // 요약 집계용
    AdminReservationSummaryResponse findReservationSummaryByScreening(
            Long screeningId
    );

    Optional<AdminReservationDetailResponse> findAdminDetailById(Long id);
}
