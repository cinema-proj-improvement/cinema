package com.elice.cinema.domain.reservation.dto.response.seatselection;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScreeningInfo {
    private Long screeningId;
    private String movieTitle;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
