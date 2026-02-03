package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.reservation.dto.response.AdminReservationDetailResponse;
import com.elice.cinema.domain.reservation.dto.response.AdminReservationPageResponse;
import com.elice.cinema.domain.reservation.dto.response.AdminReservationSummaryResponse;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReservationService {

    private final ReservationRepository reservationRepository;

    // 관리자 상영별 예매 목록 조회 (페이지 + 좌석 정렬)
    public Page<AdminReservationPageResponse> getAdminReservationListByScreening(
            Long screeningId,
            ReservationStatus status,
            Pageable pageable
    ) {
        Page<AdminReservationPageResponse> page =
                reservationRepository
                        .findAdminReservationPage(screeningId, status, pageable);

        // 좌석 요약 문자열만 도메인 기준으로 정렬
        return page.map(r ->
                new AdminReservationPageResponse(
                        r.getId(),
                        r.getReservationCode(),
                        r.getMemberName(),
                        r.getStatus(),
                        sortSeatSummary(r.getSeatSummary()),
                        r.getPaymentStatus(),
                        r.getReservedAt(),
                        r.getTotalPrice()
                )
        );
    }

    // 관리자 상영별 예매 요약 조회
    public AdminReservationSummaryResponse getReservationSummaryByScreening(
            Long screeningId
    ) {
        return reservationRepository
                .findReservationSummaryByScreening(screeningId);
    }

    // 관리자 예매 상세 조회
    public AdminReservationDetailResponse getAdminReservationDetail(Long reservationId) {

        Reservation reservation = reservationRepository
                .findByIdWithScreeningAndMovie(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        AdminReservationDetailResponse raw = reservationRepository
                .findAdminDetailById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        boolean cancelable =
                reservation.isCancelableStatus()
                        && reservation.isBeforeScreening();

        return raw.withCancelable(cancelable);
    }

    // 좌석 번호 정렬
    private String sortSeatSummary(String seatSummary) {
        if (seatSummary == null || seatSummary.isBlank()) {
            return "";
        }

        return Arrays.stream(seatSummary.split(","))
                .map(String::trim)
                .sorted((a, b) -> {
                    // 좌석 행 (A, B, C ...)
                    String rowA = a.replaceAll("\\d", "");
                    String rowB = b.replaceAll("\\d", "");
                    int rowCompare = rowA.compareTo(rowB);
                    if (rowCompare != 0) return rowCompare;

                    // 좌석 번호 (1, 2, 10 ...)
                    int numA = Integer.parseInt(a.replaceAll("\\D", ""));
                    int numB = Integer.parseInt(b.replaceAll("\\D", ""));
                    return Integer.compare(numA, numB);
                })
                .collect(Collectors.joining(", "));
    }

    @Transactional
    public void cancelReservation(Long reservationId) {

        Reservation reservation = reservationRepository
                .findByIdWithScreeningAndMovie(reservationId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESERVATION_NOT_FOUND)
                );
        reservation.cancel();
    }

}