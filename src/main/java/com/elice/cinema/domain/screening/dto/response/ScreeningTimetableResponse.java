package com.elice.cinema.domain.screening.dto.response;

import com.elice.cinema.domain.movie.dto.response.MovieTitleResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScreeningTimetableResponse {
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private MovieTitleResponse movie;
}
