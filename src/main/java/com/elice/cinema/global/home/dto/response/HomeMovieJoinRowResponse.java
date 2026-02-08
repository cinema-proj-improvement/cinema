package com.elice.cinema.global.home.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeMovieJoinRowResponse {

    private Long movieId;
    private String title;
    private String synopsis;
    private Double advanceReservationRate;
    private String thumbnail;
}
