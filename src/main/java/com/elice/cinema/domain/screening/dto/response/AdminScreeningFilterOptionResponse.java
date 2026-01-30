package com.elice.cinema.domain.screening.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminScreeningFilterOptionResponse {

    private Long movieId;
    private String movieName;

    private Long screenId;
    private String screenName;
}
