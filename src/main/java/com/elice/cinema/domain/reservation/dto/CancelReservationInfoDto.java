package com.elice.cinema.domain.reservation.dto;

import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelReservationInfoDto {
    private Long memberId;
    private ReservationStatus status;
}
