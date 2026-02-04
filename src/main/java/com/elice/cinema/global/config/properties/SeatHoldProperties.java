package com.elice.cinema.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seat-hold")
@Getter @Setter
public class SeatHoldProperties {
    private int minutes;  // 사용자 notice용 좌석 선점 시간
    private int redisGraceMinutes;  // 실제 시스템상 redis lock 점유시간 구현을 위한 시스템 여유시간
}
