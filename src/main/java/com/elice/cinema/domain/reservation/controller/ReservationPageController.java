package com.elice.cinema.domain.reservation.controller;

import com.elice.cinema.domain.movie.dto.response.ReservationMovieSelectResponse;
import com.elice.cinema.domain.reservation.dto.response.ReservationCheckoutResponse;
import com.elice.cinema.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationPageController {
    private final ReservationService reservationService;

    @GetMapping
    public String getReservationPage(Model model) {
        List<ReservationMovieSelectResponse> movies = reservationService.getMoviesWithScreeningsWithin();

        model.addAttribute("movies", movies);
        return "user/reservation/reservation-select";
    }

    @GetMapping("/{reservationId}")
    public String getCheckoutPage(@PathVariable Long reservationId,
                                  Model model) {
        ReservationCheckoutResponse reservation = reservationService.getCheckoutPage(reservationId);
        model.addAttribute("reservation", reservation);

        return "user/reservation/reservation-checkout";
    }
}
