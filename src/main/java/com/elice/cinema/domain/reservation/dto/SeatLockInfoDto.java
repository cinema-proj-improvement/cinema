package com.elice.cinema.domain.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatLockInfoDto {
    private final Long screeningId;
    private final Long seatId;
}
