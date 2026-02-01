package com.elice.cinema.domain.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminReservationSummaryResponse {

    private final int total;
    private final int confirmed;
    private final int hold;
    private final int canceled;
}
