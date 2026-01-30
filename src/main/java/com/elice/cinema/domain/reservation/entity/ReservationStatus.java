package com.elice.cinema.domain.reservation.entity;

import lombok.Getter;

@Getter
public enum ReservationStatus {
    HOLD, CONFIRMED, CANCELED
}