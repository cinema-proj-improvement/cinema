package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@AllArgsConstructor
public class MovieListResponse {

    private Long id;
    private String title;
    private LocalDate releaseDate;
    private AgeRating ageRating;
    private Set<Genre> genres;
    private Double avgScore;
    private Double advanceReservationRate;
    private MovieStatus status;
    private String thumbnail;

}
