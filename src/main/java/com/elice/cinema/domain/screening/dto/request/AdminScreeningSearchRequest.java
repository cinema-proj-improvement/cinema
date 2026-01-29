package com.elice.cinema.domain.screening.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdminScreeningSearchRequest {

    private LocalDate startDate;
    private LocalDate endDate;

    private String keyword;
    private Long movieId;
    private Long screenId;

}
