package com.elice.cinema.domain.reservation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    HOLD("예매 대기"),
    CONFIRMED("예매 확정"),
    CANCELED("예매 취소");

    private final String displayName;
}