package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class MovieResponse {

    private Long id;
    private String title;
    private Integer runningTimeMinutes;
    private LocalDate releaseDate;
    private MovieStatus status;

}
