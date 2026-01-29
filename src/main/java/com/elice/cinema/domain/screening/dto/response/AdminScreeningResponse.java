package com.elice.cinema.domain.screening.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class AdminScreeningResponse {

    private final Long id;

    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;

    private final String movieTitle;
    private final String screenName;
    private final ScreeningType screeningType;

    private final ScreeningStatus status;
}
