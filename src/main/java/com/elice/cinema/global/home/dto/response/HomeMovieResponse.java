package com.elice.cinema.global.home.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeMovieResponse {

    private Long id;
    private String title;
    private String synopsis;
    private String thumbnail;
    private Double advanceReservationRate;

    public Double getAdvanceReservationRate() {
        if (advanceReservationRate == null) return null;
        return Math.round(advanceReservationRate * 10) / 10.0;
    }
}
