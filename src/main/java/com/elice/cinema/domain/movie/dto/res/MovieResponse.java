package com.elice.cinema.domain.movie.dto.res;

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

    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getRunningTimeMinutes(),
                movie.getReleaseDate(),
                movie.getStatus()
        );
    }
}
