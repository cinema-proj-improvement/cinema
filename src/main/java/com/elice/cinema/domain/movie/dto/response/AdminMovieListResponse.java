package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminMovieListResponse {

    private Long id;
    private String thumbnail;
    private String title;
    private List<Genre> genres;
    private MovieStatus status;
    private AgeRating ageRating;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private Double avgScore;
    private Double advanceReservationRate;

}
