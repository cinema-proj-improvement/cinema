package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.movie.entity.AgeRating;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationMovieSelectResponse {
    private Long id;
    private String title;
    private AgeRating ageRating;
}
