package com.elice.cinema.domain.screen.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScreenListResponse {
    private Long id;
    private String name;
    private ScreeningType screeningType;
    private Integer totalSeats;
    private boolean operating;
}
