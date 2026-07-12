package com.elice.cinema.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seat-hold")
@Getter @Setter
public class SeatHoldProperties {
    private int minutes;  // 사용자 notice용 좌석 선점 시간 (DB hold_expires_at 기준)
    private int lockTtlSeconds;  // redis lock TTL(초) - holdSeats() 임계구역 보호 + 크래시 안전망 용도, 짧게 유지
}
