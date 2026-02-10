package com.elice.cinema.domain.reservation.dto;

import com.elice.cinema.domain.reservation.entity.ReservationStatus;

public interface CancelReservationInfoDto {
    Long getMemberId();
    ReservationStatus getStatus();
}
