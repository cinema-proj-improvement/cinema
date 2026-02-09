package com.elice.cinema.domain.reservation.controller;

import com.elice.cinema.domain.movie.dto.response.ReservationMovieSelectResponse;
import com.elice.cinema.domain.reservation.dto.request.HoldReservationRequest;
import com.elice.cinema.domain.reservation.dto.response.ReservationCheckoutResponse;
import com.elice.cinema.domain.reservation.dto.response.seatselection.SeatSelectionResponse;
import com.elice.cinema.domain.reservation.service.ReservationService;
import com.elice.cinema.domain.reservation.service.SeatSelectionFacade;
import com.elice.cinema.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationPageController {
    private final ReservationService reservationService;
    private final SeatSelectionFacade seatSelectionFacade;

    @GetMapping
    public String getReservationPage(Model model) {
        List<ReservationMovieSelectResponse> movies = reservationService.getMoviesHavingScreeningsInDateRange();

        model.addAttribute("movies", movies);
        return "user/reservation/reservation-select";
    }

    @GetMapping("/{reservationId}")
    public String getCheckoutPage(@PathVariable Long reservationId,
                                  @RequestParam(defaultValue = "false") boolean error,
                                  @RequestParam(required = false) String message,
                                  Model model) {
        ReservationCheckoutResponse reservation = reservationService.getCheckoutPage(reservationId);

        if (error) {
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", message);
        }

        model.addAttribute("reservation", reservation);

        return "user/reservation/reservation-checkout";
    }

    @GetMapping("/screenings/{screeningId}/seat-selection")
    public String getSeatSelectionPage(@PathVariable Long screeningId, Model model) {
        model.addAttribute("screeningId", screeningId);
        return "user/reservation/seat-selection";
    }

    @GetMapping("/screenings/{screeningId}/seat-selection/info")
    @ResponseBody
    public SeatSelectionResponse getSeatSelectionPageInfo(@PathVariable Long screeningId) {
        return seatSelectionFacade.getSeatSelectionPageInfo(screeningId);
    }

    @PostMapping("/holds")
    public String createHoldReservation(@AuthenticationPrincipal CustomUserDetails principal,
                                        @ModelAttribute @Valid HoldReservationRequest req) {
        Long reservationId = reservationService.holdSeats(
                req.getScreeningId(), req.getSeatIds(), principal.getMemberId()
        );
        return "redirect:/reservations/" + reservationId;
    }

    @PostMapping("/{reservationId}/cancel-hold")
    @ResponseBody
    public void cancelHold(@PathVariable Long reservationId, @AuthenticationPrincipal CustomUserDetails principal) {
        reservationService.cancelHoldReservation(reservationId, principal.getMemberId());
    }
}
