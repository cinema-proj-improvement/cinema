package com.elice.cinema.domain.movie.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieSelectResponse {
    private Long id;
    private String title;
    private Integer runningTimeMinutes;
}
