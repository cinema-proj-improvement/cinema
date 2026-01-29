package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter @Setter
public class MovieUpdateFormResponse {
    private String title;
    private int runningTimeMinutes;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private AgeRating ageRating;
    private String synopsis;

    private String thumbnailImageUrl;
    private List<String> extraImages;

    private Set<Genre> genres;
    private Set<ScreeningType> screeningTypes;

    // === 수정 불가능하지만 화면 표시용도 ===
    private Double avgScore;
    private Double advanceReservationRate;
    private MovieStatus status;
}

