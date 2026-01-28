package com.elice.cinema.domain.screen.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenListResponse {
    private Long id;
    private String name;
    private ScreeningType screeningType;
    private Integer totalSeats;
    private Boolean operating;
}
