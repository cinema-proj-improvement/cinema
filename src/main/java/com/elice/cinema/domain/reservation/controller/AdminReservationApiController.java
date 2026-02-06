package com.elice.cinema.domain.reservation.controller;

import com.elice.cinema.domain.reservation.dto.response.AdminReservationDetailResponse;
import com.elice.cinema.domain.reservation.service.AdminReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/reservations")
@RequiredArgsConstructor
public class AdminReservationApiController {

    private final AdminReservationService adminReservationService;

    // 관리자 예매 상세 조회
    @GetMapping("/{reservationId}")
    public AdminReservationDetailResponse getDetail(
            @PathVariable Long reservationId
    ) {
        return adminReservationService.getAdminReservationDetail(reservationId);
    }

    // 관리자 예매 취소
    @PostMapping("/{reservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(
            @PathVariable Long reservationId
    ) {
        adminReservationService.cancelReservation(reservationId);
    }
}

