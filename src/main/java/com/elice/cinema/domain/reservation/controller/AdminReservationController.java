package com.elice.cinema.domain.reservation.controller;

import com.elice.cinema.domain.reservation.service.AdminReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reservations")
public class AdminReservationController {

    private final AdminReservationService adminReservationService;

    @GetMapping("/{reservationId}")
    public String getReservationDetailModal(
            @PathVariable Long reservationId,
            Model model
    ) {
        model.addAttribute(
                "reservation",
                adminReservationService.getAdminReservationDetail(reservationId)
        );

        return "admin/reservation/reservation-detail-modal";
    }
}
