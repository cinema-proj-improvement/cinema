package com.elice.cinema.domain.screen.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenSeatResponse {
    private Long id;
    private String seatCode;
    private Boolean active;
    private Integer rowNo;
    private Integer colNo;
}
