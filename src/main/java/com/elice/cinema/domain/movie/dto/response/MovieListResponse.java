package com.elice.cinema.domain.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class MovieListResponse {

    private Long id;
    private String thumbnail;
    private String title;
    private LocalDate releaseDate;
    private Double advanceReservationRate;
    private Double avgScore;

}
