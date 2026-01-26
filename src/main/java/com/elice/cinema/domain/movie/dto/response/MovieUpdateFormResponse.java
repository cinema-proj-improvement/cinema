package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.elice.cinema.domain.movie.entity.ScreeningType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Builder
public class MovieUpdateFormResponse {
    private String title;
    private int runningTimeMinutes;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private AgeRating ageRating;
    private String synopsis;
    private String thumbnailImageUrl;
    private Set<Genre> genres;
    private Set<ScreeningType> screeningTypes;
    private Double avgScore;
    private Double advanceReservationRate;
    private MovieStatus status;
}

