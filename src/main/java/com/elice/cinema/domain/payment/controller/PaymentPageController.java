package com.elice.cinema.domain.payment.controller;

import com.elice.cinema.domain.reservation.dto.response.TossPaymentReservationResponse;
import com.elice.cinema.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@Controller
@RequiredArgsConstructor
public class PaymentPageController {
    private final ReservationService reservationService;

    @GetMapping("reservations/{reservationId}/payment")
    public String checkout(@PathVariable Long reservationId,
                           Model model) {
        TossPaymentReservationResponse reservation = reservationService.getTossPage(reservationId);
        model.addAttribute("reservationId", reservationId);
        model.addAttribute("reservation", reservation);
        return "user/payment/checkout";
    }
}
