package com.elice.cinema.domain.screening.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminScreeningSeatResponse {

    private Long seatId;
    private String seatCode;
    private Integer rowNo;
    private Integer colNo;
    private String status;
}
