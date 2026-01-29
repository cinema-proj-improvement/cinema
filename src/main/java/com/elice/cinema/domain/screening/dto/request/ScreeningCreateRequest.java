package com.elice.cinema.domain.screening.dto.request;

import com.elice.cinema.domain.common.ScreeningType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScreeningCreateRequest {
    private Long movieId;
    private Long screenId;
    private ScreeningType screeningType;
    private LocalDateTime startAt;
}
