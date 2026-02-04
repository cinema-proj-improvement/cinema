package com.elice.cinema.domain.reservation.controller;

import com.elice.cinema.domain.reservation.service.ReservationService;
import com.elice.cinema.domain.screening.dto.response.ReservationScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationApiController {
    private final ReservationService reservationService;

    @GetMapping("/schedule")
    public List<ReservationScheduleResponse> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long movieId
    ) {
        return reservationService.getSchedulesByDate(date, movieId);
    }
}
