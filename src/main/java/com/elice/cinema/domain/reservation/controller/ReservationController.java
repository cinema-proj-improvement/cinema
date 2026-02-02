package com.elice.cinema.domain.reservation.controller;

import com.elice.cinema.domain.reservation.service.ReservationService;
import com.elice.cinema.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    public String createHoldReservation(@AuthenticationPrincipal CustomUserDetails principal,
                                    @RequestParam Long screeningId,
                                    @RequestParam List<Long> seatIds) {
        Long reservationId = reservationService.holdSeats(screeningId, seatIds, principal.getMemberId());
        return "redirect:/reservations/" + reservationId + "/order";  // TODO: 예매 주문서 화면으로 리다이렉트
    }
}
