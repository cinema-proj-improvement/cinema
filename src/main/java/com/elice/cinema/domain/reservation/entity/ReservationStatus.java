package com.elice.cinema.domain.reservation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    HOLD("예매 대기"),
    CONFIRMED("예매 확정"),
    CANCELED("예매 취소"),
    EXPIRED("예매 만료");

    private final String displayName;

    // 좌석 선택 불가능한 예매 상태 목록
    public static List<ReservationStatus> blocked() {
        return List.of(HOLD, CONFIRMED);
    }
}