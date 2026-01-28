package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@AllArgsConstructor
public class AdminMovieListResponse {

    private Long id;
    private String title;
    private String synopsis;
    private int runningTimeMinutes;
    private Set<Genre> genres;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private MovieStatus status;
    private AgeRating ageRating;
    private Double avgScore;
    private Double advanceReservationRate;
    private Set<ScreeningType> screeningTypes;
    private String thumbnail;
}
