package com.elice.cinema.domain.screening.dto.response;

import com.elice.cinema.domain.movie.dto.response.MovieTitleResponse;
import com.elice.cinema.domain.screen.dto.response.ReservationScreenSelectResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReservationScheduleResponse {
    private Long Id;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private MovieTitleResponse movie;
    private ReservationScreenSelectResponse screen;

    private Integer remainingSeats;
}
