package com.elice.cinema.domain.reservation.controller;

import com.elice.cinema.domain.reservation.dto.request.HoldReservationRequest;
import com.elice.cinema.domain.reservation.service.ReservationService;
import com.elice.cinema.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping("/holds")
    public String createHoldReservation(@AuthenticationPrincipal CustomUserDetails principal,
                                        @ModelAttribute @Valid HoldReservationRequest req) {
        Long reservationId = reservationService.holdSeats(
                req.getScreeningId(), req.getSeatIds(), principal.getMemberId()
        );
        return "redirect:/reservations/" + reservationId + "/order";  // TODO: 예매 주문서 화면으로 리다이렉트
    }
}
