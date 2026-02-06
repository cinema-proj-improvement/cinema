package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class MovieDetailResponse {

    private Long id;
    private String title;
    private Integer runningTimeMinutes;

    private LocalDate releaseDate;
    private LocalDate endDate;
    private AgeRating ageRating;
    private String synopsis;
    private Set<Genre> genres;
    private Set<ScreeningType> screeningTypes;
    private Double avgScore;
    private Double advanceReservationRate;
    private MovieStatus status;

    private String thumbnail;
    private List<String> images;
    private Long audienceCount;

    public Double getAdvanceReservationRate() {
        if (advanceReservationRate == null) {
            return null;
        }
        return Math.round(advanceReservationRate * 10) / 10.0;
    }

}
